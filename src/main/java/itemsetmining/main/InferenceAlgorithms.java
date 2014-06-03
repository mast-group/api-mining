package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/** Container class for Inference Algorithms */
public class InferenceAlgorithms {

	/** Interface for the different inference algorithms */
	public interface InferenceAlgorithm {

		public double infer(final Set<Itemset> covering,
				final LinkedHashMap<Itemset, Double> parItemsets,
				final Transaction transaction);

	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering.
	 * <p>
	 * This is an O(log(n))-approximation algorithm where n is the number of
	 * elements in the transaction.
	 */
	public static class inferGreedy implements InferenceAlgorithm {

		@Override
		public double infer(final Set<Itemset> covering,
				final LinkedHashMap<Itemset, Double> itemsets,
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

					if (costPerItem < minCostPerItem
							&& transactionItems.containsAll(entry.getKey()
									.getItems())) { // Don't over-cover
						minCostPerItem = costPerItem;
						bestSet = entry.getKey();
						bestCost = cost;
					}

				}

				if (bestSet != null) {
					covering.add(bestSet);
					coveredItems.addAll(bestSet.getItems());
					totalCost += bestCost;
				} else { // Allow incomplete coverings
					if (totalCost == 0) // no covering is bad
						totalCost = Double.POSITIVE_INFINITY;
					break;
				}

			}

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
	public static class inferPrimalDual implements InferenceAlgorithm {

		@Override
		public double infer(final Set<Itemset> covering,
				final LinkedHashMap<Itemset, Double> itemsets,
				final Transaction transaction) {

			double totalCost = 0;
			final Random rand = new Random();
			final List<Integer> notCoveredItems = Lists
					.newArrayList(transaction.getItems());

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
						if (cost < minCost
								&& transaction.getItems().containsAll(
										entry.getKey().getItems())) { // don't
																		// over-cover
							minCost = cost;
							bestSet = entry.getKey();
						}

					}
				}

				if (bestSet != null) {
					covering.add(bestSet);
					notCoveredItems.removeAll(bestSet.getItems());
					totalCost += minCost;
				} else { // Allow incomplete coverings
					if (totalCost == 0) // no covering is bad
						totalCost = Double.POSITIVE_INFINITY;
					break;
				}

				// Make dual of element binding
				for (final Itemset set : costs.keySet()) {
					if (set.getItems().contains(element)) {
						final double cost = costs.get(set);
						costs.put(set, cost - minCost);
					}
				}

			}

			return totalCost;
		}

	}

	/**
	 * Infer ML parameters to explain transaction exactly using ILP and store in
	 * covering.
	 * <p>
	 * This is an NP-hard problem.
	 */
	public static class inferILP implements InferenceAlgorithm {

		@Override
		public double infer(final Set<Itemset> covering,
				final LinkedHashMap<Itemset, Double> itemsets,
				final Transaction transaction) {

			// Load solver if necessary
			final LinearProgramSolver solver = SolverFactory.getSolver("CPLEX");
			solver.printToScreen(false);

			// Filter out sets containing items not in transaction
			final LinkedHashMap<Itemset, Double> filteredItemsets = Maps
					.newLinkedHashMap();
			for (final Map.Entry<Itemset, Double> entry : itemsets.entrySet()) {
				if (transaction.getItems().containsAll(
						entry.getKey().getItems()))
					filteredItemsets.put(entry.getKey(), entry.getValue());
			}

			final int probSize = filteredItemsets.size();

			// Set up cost vector
			int i = 0;
			final double[] costs = new double[probSize];
			for (final double p : filteredItemsets.values()) {
				costs[i] = -Math.log(p);
				i++;
			}

			// Set objective sum(c_s*z_s)
			final LinearProgram lp = new LinearProgram(costs);

			// Add covering constraint
			for (final Integer item : transaction.getItems()) {

				i = 0;
				final double[] cover = new double[probSize];
				for (final Itemset set : filteredItemsets.keySet()) {

					// at least one set covers item
					if (set.getItems().contains(item)) {
						cover[i] = 1;
					}
					i++;
				}
				lp.addConstraint(new LinearBiggerThanEqualsConstraint(cover,
						1., "cover"));

			}

			// Set all variables to binary
			for (int j = 0; j < probSize; j++) {
				lp.setBinary(j);
			}

			// System.out.println(lp.convertToCPLEX());

			// Solve
			lp.setMinProblem(true);
			final double[] sol = solver.solve(lp);

			// Add chosen sets to covering
			i = 0;
			double totalCost = 0;
			for (final Itemset set : filteredItemsets.keySet()) {
				if (doubleToBoolean(sol[i])) {
					covering.add(set);
					totalCost += costs[i] * sol[i];
				}
				i++;
			}

			// no covering is bad
			if (totalCost == 0)
				totalCost = Double.POSITIVE_INFINITY;

			return totalCost;
		}

		/** Round double to boolean */
		private static boolean doubleToBoolean(final double d) {
			if ((int) Math.round(d) == 1)
				return true;
			return false;
		}

	}

	private InferenceAlgorithms() {

	}

}
