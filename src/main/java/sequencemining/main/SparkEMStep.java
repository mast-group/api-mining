package sequencemining.main;
//package itemsetmining.main;
//
//import itemsetmining.itemset.Itemset;
//import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
//import itemsetmining.transaction.Transaction;
//import itemsetmining.transaction.TransactionDatabase;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.apache.spark.api.java.JavaPairRDD;
//import org.apache.spark.api.java.JavaRDD;
//
//import scala.Tuple2;
//
//import com.google.common.collect.Multiset;
//
///** Class to hold the various transaction EM Steps for Spark */
//public class SparkEMStep {
//
//	/** Initialize cached itemsets */
//	static void initializeCachedItemsets(
//			final TransactionDatabase transactions,
//			final Multiset<Integer> singletons) {
//		final long noTransactions = transactions.size();
//		final JavaRDD<Transaction> updatedTransactions = transactions
//				.getTransactionRDD().map(t -> {
//					t.initializeCachedItemsets(singletons, noTransactions);
//					return t;
//				});
//
//		// Update cache reference
//		transactions.updateTransactionCache(updatedTransactions);
//	}
//
//	/** EM-step for hard EM */
//	static Map<Itemset, Double> hardEMStep(
//			final TransactionDatabase transactions,
//			final InferenceAlgorithm inferenceAlgorithm) {
//		final double noTransactions = transactions.size();
//
//		// E-step: map and cache covering
//		final JavaPairRDD<Transaction, Set<Itemset>> transactionWithCovering = transactions
//				.getTransactionRDD()
//				.mapToPair(
//						t -> {
//							final HashSet<Itemset> covering = inferenceAlgorithm
//									.infer(t);
//							t.setCachedCovering(covering);
//							return new Tuple2<Transaction, Set<Itemset>>(t,
//									covering);
//						});
//
//		// E-step: reduce and get itemset counts
//		final List<Tuple2<Itemset, Integer>> coveringWithCounts = transactionWithCovering
//				.values().flatMap(s -> s)
//				.mapToPair(s -> new Tuple2<Itemset, Integer>(s, 1))
//				.reduceByKey((a, b) -> a + b).collect();
//
//		// M-step
//		final Map<Itemset, Double> newItemsets = coveringWithCounts
//				.parallelStream().collect(
//						Collectors
//								.toMap(Tuple2::_1, t -> t._2 / noTransactions));
//
//		// Update cached itemsets
//		final JavaRDD<Transaction> updatedTransactions = transactionWithCovering
//				.keys().map(t -> {
//					t.updateCachedItemsets(newItemsets);
//					return t;
//				});
//
//		// Update cache reference
//		transactions.updateTransactionCache(updatedTransactions);
//
//		return newItemsets;
//	}
//
//	/** Get average cost of last EM-step */
//	static void calculateAndSetAverageCost(
//			final TransactionDatabase transactions) {
//		final double noTransactions = transactions.size();
//		final double averageCost = transactions.getTransactionRDD()
//				.map(Transaction::getCachedCost).reduce((a, b) -> a + b)
//				/ noTransactions;
//		transactions.setAverageCost(averageCost);
//	}
//
//	/** EM-step for structural EM */
//	static Tuple2<Double, Double> structuralEMStep(
//			final TransactionDatabase transactions,
//			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {
//		final double noTransactions = transactions.size();
//
//		// E-step: map candidate to supported transactions and cache covering
//		final JavaPairRDD<Transaction, Set<Itemset>> transactionWithCovering = transactions
//				.getTransactionRDD()
//				.mapToPair(
//						t -> {
//							if (t.contains(candidate)) {
//								t.addItemsetCache(candidate, 1.0);
//								final HashSet<Itemset> covering = inferenceAlgorithm
//										.infer(t);
//								t.setTempCachedCovering(covering);
//								return new Tuple2<Transaction, Set<Itemset>>(t,
//										covering);
//							}
//							return new Tuple2<Transaction, Set<Itemset>>(t, t
//									.getCachedCovering());
//						});
//
//		// E-step: reduce and get itemset counts
//		final List<Tuple2<Itemset, Integer>> coveringWithCounts = transactionWithCovering
//				.values().flatMap(s -> s)
//				.mapToPair(s -> new Tuple2<Itemset, Integer>(s, 1))
//				.reduceByKey((a, b) -> a + b).collect();
//
//		// M-step
//		final Map<Itemset, Double> newItemsets = coveringWithCounts
//				.parallelStream().collect(
//						Collectors
//								.toMap(Tuple2::_1, t -> t._2 / noTransactions));
//
//		// Get cost per transaction
//		final JavaPairRDD<Transaction, Double> transactionWithCost = transactionWithCovering
//				.keys().mapToPair(t -> {
//					double cost;
//					if (t.contains(candidate))
//						cost = t.getTempCachedCost(newItemsets);
//					else
//						cost = t.getCachedCost(newItemsets);
//					t.removeItemsetCache(candidate);
//					return new Tuple2<Transaction, Double>(t, cost);
//				});
//
//		// Get average cost
//		final double averageCost = transactionWithCost.values().reduce(
//				(a, b) -> a + b)
//				/ noTransactions;
//
//		// Get candidate prob
//		Double prob = newItemsets.get(candidate);
//		if (prob == null)
//			prob = 0.;
//
//		// Update cache reference
//		transactions.updateTransactionCache(transactionWithCost.keys());
//
//		return new Tuple2<Double, Double>(averageCost, prob);
//	}
//
//	/** Add accepted candidate itemset to cache */
//	static Map<Itemset, Double> addAcceptedCandidateCache(
//			final TransactionDatabase transactions, final Itemset candidate,
//			final double prob) {
//		final double noTransactions = transactions.size();
//
//		// Cached E-step: map candidate to supported transactions and cache
//		final JavaPairRDD<Transaction, Set<Itemset>> transactionWithCovering = transactions
//				.getTransactionRDD().mapToPair(
//						t -> {
//							if (t.contains(candidate)) {
//								t.addItemsetCache(candidate, prob);
//								final HashSet<Itemset> covering = t
//										.getTempCachedCovering();
//								t.setCachedCovering(covering);
//								return new Tuple2<Transaction, Set<Itemset>>(t,
//										covering);
//							}
//							return new Tuple2<Transaction, Set<Itemset>>(t, t
//									.getCachedCovering());
//						});
//
//		// E-step: reduce and get itemset counts
//		final List<Tuple2<Itemset, Integer>> coveringWithCounts = transactionWithCovering
//				.values().flatMap(s -> s)
//				.mapToPair(s -> new Tuple2<Itemset, Integer>(s, 1))
//				.reduceByKey((a, b) -> a + b).collect();
//
//		// M-step
//		final Map<Itemset, Double> newItemsets = coveringWithCounts
//				.parallelStream().collect(
//						Collectors
//								.toMap(Tuple2::_1, t -> t._2 / noTransactions));
//
//		// Update cached itemsets
//		final JavaRDD<Transaction> updatedTransactions = transactionWithCovering
//				.keys().map(t -> {
//					t.updateCachedItemsets(newItemsets);
//					return t;
//				});
//
//		// Update cache reference
//		transactions.updateTransactionCache(updatedTransactions);
//
//		return newItemsets;
//	}
//
//	private SparkEMStep() {
//	}
//
// }
