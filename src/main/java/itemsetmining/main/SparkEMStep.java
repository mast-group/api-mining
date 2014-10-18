package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionDatabase;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

import scala.Tuple2;

import com.google.common.collect.Multiset;

/** Class to hold the various transaction EM Steps for Spark */
public class SparkEMStep {

	/** Initialize cached itemsets */
	static void parallelInitializeCachedItemsets(
			TransactionDatabase transactions, final Multiset<Integer> singletons) {
		JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						t -> {
							t.initializeCachedItemsets(singletons,
									transactions.size());
							return t;
						});

		// Update cache reference
		transactions.updateTransactionCache(updatedTransactions);
	}

	/** EM-step for hard EM */
	static Map<Itemset, Double> parallelEMStep(
			final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm) {

		// E-step: map and cache covering
		JavaPairRDD<Transaction, Set<Itemset>> transactionWithCovering = transactions
				.getTransactionRDD()
				.mapToPair(
						t -> {
							final HashSet<Itemset> covering = inferenceAlgorithm
									.infer(t);
							t.setCachedCovering(covering);
							return new Tuple2<Transaction, Set<Itemset>>(t,
									covering);
						});

		// E-step: reduce and get itemset counts
		List<Tuple2<Itemset, Integer>> coveringWithCounts = transactionWithCovering
				.values().flatMap(s -> s)
				.mapToPair(s -> new Tuple2<Itemset, Integer>(s, 1))
				.reduceByKey((a, b) -> a + b).collect();

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts
				.parallelStream().collect(
						Collectors.toMap(Tuple2::_1, t -> t._2
								/ (double) transactions.size()));

		// Update cached itemsets
		JavaRDD<Transaction> updatedTransactions = transactionWithCovering
				.keys().map(t -> {
					t.updateCachedItemsets(newItemsets);
					return t;
				});

		// Update cache reference
		transactions.updateTransactionCache(updatedTransactions);

		return newItemsets;
	}

	/** Get average cost of last EM-step */
	static void calculateAndSetAverageCost(
			final TransactionDatabase transactions) {
		final double averageCost = transactions.getTransactionRDD()
				.map(Transaction::getCachedCost).reduce((a, b) -> a + b)
				/ (double) transactions.size();
		transactions.setAverageCost(averageCost);
	}

	/** EM-step for structural EM */
	static double parallelEMStep(final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {

		// E-step: map candidate to supported transactions and cache covering
		JavaPairRDD<Transaction, Set<Itemset>> transactionWithCovering = transactions
				.getTransactionRDD()
				.mapToPair(
						t -> {
							if (t.contains(candidate)) {
								t.addItemsetCache(candidate, 1.0);
								final HashSet<Itemset> covering = inferenceAlgorithm
										.infer(t);
								t.setTempCachedCovering(covering);
								return new Tuple2<Transaction, Set<Itemset>>(t,
										covering);
							}
							return new Tuple2<Transaction, Set<Itemset>>(t, t
									.getCachedCovering());
						});

		// E-step: reduce and get itemset counts
		List<Tuple2<Itemset, Integer>> coveringWithCounts = transactionWithCovering
				.values().flatMap(s -> s)
				.mapToPair(s -> new Tuple2<Itemset, Integer>(s, 1))
				.reduceByKey((a, b) -> a + b).collect();

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts
				.parallelStream().collect(
						Collectors.toMap(Tuple2::_1, t -> t._2
								/ (double) transactions.size()));

		// Get cost per transaction
		JavaPairRDD<Transaction, Double> transactionWithCost = transactionWithCovering
				.keys().mapToPair(t -> {
					final double cost = t.getCachedCost(newItemsets);
					t.removeItemsetCache(candidate);
					return new Tuple2<Transaction, Double>(t, cost);
				});

		// Get average cost
		double averageCost = transactionWithCost.values().reduce(
				(a, b) -> a + b)
				/ (double) transactions.size();
		transactions.setAverageCost(averageCost);

		// Update cache reference
		transactions.updateTransactionCache(transactionWithCost.keys());

		return averageCost;
	}

	/** Add accepted candidate itemset to cache */
	static Map<Itemset, Double> parallelAddAcceptedItemsetCache(
			final TransactionDatabase transactions, final Itemset candidate) {

		// Cached E-step
		List<Tuple2<Itemset, Integer>> coveringWithCounts = transactions
				.getTransactionRDD().map(t -> {
					if (t.contains(candidate))
						return t.getTempCachedCovering();
					return t.getCachedCovering();
				}).flatMap(s -> s)
				.mapToPair(s -> new Tuple2<Itemset, Integer>(s, 1))
				.reduceByKey((a, b) -> a + b).collect();

		// M-step
		Map<Itemset, Double> newItemsets = coveringWithCounts.parallelStream()
				.collect(
						Collectors.toMap(Tuple2::_1, t -> t._2
								/ (double) transactions.size()));

		// Update cached itemsets
		JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(t -> {
					t.updateCachedItemsets(newItemsets);
					return t;
				});

		// Update cache reference
		transactions.updateTransactionCache(updatedTransactions);

		return newItemsets;
	}

	private SparkEMStep() {
	}

}
