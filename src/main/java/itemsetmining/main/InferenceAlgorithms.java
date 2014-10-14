package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/** Container class for Inference Algorithms */
public class InferenceAlgorithms {

	/** Interface for the different inference algorithms */
	public interface InferenceAlgorithm {

		public double infer(final Set<Itemset> covering,
				final HashMap<Itemset, Double> parItemsets,
				final Transaction transaction);

	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering.
	 * <p>
	 * This is an O(log(n))-approximation algorithm where n is the number of
	 * elements in the transaction.
	 */
	public static class InferGreedy implements InferenceAlgorithm, Serializable {
		private static final long serialVersionUID = 9173178089235828142L;

		@Override
		public double infer(final Set<Itemset> covering,
				final HashMap<Itemset, Double> itemsets,
				final Transaction transaction) {

			double totalCost = 0;
			final int transactionSize = transaction.size();
			final Transaction coveredItems = new Transaction();

			final HashMap<Itemset, Double> filteredItemsets;
			if (itemsets == null) { // Preferably use itemset cache
				filteredItemsets = transaction.getCachedItemsets();
			} else { // Else filter out sets containing items not in transaction
				filteredItemsets = Maps.newHashMap();
				for (final Map.Entry<Itemset, Double> entry : itemsets
						.entrySet()) {
					if (transaction.contains(entry.getKey()))
						filteredItemsets.put(entry.getKey(), entry.getValue());
				}
			}

			while (coveredItems.size() != transactionSize) {

				double minCostPerItem = Double.POSITIVE_INFINITY;
				Itemset bestSet = null;
				double bestCost = -1;

				for (final Entry<Itemset, Double> entry : filteredItemsets
						.entrySet()) {

					final int notCovered = coveredItems.countUnion(entry
							.getKey()) - coveredItems.size();

					final double cost = -Math.log(entry.getValue());
					final double costPerItem = cost / notCovered;

					if (costPerItem < minCostPerItem) {
						minCostPerItem = costPerItem;
						bestSet = entry.getKey();
						bestCost = cost;
					}

				}

				if (bestSet != null) {
					covering.add(bestSet);
					coveredItems.add(bestSet);
					totalCost += bestCost;
				} else { // Allow incomplete coverings
					break;
				}

			}

			// Add on cost of uncovered itemsets
			for (final Itemset set : Sets.difference(filteredItemsets.keySet(),
					covering)) {
				totalCost += -Math.log(1 - filteredItemsets.get(set));
			}

			// Cache cost
			if (itemsets == null)
				transaction.setCost(totalCost);

			return totalCost;
		}

	}

	/**
	 * Infer ML parameters to explain transaction using Primal-Dual
	 * approximation and store in covering.
	 * <p>
	 * This is an O(mn) run-time f-approximation algorithm, where m is the no.
	 * elements to cover, n is the number of sets and f is the frequency of the
	 * most frequent element in the sets.
	 */
	public static class InferPrimalDual implements InferenceAlgorithm {

		@Override
		public double infer(final Set<Itemset> covering,
				final HashMap<Itemset, Double> itemsets,
				final Transaction transaction) {

			final HashMap<Itemset, Double> filteredItemsets;
			if (itemsets == null) { // Preferably use itemset cache
				filteredItemsets = transaction.getCachedItemsets();
			} else { // Else filter out sets containing items not in transaction
				filteredItemsets = Maps.newHashMap();
				for (final Map.Entry<Itemset, Double> entry : itemsets
						.entrySet()) {
					if (transaction.contains(entry.getKey()))
						filteredItemsets.put(entry.getKey(), entry.getValue());
				}
			}

			double totalCost = 0;
			final Random rand = new Random();
			final List<Integer> notCoveredItems = Lists
					.newArrayList(transaction);

			// Calculate costs
			final HashMap<Itemset, Double> costs = Maps.newHashMap();
			for (final Entry<Itemset, Double> entry : filteredItemsets
					.entrySet()) {
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

					if (entry.getKey().contains(element)) {

						final double cost = entry.getValue();
						if (cost < minCost) {
							minCost = cost;
							bestSet = entry.getKey();
						}

					}
				}

				if (bestSet != null) {
					covering.add(bestSet);
					notCoveredItems.removeAll(bestSet);
					totalCost += minCost;
				} else { // Allow incomplete coverings
					break;
				}

				// Make dual of element binding
				for (final Entry<Itemset, Double> entry : costs.entrySet()) {
					final Itemset set = entry.getKey();
					if (set.contains(element)) {
						final double cost = entry.getValue();
						costs.put(set, cost - minCost);
					}
				}

			}

