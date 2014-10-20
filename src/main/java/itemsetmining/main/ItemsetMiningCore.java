package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.TransactionDatabase;
import itemsetmining.transaction.TransactionRDD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import scala.Tuple2;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public abstract class ItemsetMiningCore {

	/** Main fixed settings */
	private static final int OPTIMIZE_PARAMS_EVERY = 1;
	private static final int COMBINE_ITEMSETS_EVERY = 1;
	private static final double OPTIMIZE_TOL = 1e-5;

	protected static final Logger logger = Logger
			.getLogger(ItemsetMiningCore.class.getName());
	public static final String LOG_DIR = "/disk/data1/jfowkes/logs/";

	/** Variable settings */
	protected static Level LOG_LEVEL = Level.FINE;
	protected static boolean TIMESTAMP_LOG = true;
	protected static long MAX_RUNTIME = 12 * 60 * 60 * 1_000; // 12hrs

	/**
	 * Learn itemsets model using structural EM
	 */
	protected static HashMap<Itemset, Double> structuralEM(
			final TransactionDatabase transactions,
			final Multiset<Integer> singletons, final ItemsetTree tree,
			final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations) {

		// Start timer
		final long startTime = System.currentTimeMillis();

		// Initialize itemset cache
		if (transactions instanceof TransactionRDD) {
			SparkEMStep.initializeCachedItemsets(transactions,
					singletons);
		} else {
			EMStep.initializeCachedItemsets(transactions, singletons);
		}

		// Intialize itemsets with singleton sets and their relative support
		// as well as supports with singletons and their actual supports
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();
		final HashMap<Itemset, Integer> supports = Maps.newHashMap();
		for (final Multiset.Entry<Integer> entry : singletons.entrySet()) {
			final Itemset set = new Itemset(entry.getElement());
			final int support = entry.getCount();
			itemsets.put(set, support / (double) transactions.size());
			supports.put(set, support);
		}
		logger.fine(" Initial itemsets: " + itemsets + "\n");

		// Initialize list of rejected sets
		final Set<Itemset> rejected_sets = Sets.newHashSet();

		// Define decreasing support ordering
		final Ordering<Itemset> supportOrdering = new Ordering<Itemset>() {
			@Override
			public int compare(final Itemset set1, final Itemset set2) {
				return supports.get(set2) - supports.get(set1);
			}
		}.compound(Ordering.usingToString());

		// Initialize average cost per transaction for singletons
		expectationMaximizationStep(itemsets, transactions, inferenceAlgorithm);

		// Structural EM
		boolean breakLoop = false;
		for (int iteration = 1; iteration <= maxEMIterations; iteration++) {

			// Learn structure
			if (iteration % COMBINE_ITEMSETS_EVERY == 0) {
				logger.finer("\n----- Itemset Combination at Step " + iteration
						+ "\n");
				combineItemsetsStep(itemsets, transactions, tree,
						rejected_sets, inferenceAlgorithm, maxStructureSteps,
						supportOrdering, supports);
				if (transactions.getIterationLimitExceeded())
					breakLoop = true;
			} else {
				logger.finer("\n+++++ Tree Structural Optimization at Step "
						+ iteration + "\n");
				learnStructureStep(itemsets, transactions, tree, rejected_sets,
						inferenceAlgorithm, maxStructureSteps);
				if (transactions.getIterationLimitExceeded())
					breakLoop = true;
			}
			logger.finer(String.format(" Average cost: %.2f%n",
					transactions.getAverageCost()));

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0
					|| iteration == maxEMIterations || breakLoop == true) {
				logger.fine("\n***** Parameter Optimization at Step "
						+ iteration + "\n");
				expectationMaximizationStep(itemsets, transactions,
						inferenceAlgorithm);
			}

			// Break loop if requested
			if (breakLoop)
				break;

			// Check if time exceeded
			if (System.currentTimeMillis() - startTime > MAX_RUNTIME) {
				logger.warning("\nRuntime limit of " + MAX_RUNTIME
						/ (60. * 1000.) + " minutes exceeded.\n");
				break;
			}

			// Checkpoint every 100 iterations to avoid StackOverflow errors due
			// to long lineage (http://tinyurl.com/ouswhrc)
			if (iteration % 100 == 0 && transactions instanceof TransactionRDD) {
				transactions.getTransactionRDD().cache();
				transactions.getTransactionRDD().checkpoint();
				transactions.getTransactionRDD().count();
			}

			if (iteration == maxEMIterations)
				logger.warning("\nEM iteration limit exceeded.\n");
		}
		logger.info("\nElapsed time: "
				+ (System.currentTimeMillis() - startTime) / (60. * 1000.)
				+ " minutes.\n");

		return itemsets;
	}

	/**
	 * Find optimal parameters for given set of itemsets and store in itemsets
	 *
	 * @return TransactionDatabase with the average cost per transaction
	 *         <p>
	 *         NB. zero probability itemsets are dropped
	 */
	private static void expectationMaximizationStep(
			final HashMap<Itemset, Double> itemsets,
			final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm) {

		logger.fine(" Structure Optimal Itemsets: " + itemsets + "\n");

		Map<Itemset, Double> prevItemsets = itemsets;

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Set up storage
			final Map<Itemset, Double> newItemsets;

			// Parallel E-step and M-step combined
			if (transactions instanceof TransactionRDD)
				newItemsets = SparkEMStep.hardEMStep(transactions,
						inferenceAlgorithm);
			else
				newItemsets = EMStep.hardEMStep(
						transactions.getTransactionList(), inferenceAlgorithm);

			// If set has stabilised calculate norm(p_prev - p_new)
			if (prevItemsets.keySet().equals(newItemsets.keySet())) {
				norm = 0;
				for (final Itemset set : prevItemsets.keySet()) {
					norm += Math.pow(
							prevItemsets.get(set) - newItemsets.get(set), 2);
				}
				norm = Math.sqrt(norm);
			}

			prevItemsets = newItemsets;
		}

		// Calculate average cost of last covering
		if (transactions instanceof TransactionRDD)
			SparkEMStep.calculateAndSetAverageCost(transactions);
		else
			EMStep.calculateAndSetAverageCost(transactions);

		itemsets.clear();
		itemsets.putAll(prevItemsets);
		logger.fine(" Parameter Optimal Itemsets: " + itemsets + "\n");
		logger.fine(String.format(" Average cost: %.2f%n",
				transactions.getAverageCost()));
	}

	/** Generate candidate itemsets from Itemset tree */
	private static void learnStructureStep(
			final HashMap<Itemset, Double> itemsets,
			final TransactionDatabase transactions, final ItemsetTree tree,
			final Set<Itemset> rejected_sets,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps) {

		// Try and find better itemset to add
		logger.finer(" Structural candidate itemsets: ");

		int iteration;
		for (iteration = 0; iteration < maxSteps; iteration++) {

			// Generate candidate itemset
			final Itemset candidate = tree.randomWalk();
			logger.finer(candidate + ", ");

			// Evaluate candidate itemset (skipping empty candidates)
			if (!rejected_sets.contains(candidate) && !candidate.isEmpty()) {
				// Skip candidates already present
				if (itemsets.keySet().contains(candidate)) {
					rejected_sets.add(candidate);
					continue;
				}
				final boolean accepted = evaluateCandidate(itemsets,
						transactions, inferenceAlgorithm, candidate);
				if (accepted == true) // Better itemset found
					return;
				rejected_sets.add(candidate); // otherwise add to rejected
				logger.finer("\n Structural candidate itemsets: ");
			}

		}

		// No better itemset found
		logger.warning("\n\n Structure iteration limit exceeded. No better candidate found.\n");
		transactions.setIterationLimitExceeded();
	}

	/**
	 * Generate candidate itemsets by combining existing sets with highest order
	 *
	 * @param itemsetOrdering
	 *            ordering that determines which itemsets to combine first
	 */
	private static void combineItemsetsStep(
			final HashMap<Itemset, Double> itemsets,
			final TransactionDatabase transactions, final ItemsetTree tree,
			final Set<Itemset> rejected_sets,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps,
			final Ordering<Itemset> itemsetOrdering,
			final HashMap<Itemset, Integer> supports) {

		// Try and find better itemset to add
		// logger.finest(" Structural candidate itemsets: ");

		// Sort itemsets according to given ordering
		final ArrayList<Itemset> sortedItemsets = Lists.newArrayList(itemsets
				.keySet());
		Collections.sort(sortedItemsets, itemsetOrdering);

		// Suggest supersets for all itemsets
		int iteration = 0;
		final int len = sortedItemsets.size();
		for (int k = 0; k < 2 * len - 2; k++) {
			for (int i = 0; i < len && i < k + 1; i++) {
				for (int j = i + 1; j < len && i + j < k + 1; j++) {
					if (k <= i + j) {

						// Create a new candidate by combining itemsets
						final Itemset candidate = new Itemset();
						candidate.add(sortedItemsets.get(i));
						candidate.add(sortedItemsets.get(j));
						// logger.finest(candidate + ", ");

						// Evaluate candidate itemset
						if (!rejected_sets.contains(candidate)) {
							rejected_sets.add(candidate); // candidate seen
							final boolean accepted = evaluateCandidate(
									itemsets, transactions, inferenceAlgorithm,
									candidate);
							if (accepted == true) { // Better itemset found
								// update supports
								supports.put(candidate,
										tree.getSupportOfItemset(candidate));
								return;
							}
							// logger.finest("\n Structural candidate itemsets: ");
						}

						iteration++;
						if (iteration > maxSteps) { // Iteration limit exceeded
							logger.warning("\n Combine iteration limit exceeded.\n");
							return; // No better itemset found
						}

					}
				}
			}
		}

		// No better itemset found
		logger.info("\n All possible candidates suggested. Exiting. \n");
		transactions.setIterationLimitExceeded();
	}

	/** Evaluate a candidate itemset to see if it should be included */
	private static boolean evaluateCandidate(
			final HashMap<Itemset, Double> itemsets,
			final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {

		logger.finer("\n Candidate: " + candidate);

		// Find cost in parallel
		Tuple2<Double, Double> costAndProb;
		if (transactions instanceof TransactionRDD) {
			costAndProb = SparkEMStep.structuralEMStep(transactions,
					inferenceAlgorithm, candidate);
		} else {
			costAndProb = EMStep.structuralEMStep(transactions,
					inferenceAlgorithm, candidate);
		}
		final double curCost = costAndProb._1;
		final double prob = costAndProb._2;
		logger.finer(String.format(", cost: %.2f", curCost));

		// Return if better set of itemsets found
		if (curCost < transactions.getAverageCost()) {
			logger.finer("\n Candidate Accepted.\n");
			// Update cache with candidate
			Map<Itemset, Double> newItemsets;
			if (transactions instanceof TransactionRDD) {
				newItemsets = SparkEMStep.addAcceptedCandidateCache(
						transactions, candidate, prob);
			} else {
				newItemsets = EMStep.addAcceptedCandidateCache(
						transactions, candidate, prob);
			}
			// Update itemsets with newly inferred itemsets
			itemsets.clear();
			itemsets.putAll(newItemsets);
			transactions.setAverageCost(curCost);
			return true;
		} // otherwise keep trying

		// No better candidate found
		return false;
	}

	/**
	 * Calculate interestingness as defined by i(S) = |z_S = 1|/|T : S in T|
	 * where |z_S = 1| is calculated by pi_S*|T| and |T : S in T| = supp(S)
	 */
	protected static HashMap<Itemset, Double> calculateInterestingness(
			final HashMap<Itemset, Double> itemsets,
			final TransactionDatabase transactions, final ItemsetTree tree) {

		final HashMap<Itemset, Double> interestingnessMap = Maps.newHashMap();

		// Calculate interestingness
		final long noTransactions = transactions.size();
		for (final Itemset set : itemsets.keySet()) {
			final double interestingness = itemsets.get(set) * noTransactions
					/ (double) tree.getSupportOfItemset(set);
			interestingnessMap.put(set, interestingness);
		}

		return interestingnessMap;
	}

}
