package itemsetmining.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetMining {

	private static final int STRUCTURAL_EM_ITERATIONS = 100;
	private static final int OPTIMIZE_PARAMS_EVERY = 5;
	private static final int OPTIMIZE_PARAMS_BURNIN = 5;

	private static final int OPTIMIZE_ITERATIONS = 100;
	private static final int MAX_RANDOM_WALKS = 100;
	private static int LEN_INIT = 5; // No. of items to initialise

	private static final Random rand = new Random();

	// TODO don't read all transactions into memory
	public static void main(final String[] args) throws IOException {

		// Read in transaction database
		final URL url = ItemsetMining.class.getClassLoader().getResource(
				"contextPasquier99.txt");
		// final URL url = ItemsetMining.class.getClassLoader().getResource(
		// "chess.txt");
		final String input = java.net.URLDecoder.decode(url.getPath(), "UTF-8");
		System.out.println("======= Input Transactions =======\n"
				+ Files.toString(new File(input), Charsets.UTF_8));
		final List<Transaction> transactions = readTransactions(input);

		// Apply the algorithm to build the itemset tree
		final ItemsetTree tree = new ItemsetTree();
		tree.buildTree(input);
		tree.printStatistics();
		System.out.println("THIS IS THE TREE:");
		tree.printTree();

		// Run inference to find interesting itemsets
		System.out.println("============= ITEMSET INFERENCE =============");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				LEN_INIT, tree, STRUCTURAL_EM_ITERATIONS);
		System.out
				.println("\n============= INTERESTING ITEMSETS =============\n"
						+ itemsets + "\n");
		System.out.println("======= Input Transactions =======\n"
				+ Files.toString(new File(input), Charsets.UTF_8) + "\n");

		// Compare with the FPGROWTH algorithm (we use a relative support)
		final double minsup = 0.4; // means a minsup of 2 transaction
		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(input, null, minsup);
		algo.printStats();
		patterns.printItemsets(algo.getDatabaseSize());

	}

	/** Learn itemsets model using structural EM */
	public static HashMap<Itemset, Double> structuralEM(
			final List<Transaction> transactions, final int lenUniverse,
			final ItemsetTree tree, final int iterations) {

		// Intialize with equiprobable singleton sets
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();
		for (int i = 0; i < lenUniverse; i++) {
			itemsets.put(new Itemset(i + 1), 0.1);
		}
		// itemsets.put(new Itemset(1), 0.1);
		// itemsets.put(new Itemset(3), 0.1);
		// itemsets.put(new Itemset(4), 0.1);
		// itemsets.put(new Itemset(5), 0.1);
		// itemsets.put(new Itemset(6), 0.1);
		System.out.println(" Initial itemsets: " + itemsets);
		double averageCost = 0;

		// Structural EM
		for (int i = 1; i <= iterations; i++) {

			// Learn structure
			System.out.println("\n+++++ Structural Optimization Step " + i);
			learnStructureStep(averageCost, itemsets, transactions, tree);

			// Optimize parameters of new structure
			if (i >= OPTIMIZE_PARAMS_BURNIN && i % OPTIMIZE_PARAMS_EVERY == 0)
				averageCost = expectationMaximizationStep(itemsets,
						transactions);
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
			final HashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions) {

		double averageCost = 0;
		HashMap<Itemset, Double> prevItemsets = itemsets;
		final double n = transactions.size();
		for (int i = 0; i < OPTIMIZE_ITERATIONS; i++) {

			// E step and M step combined
			final HashMap<Itemset, Double> newItemsets = Maps.newHashMap();
			for (final Transaction transaction : transactions) {

				final Set<Itemset> covering = Sets.newHashSet();
				// TODO parallelize inference step
				final double cost = inferGreedy(covering, prevItemsets,
						transaction);
				for (final Itemset set : covering) {

					final Double p = newItemsets.get(set);
					if (p != null) {
						newItemsets.put(set, p + (1. / n));
					} else {
						newItemsets.put(set, 1. / n);
					}
				}

				// Save cost on last iteration
				if (i == OPTIMIZE_ITERATIONS - 1) {
					averageCost += cost / n;
				}

			}
			prevItemsets = newItemsets;
		}
		itemsets.clear();
		itemsets.putAll(prevItemsets);
		System.out.println("\n***** Parameter Optimization Step");
		System.out.println(" Parameter Optimal Itemsets: " + itemsets);
		System.out.println(" Average cost: " + averageCost);
		return averageCost;
	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering.
	 * <p>
	 * This is an O(log(n))-approximation algorithm where n is the number of
	 * elements in the transaction.
	 */
	public static double inferGreedy(final Set<Itemset> covering,
			final HashMap<Itemset, Double> itemsets,
			final Transaction transaction) {

		// TODO priority queue implementation?
		double totalCost = 0;
		final Set<Integer> coveredItems = Sets.newHashSet();
		final List<Integer> transactionItems = transaction.getItems();

		while (!coveredItems.containsAll(transactionItems)) {

			double minCostPerItem = Double.POSITIVE_INFINITY;
			Itemset bestSet = null;
			double bestCost = -1;

			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {

				int notCovered = 0;
				for (final Integer item : entry.getKey().getItems()) {
					if (!coveredItems.contains(item)) {
						notCovered++;
					}
				}

				final double cost = -Math.log(entry.getValue());
				final double costPerItem = cost / notCovered;

				if (costPerItem < minCostPerItem) {
					minCostPerItem = costPerItem;
					bestSet = entry.getKey();
					bestCost = cost;
				}

			}

			// Allow incomplete coverings
			// FIXME check that totalCost is properly calculated now
			if (bestSet != null) {
				covering.add(bestSet);
				coveredItems.addAll(bestSet.getItems());
				totalCost += bestCost;
			} else {
				// System.out.println("Incomplete covering.");
				break;
			}

		}

		return totalCost;
	}

	/**
	 * Infer ML parameters to explain transaction using Primal-Dual
	 * approximation and store in covering.
	 * <p>
	 * This is an O(mn) run-time f-approximation algorithm, where m is the no.
	 * elements to cover, n is the number of sets and f is the frequency of the
	 * most frequent element in the sets.
	 */
	public static double inferPrimalDual(final Set<Itemset> covering,
			final HashMap<Itemset, Double> itemsets,
			final Transaction transaction) {

		double totalCost = 0;
		final List<Integer> notCoveredItems = Lists.newArrayList(transaction
				.getItems());

		// Calculate costs
		final HashMap<Itemset, Double> costs = Maps.newHashMap();
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			costs.put(entry.getKey(), -Math.log(entry.getValue()));
		}

		while (!notCoveredItems.isEmpty()) {

			double minCost = Double.POSITIVE_INFINITY;
			Itemset bestSet = null;

			// Pick random element
			final int index = rand.nextInt(notCoveredItems.size());
			final Integer element = notCoveredItems.get(index);

			// Increase dual of element as much as possible
			for (final Entry<Itemset, Double> entry : costs.entrySet()) {

				if (entry.getKey().getItems().contains(element)) {

					final double cost = entry.getValue();
					if (cost < minCost) {
						minCost = cost;
						bestSet = entry.getKey();
					}

				}
			}

			// Make dual of element binding
			for (final Itemset set : costs.keySet()) {
				if (set.getItems().contains(element)) {
					final double cost = costs.get(set);
					costs.put(set, cost - minCost);
				}
			}

			// Allow incomplete coverings
			if (bestSet != null) {
				covering.add(bestSet);
				notCoveredItems.removeAll(bestSet.getItems());
				totalCost += minCost;
			} else {
				// System.out.println("Incomplete covering.");
				break;
			}

		}

		return totalCost;
	}

	// TODO keep a set of previous suggestions for efficiency?
	public static void learnStructureStep(final double averageCost,
			final HashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions, final ItemsetTree tree) {

		// Try and find better itemset to add
		final double n = transactions.size();
		System.out.print(" Structural candidate itemsets: ");
		for (int i = 0; i < MAX_RANDOM_WALKS; i++) {

			// Candidate itemset
			final Itemset set = tree.randomWalk();
			System.out.print(set + ", ");

			// Skip empty candidates and candidates already present
			if (!set.isEmpty() && !itemsets.keySet().contains(set)) {

				System.out.print("\n potential candidate: " + set);
				// Estimate itemset probability (M-step assuming always
				// included)
				double p = 0;
				for (final Transaction transaction : transactions) {
					if (transaction.getItems().containsAll(set.getItems())) {
						p++;
					}
				}
				p = p / n;

				// Add itemset and find cost
				itemsets.put(set, p);
				double curCost = 0;
				for (final Transaction transaction : transactions) {

					final Set<Itemset> covering = Sets.newHashSet();
					// TODO parallelize inference step
					final double cost = inferGreedy(covering, itemsets,
							transaction);
					curCost += cost;
				}
				curCost = curCost / n;
				System.out.print(", candidate cost: " + curCost);

				if (curCost > averageCost) { // found better set of itemsets
					System.out.print("\n Candidate Accepted.");
					break;
				} // otherwise keep trying
				itemsets.remove(set);
				System.out.print("\n Structural candidate itemsets: ");
			}

		}
		System.out.println("\n Structure Optimal Itemsets: " + itemsets);

	}

	public static List<Transaction> readTransactions(final String input)
			throws IOException {

		final List<Transaction> transactions = Lists.newArrayList();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils
				.lineIterator(new File(input), "UTF-8");
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
}