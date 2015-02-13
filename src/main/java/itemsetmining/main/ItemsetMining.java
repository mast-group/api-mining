package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.itemset.Rule;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.SparkItemsetMining.LogLevelConverter;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionList;
import itemsetmining.util.Logging;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetMining extends ItemsetMiningCore {

	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "Dataset filename")
		private final File dataset = new File(
				"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Uganda/keywords/uganda_en_teenage_pregnancy.dat");

		@Parameter(names = { "-s", "--maxSteps" }, description = "Max structure steps")
		int maxStructureSteps = 10_000;

		@Parameter(names = { "-i", "--iterations" }, description = "Max iterations")
		int maxEMIterations = 1_000;

		@Parameter(names = { "-l", "--log-level" }, description = "Log level", converter = LogLevelConverter.class)
		Level logLevel = Level.FINE;

		@Parameter(names = { "-r", "--runtime" }, description = "Max Runtime (min)")
		long maxRunTime = 72 * 60; // 12hrs

		@Parameter(names = { "-t", "--timestamp" }, description = "Timestamp Logfile", arity = 1)
		boolean timestampLog = true;
	}

	public static void main(final String[] args) throws IOException {

		// Main fixed parameters
		final boolean associationRules = false;
		final InferenceAlgorithm inferenceAlg = new InferGreedy();

		// Runtime parameters
		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);

		try {
			jc.parse(args);

			// Set loglevel, runtime, timestamp and log file
			LOG_LEVEL = params.logLevel;
			MAX_RUNTIME = params.maxRunTime * 60 * 1_000;
			final File logFile = Logging.getLogFileName("IIM",
					params.timestampLog, LOG_DIR, params.dataset);

			// Mine interesting itemsets
			final Map<Itemset, Double> itemsets = mineItemsets(params.dataset,
					inferenceAlg, params.maxStructureSteps,
					params.maxEMIterations, logFile);

			// Generate Association rules from the interesting itemsets
			if (associationRules) {
				final List<Rule> rules = generateAssociationRules(itemsets);
				System.out
						.println("\n============= ASSOCIATION RULES =============");
				for (final Rule rule : rules) {
					System.out.println(rule.toString());
				}
				System.out.println("\n");
			}

		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}

	}

	/** Mine interesting itemsets */
	public static Map<Itemset, Double> mineItemsets(final File inputFile,
			final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations,
			final File logFile) throws IOException {

		// Set up logging
		if (logFile != null)
			Logging.setUpFileLogger(logger, LOG_LEVEL, logFile);
		else
			Logging.setUpConsoleLogger(logger, LOG_LEVEL);

		// Read in transaction database
		final TransactionList transactions = readTransactions(inputFile);

		// Determine most frequent singletons
		final Multiset<Integer> singletons = scanDatabaseToDetermineFrequencyOfSingleItems(inputFile);

		// Apply the algorithm to build the itemset tree
		final ItemsetTree tree = new ItemsetTree(singletons);
		tree.buildTree(inputFile);
		if (LOG_LEVEL.equals(Level.FINE))
			tree.printStatistics(logger);
		if (LOG_LEVEL.equals(Level.FINEST)) {
			logger.finest("THIS IS THE TREE:\n");
			logger.finest(tree.toString());
		}

		// Run inference to find interesting itemsets
		logger.fine("\n============= ITEMSET INFERENCE =============\n");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				singletons, tree, inferenceAlgorithm, maxStructureSteps,
				maxEMIterations);
		if (LOG_LEVEL.equals(Level.FINEST))
			logger.finest("\n======= Transaction Database =======\n"
					+ Files.toString(inputFile, Charsets.UTF_8) + "\n");

		// Sort itemsets by interestingness
		final HashMap<Itemset, Double> intMap = calculateInterestingness(
				itemsets, transactions, tree);
		final Map<Itemset, Double> sortedItemsets = sortItemsets(itemsets,
				intMap);

		logger.info("\n============= INTERESTING ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : sortedItemsets.entrySet()) {
			logger.info(String.format("%s\tprob: %1.5f \tint: %1.5f %n",
					entry.getKey(), entry.getValue(),
					intMap.get(entry.getKey())));
		}
		logger.info("\n");

		return sortedItemsets;
	}

	public static TransactionList readTransactions(final File inputFile)
			throws IOException {

		final List<Transaction> transactions = Lists.newArrayList();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#'
					|| line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the transaction into items
			final String[] lineSplited = line.split(" ");
			// create a structure for storing the transaction
			final Transaction transaction = new Transaction();
			// for each item in the transaction
			for (int i = 0; i < lineSplited.length; i++) {
				// convert the item to integer and add it to the structure
				transaction.add(Integer.parseInt(lineSplited[i]));

			}
			transactions.add(transaction);

		}
		// close the input file
		LineIterator.closeQuietly(it);

		return new TransactionList(transactions);
	}

	/**
	 * This method scans the input database to calculate the support of single
	 * items.
	 *
	 * @param inputFile
	 *            the input file
	 * @return a multiset for storing the support of each item
	 */
	public static Multiset<Integer> scanDatabaseToDetermineFrequencyOfSingleItems(
			final File inputFile) throws IOException {

		final Multiset<Integer> singletons = HashMultiset.create();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#'
					|| line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the line into items
			final String[] lineSplit = line.split(" ");
			// for each item
			for (final String itemString : lineSplit) {
				// increase the support count of the item
				singletons.add(Integer.parseInt(itemString));
			}
		}
		// close the input file
		LineIterator.closeQuietly(it);

		return singletons;
	}

	private static List<Rule> generateAssociationRules(
			final Map<Itemset, Double> itemsets) {

		final List<Rule> rules = Lists.newArrayList();

		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			final HashSet<Integer> setForRecursion = Sets.newHashSet(entry
					.getKey());
			recursiveGenRules(rules, setForRecursion, new HashSet<Integer>(),
					entry.getValue());
		}

		return rules;
	}

	private static void recursiveGenRules(final List<Rule> rules,
			final HashSet<Integer> antecedent,
			final HashSet<Integer> consequent, final double prob) {

		// Stop if no more rules to generate
		if (antecedent.isEmpty())
			return;

		// Add rule
		if (!antecedent.isEmpty() && !consequent.isEmpty())
			rules.add(new Rule(antecedent, consequent, prob));

		// Recursively generate more rules
		for (final Integer element : antecedent) {
			final HashSet<Integer> newAntecedent = Sets.newHashSet(antecedent);
			newAntecedent.remove(element);
			final HashSet<Integer> newConsequent = Sets.newHashSet(consequent);
			newConsequent.add(element);
			recursiveGenRules(rules, newAntecedent, newConsequent, prob);
		}

	}

}