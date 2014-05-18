package itemsetmining.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ItemsetMining {

	private static final int EM_ITERATIONS = 1000;
	private static final int MAX_STRUCTURE_ITERATIONS = 1000;

	// private final HashMap<Itemset, Double> itemsets = Maps.newHashMap();

	// TODO don't read all transactions into memory
	public static void main(final String[] args) {

	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering
	 */
	public double inferGreedy(final Set<Itemset> covering,
			final HashMap<Itemset, Double> itemsets,
			final Transaction transaction) {

		// TODO priority queue implementation?
		double totalCost = 0;
		final Set<Integer> coveredItems = Sets.newHashSet();
		final List<Integer> transactionItems = transaction.getItems();

		while (!coveredItems.containsAll(transactionItems)) {

			double minCostPerItem = Double.POSITIVE_INFINITY;
			Itemset bestSet = null;

			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {

				final Set<Integer> notCoveredItems = Sets.newHashSet(entry
						.getKey().getItems());
				notCoveredItems.removeAll(coveredItems);

				final double cost = -Math.log(entry.getValue());
				final double costPerItem = cost / notCoveredItems.size();

				if (costPerItem < minCostPerItem) {
					minCostPerItem = costPerItem;
					bestSet = entry.getKey();
					totalCost += cost;
				}

			}
			assert bestSet != null;

			covering.add(bestSet);
			coveredItems.addAll(bestSet.getItems());

		}

		return totalCost;
	}

	/** Learn itemsets model using structural EM */
	public HashMap<Itemset, Double> structuralEM(
			List<Transaction> transactions, int lenUniverse, ItemsetTree tree,
			int iterations) {

		// Intialize with equiprobable singleton sets
		HashMap<Itemset, Double> itemsets = Maps.newHashMap();
		for (int i = 0; i < lenUniverse; i++) {
			itemsets.put(new Itemset(i), 0.5);
		}

		// Structural EM
		for (int i = 0; i < iterations; i++) {

			// Optimize parameters
			double averageCost = expectationMaximizationStep(itemsets,
					transactions);

			// Learn structure
			learnStructureStep(averageCost, itemsets, transactions, tree);

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
	public double expectationMaximizationStep(
			HashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions) {

		double averageCost = 0;
		final double n = transactions.size();
		for (int i = 0; i < EM_ITERATIONS; i++) {

			// E step and M step combined
			final HashMap<Itemset, Double> newItemsets = Maps.newHashMap();
			for (final Transaction transaction : transactions) {

				final Set<Itemset> covering = Sets.newHashSet();
				// TODO parallelize inference step
				final double cost = inferGreedy(covering, itemsets, transaction);
				for (final Itemset set : covering) {

					final Double p = newItemsets.get(set);
					if (p != null) {
						newItemsets.put(set, p + (1. / n));
					} else {
						newItemsets.put(set, 1. / n);
					}
				}

				// Save cost on last iteration
				if (i == EM_ITERATIONS - 1) {
					averageCost += cost / n;
				}

			}
			itemsets = newItemsets;
		}
		return averageCost;
	}

	public void learnStructureStep(final double averageCost,
			final HashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions, final ItemsetTree tree) {

		// Try and find better itemset to add
		final double n = transactions.size();
		for (int i = 0; i < MAX_STRUCTURE_ITERATIONS; i++) {

			// Candidate itemset
			Itemset set = new Itemset();
			tree.randomWalk(set);
			assert !set.getItems().isEmpty();

			// Estimate itemset probability (M-step assuming always included)
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
				final double cost = inferGreedy(covering, itemsets, transaction);
				curCost += cost;
			}
			curCost = curCost / n;

			if (curCost > averageCost) { // found better set of itemsets
				break;
			} // otherwise keep trying
			itemsets.remove(set);

		}

	}

}