			// Add on cost of uncovered itemsets
			for (final Itemset set : Sets.difference(filteredItemsets.keySet(),
					covering)) {
				totalCost += -Math.log(1 - filteredItemsets.get(set));
			}

			// Cache cost
			if (itemsets == null)
				transaction.setCost(totalCost);

			return totalCost;
		}

	}

	/**
	 * Infer ML parameters to explain transaction exactly using ILP and store in
	 * covering.
	 * <p>
	 * This is an NP-hard problem.
	 */
	public static class InferILP implements InferenceAlgorithm {

		@Override
		public double infer(final Set<Itemset> covering,
				final HashMap<Itemset, Double> itemsets,
				final Transaction transaction) {

			// // Cache not implemented for ILP yet (requires LinkedHashMap)
			// if (itemsets == null)
			// throw new UnsupportedOperationException(
			// "Cache not implemented for ILP yet!");
			//
			// // Load solver if necessary
			// final LinearProgramSolver solver =
			// SolverFactory.getSolver("CPLEX");
			// solver.printToScreen(false);
			//
			// // Filter out sets containing items not in transaction
			// final LinkedHashMap<Itemset, Double> filteredItemsets = Maps
			// .newLinkedHashMap();
			// for (final Map.Entry<Itemset, Double> entry :
			// itemsets.entrySet()) {
			// if (transaction.contains(entry.getKey()))
			// filteredItemsets.put(entry.getKey(), entry.getValue());
			// }
			//
			// final int probSize = filteredItemsets.size();
			//
			// // Set up cost vector
			// int i = 0;
			// final double[] costs = new double[probSize];
			// for (final double p : filteredItemsets.values()) {
			// costs[i] = -Math.log(p / (1 - p));
			// i++;
			// }
			//
			// // Set objective sum(c_s*z_s)
			// final LinearProgram lp = new LinearProgram(costs);
			//
			// // Add covering constraint
			// for (final Integer item : transaction) {
			//
			// i = 0;
			// final double[] cover = new double[probSize];
			// for (final Itemset set : filteredItemsets.keySet()) {
			//
			// // at least one set covers item
			// if (set.contains(item)) {
			// cover[i] = 1;
			// }
			// i++;
			// }
			// lp.addConstraint(new LinearBiggerThanEqualsConstraint(cover,
			// 1., "cover"));
			//
			// }
			//
			// // Set all variables to binary
			// for (int j = 0; j < probSize; j++) {
			// lp.setBinary(j);
			// }
			//
			// // Solve
			// lp.setMinProblem(true);
			// final double[] sol = solver.solve(lp);
			//
			// // No solution is bad
			// if (sol == null)
			// return Double.POSITIVE_INFINITY;
			//
			// // Add chosen sets to covering
			// i = 0;
			// double totalCost = 0;
			// for (final Entry<Itemset, Double> entry : filteredItemsets
			// .entrySet()) {
			// if (doubleToBoolean(sol[i])) {
			// final Itemset set = entry.getKey();
			// covering.add(set);
			// totalCost += costs[i] * sol[i];
			// }
			// totalCost += -Math.log(1 - entry.getValue());
			// i++;
			// }
			//
			// return totalCost;

			throw new UnsupportedOperationException("Commented out for Spark.");
		}

		/** Round double to boolean */
		@SuppressWarnings("unused")
		private static boolean doubleToBoolean(final double d) {
			if ((int) Math.round(d) == 1)
				return true;
			return false;
		}

	}

	private InferenceAlgorithms() {

	}

}
