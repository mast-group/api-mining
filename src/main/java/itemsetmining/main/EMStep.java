package itemsetmining.main;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionDatabase;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import scala.Tuple2;

import com.google.common.collect.Multiset;

/** Class to hold the various transaction EM Steps */
public class EMStep {

	/** Initialize cached itemsets */
	static void parallelInitializeCachedItemsets(
			TransactionDatabase transactions, final Multiset<Integer> singletons) {
		transactions
				.getTransactionList()
				.parallelStream()
				.forEach(
						t -> t.initializeCachedItemsets(singletons,
								transactions.size()));
	}

	/** EM-step for hard EM */
	static Map<Itemset, Double> parallelEMStep(
			final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm) {

		// E-step
		final Map<Itemset, Long> coveringWithCounts = transactions
				.parallelStream()
				.map(t -> {
					final HashSet<Itemset> covering = inferenceAlgorithm
							.infer(t);
					t.setCachedCovering(covering);
					return covering;
				}).flatMap(HashSet::stream)
				.collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts
				.entrySet()
				.parallelStream()
				.collect(
						Collectors.toMap(Map.Entry::getKey, v -> v.getValue()
								/ (double) transactions.size()));

		// Update cached itemsets
		transactions.parallelStream().forEach(
				t -> t.updateCachedItemsets(newItemsets));

		return newItemsets;
	}

	/** Get average cost of last EM-step */
	static void calculateAndSetAverageCost(TransactionDatabase transactions) {
		final double averageCost = transactions.getTransactionList()
				.parallelStream().map(Transaction::getCachedCost)
				.reduce(0., (sum, c) -> sum += c, (sum1, sum2) -> sum1 + sum2)
				/ (double) transactions.size();
		transactions.setAverageCost(averageCost);
	}

	/** EM-step for structural EM */
	static Tuple2<Double, Double> parallelEMStep(
			TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {

		// E-step (adding candidate to transactions that support it)
		final Map<Itemset, Long> coveringWithCounts = transactions
				.getTransactionList()
				.parallelStream()
				.map(t -> {
					if (t.contains(candidate)) {
						t.addItemsetCache(candidate, 1.0);
						final HashSet<Itemset> covering = inferenceAlgorithm
								.infer(t);
						t.setTempCachedCovering(covering);
						return covering;
					}
					return t.getCachedCovering();
				}).flatMap(HashSet::stream)
				.collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts
				.entrySet()
				.parallelStream()
				.collect(
						Collectors.toMap(Map.Entry::getKey, v -> v.getValue()
								/ (double) transactions.size()));

		// Get average cost (removing candidate from supported transactions)
		final double averageCost = transactions.getTransactionList()
				.parallelStream().map(t -> {
					double cost;
					if (t.contains(candidate))
						cost = t.getTempCachedCost(newItemsets);
					else
						cost = t.getCachedCost(newItemsets);
					t.removeItemsetCache(candidate);
					return cost;
				})
				.reduce(0., (sum, c) -> sum += c, (sum1, sum2) -> sum1 + sum2)
				/ (double) transactions.size();

		// Get candidate prob
		Double prob = newItemsets.get(candidate);
		if (prob == null)
			prob = 0.;

		return new Tuple2<Double, Double>(averageCost, prob);
	}

	/** Add accepted candidate itemset to cache */
	static Map<Itemset, Double> parallelAddAcceptedItemsetCache(
			final TransactionDatabase transactions, final Itemset candidate,
			final double prob) {

		// Cached E-step (adding candidate to transactions that support it)
		final Map<Itemset, Long> coveringWithCounts = transactions
				.getTransactionList().parallelStream().map(t -> {
					if (t.contains(candidate)) {
						t.addItemsetCache(candidate, prob);
						return t.getTempCachedCovering();
					}
					return t.getCachedCovering();
				}).flatMap(HashSet::stream)
				.collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts
				.entrySet()
				.parallelStream()
				.collect(
						Collectors.toMap(Map.Entry::getKey, v -> v.getValue()
								/ (double) transactions.size()));

		// Update cached itemsets
		transactions.getTransactionList().parallelStream()
				.forEach(t -> t.updateCachedItemsets(newItemsets));

		return newItemsets;
	}

	private EMStep() {
	}

}
