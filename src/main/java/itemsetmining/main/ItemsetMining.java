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
import java.util.Collections;
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
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;

public class ItemsetMining {

	private static final double SINGLETON_PRIOR_PROB = 0.5;
	private static final int OPTIMIZE_PARAMS_EVERY = 1;
	private static final int SIMPLIFY_COMBINE_ITEMSETS_EVERY = 2;
	private static final double OPTIMIZE_TOL = 1e-10;

	private static boolean APRIORI_CANDIDATE_GENERATION = false;
	private static final Logger logger = Logger.getLogger(ItemsetMining.class
			.getName());
	private static final Level LOGLEVEL = Level.ALL;

	public static void main(final String[] args) throws IOException {

		// Main function parameters
		final String dataset = "caviar.txt";
		final boolean associationRules = false;
		final InferenceAlgorithm inferenceAlg = new inferGreedy();

		// Max iterations
		final int maxStructureSteps = 1000;
		final int maxEMIterations = 10;

		// FPGrowth parameters
		final boolean fpGrowth = false;
		final double fpGrowthSupport = 0.25; // relative support
		final double fpGrowthMinConf = 0;
		final double fpGrowthMinLift = 0;

		// Find transaction database
		final URL url = ItemsetMining.class.getClassLoader().getResource(
				dataset);
		final String input = java.net.URLDecoder.decode(url.getPath(), "UTF-8");
		final File inputFile = new File(input);

		// Mine interesting itemsets
		final HashMap<Itemset, Double> itemsets = mineItemsets(inputFile,
				inferenceAlg, maxStructureSteps, maxEMIterations);

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
	public static HashMap<Itemset, Double> mineItemsets(final File inputFile,
			final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations)
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
				singletons.elementSet(), tree, inferenceAlgorithm,
				maxStructureSteps, maxEMIterations);
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
	private static HashMap<Itemset, Double> structuralEM(
			final List<Transaction> transactions,
			final Set<Integer> singletons, final ItemsetTree tree,
			final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations) {

		// Intialize with equiprobable singleton sets
		final LinkedHashMap<Itemset, Double> itemsets = Maps.newLinkedHashMap();
		for (final int singleton : singletons) {
			itemsets.put(new Itemset(singleton), SINGLETON_PRIOR_PROB);
		}
		logger.fine(" Initial itemsets: " + itemsets + "\n");
		double averageCost = Double.POSITIVE_INFINITY;

		// Convert singletons to itemsets if using Apriori candidate generation
		final List<Itemset> candidates = Lists.newArrayList();
		if (APRIORI_CANDIDATE_GENERATION) {
			for (final int singleton : singletons)
				candidates.add(new Itemset(singleton));
		}

		// Initialize list of rejected sets
		final Set<Itemset> rejected_sets = Sets.newHashSet();

		// Initial parameter optimization step
		averageCost = expectationMaximizationStep(itemsets, transactions,
				inferenceAlgorithm);

		// Structural EM
		for (int iteration = 1; iteration <= maxEMIterations; iteration++) {

			// Learn structure
			if (APRIORI_CANDIDATE_GENERATION) {
				logger.finer("\n+++++ Apriori Structural Optimization at Step "
						+ iteration + "\n");
				averageCost = learnStructureAprioriStep(averageCost, itemsets,
						transactions, candidates, inferenceAlgorithm,
						maxStructureSteps);
				if (averageCost == -1) // apriori finished
					break;
			} else if (iteration % SIMPLIFY_COMBINE_ITEMSETS_EVERY == 0) {
				logger.finer("\n----- Itemset Simplification at Step "
						+ iteration + "\n"); // TODO use dedicated maxSteps
												// parameter
				averageCost = simplifyItemsetsStep(averageCost, itemsets,
						transactions, rejected_sets, inferenceAlgorithm,
						maxStructureSteps);

				logger.finer("\n----- Itemset Combination at Step " + iteration
						+ "\n");
				averageCost = combineItemsetsStep(averageCost, itemsets,
						transactions, rejected_sets, inferenceAlgorithm,
						maxStructureSteps);
			} else {
				logger.finer("\n+++++ Tree Structural Optimization at Step "
						+ iteration + "\n");
				averageCost = learnStructureStep(averageCost, itemsets,
						transactions, tree, rejected_sets, inferenceAlgorithm,
						maxStructureSteps);
				if (averageCost == -1) // structure iteration limit exceeded
					break;
			}
			logger.finer(String.format(" Average cost: %.2f\n", averageCost));

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0) {
				logger.fine("\n***** Parameter Optimization at Step "
						+ iteration + "\n");
				averageCost = expectationMaximizationStep(itemsets,
						transactions, inferenceAlgorithm);
			}

		}

