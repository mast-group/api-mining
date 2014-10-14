package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;
import itemsetmining.util.FutureThreadPool;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/** Class to hold the various serial/parallel transaction EM Steps */
public class EMStep {

	/** Serial E-step and M-step combined */
	static double serialEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final HashMap<Itemset, Double> itemsets,
			final double noTransactions,
			final HashMap<Itemset, Double> newItemsets) {

		final Multiset<Itemset> allCoverings = HashMultiset.create();

		double averageCost = 0;
		for (final Transaction transaction : transactions) {

			final Set<Itemset> covering = Sets.newHashSet();
			final double cost = inferenceAlgorithm.infer(covering, itemsets,
					transaction);
			averageCost += cost;
			allCoverings.addAll(covering);

		}
		averageCost = averageCost / noTransactions;

		// Normalise probabilities
		for (final Itemset set : allCoverings.elementSet()) {
			newItemsets.put(set, allCoverings.count(set) / noTransactions);
		}

		return averageCost;
	}

	/** Serial E-step and M-step combined (without covering, just cost) */
	static double serialEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final HashMap<Itemset, Double> itemsets,
			final double noTransactions, final Itemset candidate,
			final double prob, final Set<Itemset> subsets) {

		double averageCost = 0;
		for (final Transaction transaction : transactions) {

			double cost;
			final Set<Itemset> covering = Sets.newHashSet();
			if (itemsets == null) { // use cache
				final boolean hasChanged = transaction.addItemsetCache(
						candidate, prob, subsets);
				if (hasChanged) {
					cost = inferenceAlgorithm.infer(covering, itemsets,
							transaction);
					transaction.removeItemsetCache(candidate, prob, subsets);
				} else
					cost = transaction.getCost(); // use cached cost
			} else {
				cost = inferenceAlgorithm
						.infer(covering, itemsets, transaction);
			}
			averageCost += cost;

		}
		averageCost = averageCost / noTransactions;
		return averageCost;
	}

	/** Parallel E-step and M-step combined */
	static double parallelEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final HashMap<Itemset, Double> itemsets,
			final double noTransactions,
			final HashMap<Itemset, Double> newItemsets) {

		final Multiset<Itemset> allCoverings = ConcurrentHashMultiset.create();

		// Parallel E-step and M-step combined
		final FutureThreadPool<Double> ftp = new FutureThreadPool<Double>();
		for (final Transaction transaction : transactions) {

			ftp.pushTask(new Callable<Double>() {
				@Override
				public Double call() {
					final Set<Itemset> covering = Sets.newHashSet();
					final double cost = inferenceAlgorithm.infer(covering,
							itemsets, transaction);
					allCoverings.addAll(covering);
					return cost;
				}
			});
		}
		// Wait for tasks to finish
		final List<Double> costs = ftp.getCompletedTasks();

		// Normalise probabilities
		for (final Itemset set : allCoverings.elementSet()) {
			newItemsets.put(set, allCoverings.count(set) / noTransactions);
		}

		return sum(costs) / noTransactions;
	}

	/** Parallel E-step and M-step combined (without covering, just cost) */
	static double parallelEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final HashMap<Itemset, Double> itemsets,
			final double noTransactions, final Itemset candidate,
			final double prob, final Set<Itemset> subsets) {

		// Parallel E-step and M-step combined
		final FutureThreadPool<Double> ftp = new FutureThreadPool<Double>();
		for (final Transaction transaction : transactions) {

			ftp.pushTask(new Callable<Double>() {
				@Override
				public Double call() {

					double cost;
					final Set<Itemset> covering = Sets.newHashSet();
					if (itemsets == null) { // use cache
						final boolean hasChanged = transaction.addItemsetCache(
								candidate, prob, subsets);
						if (hasChanged) {
							cost = inferenceAlgorithm.infer(covering, itemsets,
									transaction);
							transaction.removeItemsetCache(candidate, prob,
									subsets);
						} else
							cost = transaction.getCost(); // use cached cost
					} else {
						cost = inferenceAlgorithm.infer(covering, itemsets,
								transaction);
					}
					return cost;

				}
			});
		}
		return sum(ftp.getCompletedTasks()) / noTransactions;
	}

	/** Calculates the sum of a Collection */
	static double sum(final Iterable<Double> values) {
		double sum = 0;
		for (final Double element : values) {
			sum += element;
		}
		return sum;
	}

	private EMStep() {
	}

}
