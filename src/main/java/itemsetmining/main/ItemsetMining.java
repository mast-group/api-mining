package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.itemset.Rule;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.InferenceAlgorithms.inferGreedy;
import itemsetmining.transaction.Transaction;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.Rules;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

import com.google.common.base.Charsets;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetMining {

	// Main function parameters
	private static final String dataset = "caviar.txt";
	private static final boolean associationRules = false;
	private static final InferenceAlgorithm inferenceAlg = new inferGreedy();

	private static final boolean fpGrowth = false;
	private static final double fpGrowthSupport = 0.25; // relative support
	private static final double fpGrowthMinConf = 0;
	private static final double fpGrowthMinLift = 0;

	// Global Constants
	private static final double SINGLETON_PRIOR_PROB = 0.5;
	private static final int MAX_RANDOM_WALKS = 100;
	private static final int MAX_STRUCTURE_ITERATIONS = 10;

	private static final int OPTIMIZE_PARAMS_EVERY = 1;
	private static final double OPTIMIZE_TOL = 1e-10;

	private static final Logger logger = Logger.getLogger(ItemsetMining.class
			.getName());
	private static final Level LOGLEVEL = Level.FINE;

	public static void main(final String[] args) throws IOException {

		// Find transaction database
		final URL url = ItemsetMining.class.getClassLoader().getResource(
				dataset);
		final String input = java.net.URLDecoder.decode(url.getPath(), "UTF-8");
		final File inputFile = new File(input);

		// Mine interesting itemsets
		final HashMap<Itemset, Double> itemsets = mineItemsets(inputFile);

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

		// Compare with the FPGROWTH algorithm
		if (fpGrowth) {
			final AlgoFPGrowth algo = new AlgoFPGrowth();
			final Itemsets patterns = algo.runAlgorithm(input, null,
					fpGrowthSupport);
			algo.printStats();
			patterns.printItemsets(algo.getDatabaseSize());

			// Generate association rules from FPGROWTH itemsets
			if (associationRules) {
				final AlgoAgrawalFaster94 algo2 = new AlgoAgrawalFaster94();
				final Rules rules2 = algo2.runAlgorithm(patterns, null,
						algo.getDatabaseSize(), fpGrowthMinConf,
						fpGrowthMinLift);
				rules2.printRulesWithLift(algo.getDatabaseSize());
			}
		}

	}

	/** Mine interesting itemsets */
	public static HashMap<Itemset, Double> mineItemsets(final File inputFile)
			throws IOException {

		// Set up logging
		LogManager.getLogManager().reset();
		logger.setLevel(LOGLEVEL);
		final ConsoleHandler handler = new Handler();
		handler.setLevel(Level.ALL);
		final Formatter formatter = new Formatter() {
			@Override
			public String format(final LogRecord record) {
				return record.getMessage();
			}
		};
		handler.setFormatter(formatter);
		logger.addHandler(handler);

		// Read in transaction database
		final List<Transaction> transactions = readTransactions(inputFile);

		// Determine most frequent singletons
		final Multiset<Integer> singletons = scanDatabaseToDetermineFrequencyOfSingleItems(inputFile);

		// Apply the algorithm to build the itemset tree
		final ItemsetTree tree = new ItemsetTree();
		tree.buildTree(inputFile, singletons);
		if (LOGLEVEL.equals(Level.FINE))
			tree.printStatistics();
		if (LOGLEVEL.equals(Level.FINEST)) {
			logger.finest("THIS IS THE TREE:\n");
			logger.finest(tree.toString());
		}

		// Run inference to find interesting itemsets
		logger.fine("\n============= ITEMSET INFERENCE =============\n");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				singletons.elementSet(), tree, inferenceAlg);
		if (LOGLEVEL.equals(Level.FINEST))
			logger.finest("\n======= Transaction Database =======\n"
					+ Files.toString(inputFile, Charsets.UTF_8) + "\n");
		logger.info("\n============= INTERESTING ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			logger.info(String.format("%s\tprob: %1.5f %n", entry.getKey(),
					entry.getValue()));
		}
		logger.info("\n");

		return itemsets;
	}

	/**
	 * Learn itemsets model using structural EM
	 * 
	 */
	public static HashMap<Itemset, Double> structuralEM(
			final List<Transaction> transactions,
			final Set<Integer> singletons, final ItemsetTree tree,
			final InferenceAlgorithm inferenceAlgorithm) {

		// Intialize with equiprobable singleton sets
		final LinkedHashMap<Itemset, Double> itemsets = Maps.newLinkedHashMap();
		for (final int singleton : singletons) {
			itemsets.put(new Itemset(singleton), SINGLETON_PRIOR_PROB);
		}
		logger.fine(" Initial itemsets: " + itemsets + "\n");
		double averageCost = Double.POSITIVE_INFINITY;

		// Initial parameter optimization step
		averageCost = expectationMaximizationStep(itemsets, transactions,
				inferenceAlgorithm);

		// Structural EM
		for (int iteration = 1; iteration <= MAX_STRUCTURE_ITERATIONS; iteration++) {

			// Learn structure
			logger.finer("\n+++++ Structural Optimization Step " + iteration
					+ "\n");
			averageCost = learnStructureStep(averageCost, itemsets,
					transactions, tree, inferenceAlgorithm);
			logger.finer(String.format(" Average cost: %.2f\n", averageCost));

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0) {
				logger.fine("\n***** Parameter Optimization at Step "
						+ iteration + "\n");
				averageCost = expectationMaximizationStep(itemsets,
						transactions, inferenceAlgorithm);
			}

		}

		return itemsets;
	}

	/**
	 * Find optimal parameters for given set of itemsets and store in itemsets
	 * 
	 * @return average cost per transaction
	 *         <p>
	 *         NB. zero probability itemsets are dropped
	 */
	public static double expectationMaximizationStep(
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm) {

		logger.fine(" Structure Optimal Itemsets: " + itemsets + "\n");

		double averageCost = 0;
		LinkedHashMap<Itemset, Double> prevItemsets = itemsets;
		final double n = transactions.size();

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Set up storage
			final LinkedHashMap<Itemset, Double> newItemsets = Maps
					.newLinkedHashMap();
			final Multiset<Itemset> allCoverings = ConcurrentHashMultiset
					.create();

			// Parallel E-step and M-step combined
			averageCost = 0;
			for (final Transaction transaction : transactions) {

				final LinkedHashMap<Itemset, Double> parItemsets = prevItemsets;

				final Set<Itemset> covering = Sets.newHashSet();
				final double cost = inferenceAlgorithm.infer(covering,
						parItemsets, transaction);
				averageCost += cost;
				allCoverings.addAll(covering);

			}
			averageCost = averageCost / n;

			// Normalise probabilities
			for (final Itemset set : allCoverings.elementSet()) {
				newItemsets.put(set, allCoverings.count(set) / n);
			}

			// If set has stabilised calculate norm(p_prev - p_new)
			if (prevItemsets.size() == newItemsets.size()) {
				norm = 0;
				for (final Itemset set : prevItemsets.keySet()) {
					norm += Math.pow(
							prevItemsets.get(set) - newItemsets.get(set), 2);
				}
				norm = Math.sqrt(norm);
			}

			prevItemsets = newItemsets;
		}

		itemsets.clear();
		itemsets.putAll(prevItemsets);
		logger.fine(" Parameter Optimal Itemsets: " + itemsets + "\n");
		logger.fine(String.format(" Average cost: %.2f\n", averageCost));
		assert !Double.isNaN(averageCost);
		assert !Double.isInfinite(averageCost);
		return averageCost;
	}

	public static double learnStructureStep(final double averageCost,
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions, final ItemsetTree tree,
			final InferenceAlgorithm inferenceAlgorithm) {

		// Try and find better itemset to add
		final double n = transactions.size();
		logger.finer(" Structural candidate itemsets: ");

		int iteration;
		for (iteration = 0; iteration < MAX_RANDOM_WALKS; iteration++) {

			// Candidate itemset
			final Itemset set = tree.randomWalk();
			logger.finer(set + ", ");

			// Skip empty candidates and candidates already present
			if (!set.isEmpty() && !itemsets.keySet().contains(set)) {

				logger.finer("\n potential candidate: " + set);
				// Estimate itemset probability (M-step assuming always
				// included)
				double p = 0;

				for (final Transaction transaction : transactions) {
					if (transaction.getItems().containsAll(set.getItems())) {
						p++;
					}
				}
				p = p / n;

				// Adjust probabilities for subsets of itemset
				for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
					if (set.getItems().containsAll(entry.getKey().getItems())) {
						itemsets.put(entry.getKey(), entry.getValue() - p);
					}
				}
				// Add itemset
				itemsets.put(set, p);

				// Find cost in parallel
				double curCost = 0;
				for (final Transaction transaction : transactions) {

					final Set<Itemset> covering = Sets.newHashSet();
					final double cost = inferenceAlgorithm.infer(covering,
							itemsets, transaction);
					curCost += cost;

				}
				curCost = curCost / n;
				logger.finer(String.format(", cost: %.2f", curCost));

				if (curCost < averageCost) { // found better set of itemsets
					logger.finer("\n Candidate Accepted.\n");
					return curCost;
				} // otherwise keep trying
				itemsets.remove(set);
				// and restore original probabilities
				for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
					if (set.getItems().containsAll(entry.getKey().getItems())) {
						itemsets.put(entry.getKey(), entry.getValue() + p);
					}
				}

				logger.finer("\n Structural candidate itemsets: ");
			}

		}
		logger.finer("\n");

		return averageCost;
	}

	public static List<Transaction> readTransactions(final File inputFile)
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

		return transactions;
	}

	/**
	 * This method scans the input database to calculate the support of single
	 * items.
	 * 
	 * @param inputFile
	 *            the input file
	 * @return a multiset for storing the support of each item
	 */
	private static Multiset<Integer> scanDatabaseToDetermineFrequencyOfSingleItems(
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
			final String[] lineSplited = line.split(" ");
			// for each item
			for (final String itemString : lineSplited) {
				// increase the support count of the item
				singletons.add(Integer.parseInt(itemString));
			}
		}
		// close the input file
		LineIterator.closeQuietly(it);

		return singletons;
	}

	private static List<Rule> generateAssociationRules(
			final HashMap<Itemset, Double> itemsets) {

		final List<Rule> rules = Lists.newArrayList();

		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			final HashSet<Integer> setForRecursion = Sets.newHashSet(entry
					.getKey().getItems());
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

	public static class Handler extends ConsoleHandler {
		@Override
		protected void setOutputStream(final OutputStream out)
				throws SecurityException {
			super.setOutputStream(System.out);
		}
	}

}