		if (averageCost == -1) {
			logger.finer("\n----- Final Itemset Simplification\n");
			averageCost = simplifyItemsetsStep(averageCost, itemsets,
					transactions, rejected_sets, inferenceAlgorithm,
					maxStructureSteps);
			logger.finer("\n----- Final Itemset Combination\n");
			averageCost = combineItemsetsStep(averageCost, itemsets,
					transactions, rejected_sets, inferenceAlgorithm,
					maxStructureSteps);
			logger.fine("\n***** Final parameter Optimization\n");
			averageCost = expectationMaximizationStep(itemsets, transactions,
					inferenceAlgorithm);
		} else
			logger.warning("\nEM iteration limit exceeded.\n");
		return itemsets;
	}

	/**
	 * Find optimal parameters for given set of itemsets and store in itemsets
	 * 
	 * @return average cost per transaction
	 *         <p>
	 *         NB. zero probability itemsets are dropped
	 */
	private static double expectationMaximizationStep(
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

				// final LinkedHashMap<Itemset, Double> parItemsets =
				// prevItemsets;

				final Set<Itemset> covering = Sets.newHashSet();
				final double cost = inferenceAlgorithm.infer(covering,
						prevItemsets, transaction);
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

	/** Generate candidate itemsets from Itemset tree */
	private static double learnStructureStep(final double averageCost,
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions, final ItemsetTree tree,
			final Set<Itemset> rejected_sets,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps) {

		// Try and find better itemset to add
		logger.finer(" Structural candidate itemsets: ");

		int iteration;
		for (iteration = 0; iteration < maxSteps; iteration++) {

			// Generate candidate itemset
			final Itemset candidate = tree.randomWalk();
			logger.finer(candidate + ", ");

			// Evaluate candidate itemset
			if (!rejected_sets.contains(candidate)) {
				final Double betterCost = evaluateCandidate(averageCost,
						itemsets, transactions, inferenceAlgorithm, candidate);
				if (betterCost != null) // Better itemset found
					return betterCost;
				else
					rejected_sets.add(candidate); // otherwise add to rejected
			}

		}

		// No better itemset found
		logger.warning("\n\n Structure iteration limit exceeded. No better candidate found.\n");
		return -1;
	}

	/** Generate candidate itemsets from power set */
	private static double simplifyItemsetsStep(final double averageCost,
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions,
			final Set<Itemset> rejected_sets,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps) {

		// Try and find better itemset to add
		logger.finer(" Structural candidate itemsets: ");

		// Sort itemsets from largest to smallest // TODO skip sorting?
		final List<Itemset> sortedItemsets = Lists.newArrayList(itemsets
				.keySet());
		Collections.sort(sortedItemsets,
				new orderBySize().reverse()
						.compound((Ordering.usingToString())));

		// Suggest powersets for all itemsets
		int iteration = 0;
		for (final Itemset set : sortedItemsets) {

			final Set<Set<Integer>> powerset = Sets.powerSet(set.getItems());
			for (final Set<Integer> subset : powerset) {

				// Evaluate candidate itemset
				final Itemset candidate = new Itemset(subset);
				if (!rejected_sets.contains(candidate)) {
					final Double betterCost = evaluateCandidate(averageCost,
							itemsets, transactions, inferenceAlgorithm,
							candidate);
					if (betterCost != null) // Better itemset found
						return betterCost;
					else
						rejected_sets.add(candidate); // otherwise add to
														// rejected
				}

				iteration++;
				if (iteration > maxSteps) { // Iteration limit exceeded
					logger.warning("\n Simplify iteration limit exceeded.\n");
					return averageCost; // No better itemset found
				}

			}
		}

		// No better itemset found
		logger.finer("\n No better candidate found.\n");
		return averageCost;
	}

	/** Generate candidate itemsets by combining existing sets */
	private static double combineItemsetsStep(final double averageCost,
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions,
			final Set<Itemset> rejected_sets,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps) {

		// Try and find better itemset to add
		logger.finer(" Structural candidate itemsets: ");

		// Sort itemsets from smallest to largest // TODO skip sorting?
		final List<Itemset> sortedItemsets = Lists.newArrayList(itemsets
				.keySet());
		Collections.sort(sortedItemsets,
				new orderBySize().compound((Ordering.usingToString())));

		// Suggest supersets for all itemsets
		int iteration = 0;
		for (int i = 0; i < sortedItemsets.size(); i++) {
			final HashSet<Integer> itemset1 = sortedItemsets.get(i).getItems();
			for (int j = i + 1; j < sortedItemsets.size(); j++) {
				final HashSet<Integer> itemset2 = sortedItemsets.get(j)
						.getItems();

				// Create a new candidate by combining itemsets
				// TODO store itemset as sorted list to prevent duplicates?
				final Itemset candidate = new Itemset(itemset1);
				candidate.add(itemset2);

				// Evaluate candidate itemset
				if (!rejected_sets.contains(candidate)) {
					final Double betterCost = evaluateCandidate(averageCost,
							itemsets, transactions, inferenceAlgorithm,
							candidate);
					if (betterCost != null) // Better itemset found
						return betterCost;
					else
						rejected_sets.add(candidate); // otherwise add to
														// rejected
				}

				iteration++;
				if (iteration > maxSteps) { // Iteration limit exceeded
					logger.warning("\n Combine iteration limit exceeded.\n");
					return averageCost; // No better itemset found
				}

			}
		}

		// No better itemset found
		logger.finer("\n No better candidate found.\n");
		return averageCost;
	}

	/** Generate candidate itemsets Apriori style from singletons */
	private static double learnStructureAprioriStep(final double averageCost,
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions,
			final List<Itemset> candidates,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps) {

		// Try and find better itemset to add
		logger.finer(" Structural candidate itemsets: ");

		// Generate candidates of size k from sets of size k-1
		int iteration = 0;
		List<Itemset> prevCandidates = candidates;
		while (!prevCandidates.isEmpty()) {

			// For each pair of itemsets of size k-1
			final List<Itemset> newCandidates = Lists.newArrayList();
			for (int i = 0; i < prevCandidates.size(); i++) {
				final HashSet<Integer> itemset1 = prevCandidates.get(i)
						.getItems();
				for (int j = i + 1; j < prevCandidates.size(); j++) {
					final HashSet<Integer> itemset2 = prevCandidates.get(j)
							.getItems();

					// Create a new candidate by combining itemsets
					// TODO store itemset as sorted list to prevent duplicates?
					final Itemset candidate = new Itemset(itemset1);
					candidate.add(itemset2);

					if (!newCandidates.contains(candidate)) {

						// Evaluate candidate itemset
						final Double betterCost = evaluateCandidate(
								averageCost, itemsets, transactions,
								inferenceAlgorithm, candidate);
						if (betterCost != null) { // Better itemset found
							if (prevCandidates != candidates) {
								candidates.clear();
								candidates.addAll(prevCandidates);
							}
							return betterCost;
						}

						// Add candidate to level k
						newCandidates.add(candidate);

					}

					iteration++;
					if (iteration > maxSteps) { // Iteration limit exceeded
						logger.warning("\n Structure iteration limit exceeded. No better candidate found.\n");
						return -1; // No better itemset found
					}

				}
			}

			prevCandidates = newCandidates;
		}

		// No better itemset found
		logger.finer("\n All possible candidates found.\n");
		return -1;
	}

	/** Evaluate a candidate itemset to see if it should be included */
	private static Double evaluateCandidate(final double averageCost,
			final LinkedHashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {

		// Skip empty candidates and candidates already present
		// TODO can probs skip itemset.contains now that we use reject list
		if (!candidate.isEmpty() && !itemsets.keySet().contains(candidate)) {

			logger.finer("\n potential candidate: " + candidate);
			final double n = transactions.size();
			// Estimate itemset probability (M-step assuming always
			// included)
			double p = 0;

			for (final Transaction transaction : transactions) {
				if (transaction.getItems().containsAll(candidate.getItems())) {
					p++;
				}
			}
			p = p / n;

			// Adjust probabilities for subsets of itemset
			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
				if (candidate.getItems().containsAll(entry.getKey().getItems())) {
					itemsets.put(entry.getKey(), entry.getValue() - p);
				}
			}
			// Add itemset
			itemsets.put(candidate, p);

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
			itemsets.remove(candidate);
			// and restore original probabilities
			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
				if (candidate.getItems().containsAll(entry.getKey().getItems())) {
					itemsets.put(entry.getKey(), entry.getValue() + p);
				}
			}

			logger.finer("\n Structural candidate itemsets: ");
		}
		// No better candidate found
		return null;
	}

	private static List<Transaction> readTransactions(final File inputFile)
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

	private static class orderBySize extends Ordering<Itemset> {
		@Override
		public int compare(final Itemset set1, final Itemset set2) {
			return Ints.compare(set1.getItems().size(), set2.getItems().size());
		}
	};

	public static class Handler extends ConsoleHandler {
		@Override
		protected void setOutputStream(final OutputStream out)
				throws SecurityException {
			super.setOutputStream(System.out);
		}
	}

}