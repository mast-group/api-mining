package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.InferenceAlgorithms.InferILP;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.TransactionDatabase;
import itemsetmining.transaction.TransactionRDD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static final boolean ITEMSET_CACHE = true;
	private static final boolean SERIAL = false;
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
			TransactionDatabase transactions,
			final Multiset<Integer> singletons, final ItemsetTree tree,
			final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations) {

		// Start timer
		final long startTime = System.currentTimeMillis();

		// Initialize itemset cache
		final long noTransactions = transactions.size();
		if (ITEMSET_CACHE) {
			if (transactions instanceof TransactionRDD) {
				transactions = SparkCacheFunctions.parallelInitializeCache(
						transactions, singletons);
			} else if (SERIAL) {
				CacheFunctions.serialInitializeCache(
						transactions.getTransactionList(), noTransactions,
						singletons);
			} else {
				CacheFunctions.parallelInitializeCache(
						transactions.getTransactionList(), noTransactions,
						singletons);
			}
		}

		// Intialize itemsets with singleton sets and their relative support
		// as well as supports with singletons and their actual supports
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();
		final HashMap<Itemset, Integer> supports = Maps.newHashMap();
		for (final Multiset.Entry<Integer> entry : singletons.entrySet()) {
			final Itemset set = new Itemset(entry.getElement());
			final int support = entry.getCount();
			itemsets.put(set, support / (double) noTransactions);
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

		// Structural EM
		boolean breakLoop = false;
		for (int iteration = 1; iteration <= maxEMIterations; iteration++) {

			// Learn structure
			if (iteration % COMBINE_ITEMSETS_EVERY == 0) {
				logger.finer("\n----- Itemset Combination at Step " + iteration
						+ "\n");
				transactions = combineItemsetsStep(itemsets, transactions,
						tree, rejected_sets, inferenceAlgorithm,
						maxStructureSteps, supportOrdering, supports);
				if (transactions.getIterationLimitExceeded())
					breakLoop = true;
			} else {
				logger.finer("\n+++++ Tree Structural Optimization at Step "
						+ iteration + "\n");
				transactions = learnStructureStep(itemsets, transactions, tree,
						rejected_sets, inferenceAlgorithm, maxStructureSteps);
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
				transactions = expectationMaximizationStep(itemsets,
						transactions, inferenceAlgorithm);
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
	private static TransactionDatabase expectationMaximizationStep(
			final HashMap<Itemset, Double> itemsets,
			TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm) {

		logger.fine(" Structure Optimal Itemsets: " + itemsets + "\n");

		double averageCost = 0;
		HashMap<Itemset, Double> prevItemsets = itemsets;
		final double noTransactions = transactions.size();

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Use cache in inference algorithm by not passing prevItemsets
			final HashMap<Itemset, Double> passItemsets;
			if (ITEMSET_CACHE)
				passItemsets = null;
			else
				passItemsets = prevItemsets;

			// Set up storage
			final HashMap<Itemset, Double> newItemsets = Maps.newHashMap();

			// Parallel E-step and M-step combined
			if (transactions instanceof TransactionRDD) {
				averageCost = SparkEMStep.parallelEMStep(
						transactions.getTransactionRDD(), inferenceAlgorithm,
						passItemsets, noTransactions, newItemsets);
				if (ITEMSET_CACHE)
					transactions = SparkCacheFunctions
							.parallelUpdateCacheProbabilities(transactions,
									newItemsets);
			} else if (SERIAL || inferenceAlgorithm instanceof InferILP) {
				averageCost = EMStep.serialEMStep(
						transactions.getTransactionList(), inferenceAlgorithm,
						passItemsets, noTransactions, newItemsets);
				if (ITEMSET_CACHE)
					CacheFunctions.serialUpdateCacheProbabilities(
							transactions.getTransactionList(), newItemsets);
			} else {
				averageCost = EMStep.parallelEMStep(
						transactions.getTransactionList(), inferenceAlgorithm,
						passItemsets, noTransactions, newItemsets);
				if (ITEMSET_CACHE)
					CacheFunctions.parallelUpdateCacheProbabilities(
							transactions.getTransactionList(), newItemsets);
				// checkCacheWorks(averageCost, averageCostNoCache);
			}

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

		itemsets.clear();
		itemsets.putAll(prevItemsets);
		logger.fine(" Parameter Optimal Itemsets: " + itemsets + "\n");
		logger.fine(String.format(" Average cost: %.2f%n", averageCost));
		assert !Double.isNaN(averageCost);
		assert !Double.isInfinite(averageCost);

		// Update average cost for transactions
		transactions.setAverageCost(averageCost);

		return transactions;
	}

	/** Generate candidate itemsets from Itemset tree */
	private static TransactionDatabase learnStructureStep(
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
				final TransactionDatabase betterCost = evaluateCandidate(
						itemsets, transactions, tree, inferenceAlgorithm,
						candidate);
				if (betterCost != null) // Better itemset found
					return betterCost;
				rejected_sets.add(candidate); // otherwise add to rejected
				logger.finer("\n Structural candidate itemsets: ");
			}

		}

		// No better itemset found
		logger.warning("\n\n Structure iteration limit exceeded. No better candidate found.\n");
		transactions.setIterationLimitExceeded();
		return transactions;
	}

	/**
	 * Generate candidate itemsets by combining existing sets with highest order
	 *
	 * @param itemsetOrdering
	 *            ordering that determines which itemsets to combine first
	 */
	private static TransactionDatabase combineItemsetsStep(
			final HashMap<Itemset, Double> itemsets,
			final TransactionDatabase transactions, final ItemsetTree tree,
			final Set<Itemset> rejected_sets,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps,
			final Ordering<Itemset> itemsetOrdering,
			final HashMap<Itemset, Integer> supports) {

		// Try and find better itemset to add
		// logger.finest(" Structural candidate itemsets: ");

		// Sort itemsets according to given ordering
		// TODO store itemset as sorted list to prevent re-sorting?
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
							final TransactionDatabase betterCost = evaluateCandidate(
									itemsets, transactions, tree,
									inferenceAlgorithm, candidate);
							if (betterCost != null) { // Better itemset found
								// update supports
								supports.put(candidate, (int) (itemsets
										.get(candidate) * transactions.size()));
								return betterCost;
							}
							// logger.finest("\n Structural candidate itemsets: ");
						}

						iteration++;
						if (iteration > maxSteps) { // Iteration limit exceeded
							logger.warning("\n Combine iteration limit exceeded.\n");
							return transactions; // No better itemset found
						}

					}
				}
			}
		}

		// No better itemset found
		logger.info("\n All possible candidates suggested. Exiting. \n");
		transactions.setIterationLimitExceeded();
		return transactions;
	}

	/** Evaluate a candidate itemset to see if it should be included */
	private static TransactionDatabase evaluateCandidate(
			final HashMap<Itemset, Double> itemsets,
			TransactionDatabase transactions, final ItemsetTree tree,
			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {

		logger.finer("\n Candidate: " + candidate);
		final double noTransactions = transactions.size();

		// Calculate itemset support (M-step assuming always included)
		final double p = tree.getSupportOfItemset(candidate) / noTransactions;

		// Find direct subsets of candidate
		final Set<Itemset> subsets = getDirectSubsets(itemsets.keySet(),
				candidate);

		// If not using cache: Add candidate to itemsets
		HashMap<Itemset, Double> negativeItemsets;
		if (!ITEMSET_CACHE)
			negativeItemsets = addCandidateItemsets(itemsets, candidate, p,
					subsets);

		// Use cache in inference algorithm by not passing itemsets
		final HashMap<Itemset, Double> passItemsets;
		if (ITEMSET_CACHE)
			passItemsets = null;
		else
			passItemsets = itemsets;

		// Find cost in parallel
		double curCost = 0;
		if (transactions instanceof TransactionRDD) {
			curCost = SparkEMStep.parallelEMStep(
					transactions.getTransactionRDD(), inferenceAlgorithm,
					passItemsets, noTransactions, candidate, p, subsets);
		} else if (SERIAL || inferenceAlgorithm instanceof InferILP) {
			curCost = EMStep.serialEMStep(transactions.getTransactionList(),
					inferenceAlgorithm, passItemsets, noTransactions,
					candidate, p, subsets);
		} else {
			curCost = EMStep.parallelEMStep(transactions.getTransactionList(),
					inferenceAlgorithm, passItemsets, noTransactions,
					candidate, p, subsets);
			// checkCacheWorks(curCost, curCostNoCache);
		}
		logger.finer(String.format(", cost: %.2f", curCost));

		// Return if better set of itemsets found
		if (curCost < transactions.getAverageCost()) {
			logger.finer("\n Candidate Accepted.\n");
			if (ITEMSET_CACHE) {
				// Update cache with candidate
				if (transactions instanceof TransactionRDD) {
					transactions = SparkCacheFunctions.parallelAddItemsetCache(
							transactions, candidate, p, subsets);
				} else if (SERIAL) {
					CacheFunctions.serialAddItemsetCache(
							transactions.getTransactionList(), candidate, p,
							subsets);
				} else {
					CacheFunctions.parallelAddItemsetCache(
							transactions.getTransactionList(), candidate, p,
							subsets);
				}
				// Update itemsets with candidate
				addAcceptedCandidateItemsets(itemsets, candidate, p, subsets);
			}
			transactions.setAverageCost(curCost);
			return transactions;
		} // otherwise keep trying

		// If not using cache: Remove candidate from itemsets
		if (!ITEMSET_CACHE)
			removeCandidateItemsets(itemsets, candidate, p, subsets,
					negativeItemsets);

		// No better candidate found
		return null;
	}

	/** Very useful for debugging */
	@SuppressWarnings("unused")
	private static void checkCacheWorks(final double curCost,
			final double curCostNoCache) {

		if (Math.abs(curCost - curCostNoCache) > 1e-12)
			logger.severe("\nCosts do not match!! N.C: " + curCostNoCache
					+ " C:" + curCost);
		else
			logger.info("\nCosts match.");
	}

	public static void addAcceptedCandidateItemsets(
			final HashMap<Itemset, Double> itemsets, final Itemset candidate,
			final double p, final Set<Itemset> subsets) {

		// Adjust probabilities for direct subsets of itemset
		for (final Itemset subset : subsets) {
			final double newProb = itemsets.get(subset) - p;
			if (newProb > 0.0)
				itemsets.put(subset, newProb);
			else
				itemsets.put(subset, 1e-10);
		}

		// Add candidate
		itemsets.put(candidate, p);
	}

	public static HashMap<Itemset, Double> addCandidateItemsets(
			final HashMap<Itemset, Double> itemsets, final Itemset candidate,
			final double p, final Set<Itemset> subsets) {

		// Storage for negative probabilities
		final HashMap<Itemset, Double> negativeItemsets = Maps.newHashMap();

		// Adjust probabilities for direct subsets of itemset
		for (final Itemset subset : subsets) {
			final double newProb = itemsets.get(subset) - p;
			if (newProb > 0.0) {
				itemsets.put(subset, newProb);
			} else {
				negativeItemsets.put(subset, newProb);
				itemsets.put(subset, 1e-10);
			}
		}

		// Add candidate
		itemsets.put(candidate, p);

		return negativeItemsets;
	}

	public static void removeCandidateItemsets(
			final HashMap<Itemset, Double> itemsets, final Itemset candidate,
			final double p, final Set<Itemset> subsets,
			final HashMap<Itemset, Double> negativeItemsets) {

		// Remove candidate
		itemsets.remove(candidate);

		// Restore negative probs
		itemsets.putAll(negativeItemsets);

		// and restore original probabilities
		for (final Itemset subset : subsets) {
			final double prob = itemsets.get(subset);
			itemsets.put(subset, prob + p);
		}
	}

	/** Find all itemsets that are direct subsets of candidate itemset */
	static Set<Itemset> getDirectSubsets(final Set<Itemset> itemsets,
			final Itemset candidate) {

		// Find all subsets
		final Set<Itemset> subsets = Sets.newHashSet();
		for (final Itemset set : itemsets) {
			if (candidate.contains(set))
				subsets.add(set);
		}

		// Remove subsets with supersets
		final Set<Itemset> directSubsets = Sets.newHashSet();
		for (final Itemset set : subsets) {
			boolean isDirectSubset = true;
			for (final Itemset otherSet : subsets) {
				if (!otherSet.equals(set) && otherSet.contains(set)) {
					isDirectSubset = false; // set has superset
					break;
				}
			}
			if (isDirectSubset)
				directSubsets.add(set);
		}

		return directSubsets;
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
