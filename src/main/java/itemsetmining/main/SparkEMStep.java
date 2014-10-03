package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/** Class to hold the various parallel transaction EM Steps for Spark */
public class SparkEMStep {

	/** Spark parallel E-step and M-step combined */
	static double parallelEMStep(final JavaRDD<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final HashMap<Itemset, Double> itemsets,
			final double noTransactions,
			final HashMap<Itemset, Double> newItemsets) {

		// Map: Parallel E-step and M-step combined
		final JavaPairRDD<Set<Itemset>, Double> coveringWithCost = transactions
				.mapToPair(new PairFunction<Transaction, Set<Itemset>, Double>() {
					private static final long serialVersionUID = -4944391752990605173L;

					@Override
					public Tuple2<Set<Itemset>, Double> call(
							final Transaction transaction) {
						final Set<Itemset> covering = Sets.newHashSet();
						final double cost = inferenceAlgorithm.infer(covering,
								itemsets, transaction);
						return new Tuple2<Set<Itemset>, Double>(covering, cost);
					}
				});

		// Reduce: get Itemset counts
		final List<Tuple2<Itemset, Integer>> coveringWithCounts = coveringWithCost
				.keys().flatMap(new GetItemSets())
				.mapToPair(new PairItemsetCount()).reduceByKey(new SumCounts())
				.collect();

		// Normalise probabilities
		for (final Tuple2<Itemset, Integer> tuple : coveringWithCounts) {
			newItemsets.put(tuple._1, tuple._2 / noTransactions);
		}

		// Reduce: sum Itemset costs
		return coveringWithCost.values().reduce(
				new SparkItemsetMining.SumCost())
				/ noTransactions;
	}

	/** Spark parallel E-step and M-step combined (without covering, just cost) */
	static double parallelEMStep(final JavaRDD<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final HashMap<Itemset, Double> itemsets,
			final double noTransactions, final Itemset candidate,
			final double prob, Set<Itemset> subsets) {

		// Map: Parallel E-step and M-step combined
		final JavaRDD<Double> coveringWithCost = transactions
				.map(new Function<Transaction, Double>() {
					private static final long serialVersionUID = -4944391752990605173L;

					@Override
					public Double call(final Transaction transaction) {

						if (itemsets == null)
							transaction.addItemsetCache(candidate, prob,
									subsets);

						final Set<Itemset> covering = Sets.newHashSet();
						final double cost = inferenceAlgorithm.infer(covering,
								itemsets, transaction);

						// No need for cache remove as RDD is immutable
						return cost;
					}
				});

		// Reduce: sum Itemset costs
		return coveringWithCost.reduce(new SparkItemsetMining.SumCost())
				/ noTransactions;
	}

	/** Spark parallel candidate probability estimation */
	static double parallelCandidateSupport(
			final JavaRDD<Transaction> transactions, final Itemset candidate,
			final double noTransactions) {

		final double p = transactions.map(new Function<Transaction, Integer>() {
			private static final long serialVersionUID = -2369728585019081981L;

			@Override
			public Integer call(final Transaction transaction) throws Exception {
				if (transaction.contains(candidate)) {
					return 1;
				}
				return 0;
			}

		}).reduce(new SumCounts());
		return p / noTransactions;

	}

	/** Spark parallel calculate support for each itemset */
	static Multiset<Itemset> parallelSupportCount(
			final JavaRDD<Transaction> transactions,
			final HashMap<Itemset, Double> itemsets) {

		return transactions.map(new Function<Transaction, Multiset<Itemset>>() {
			private static final long serialVersionUID = -4859646175437626337L;

			@Override
			public Multiset<Itemset> call(final Transaction transaction)
					throws Exception {

				final Multiset<Itemset> support = HashMultiset.create();
				for (final Itemset set : itemsets.keySet()) {
					if (transaction.contains(set))
						support.add(set);
				}
				return support;
			}

		}).reduce(new CombineMultisets());
	}

	/** Pair itemsets with counts */
	private static class PairItemsetCount implements
			PairFunction<Itemset, Itemset, Integer> {
		private static final long serialVersionUID = 2455054429227183005L;

		@Override
		public Tuple2<Itemset, Integer> call(final Itemset set) {
			return new Tuple2<Itemset, Integer>(set, 1);
		}
	}

	/** Get itemsets from a set of itemsets */
	private static class GetItemSets implements
			FlatMapFunction<Set<Itemset>, Itemset> {
		private static final long serialVersionUID = -1372354921360086260L;

		@Override
		public Iterable<Itemset> call(final Set<Itemset> itemset)
				throws Exception {
			return itemset;
		}
	}

	/** Add together counts */
	static class SumCounts implements Function2<Integer, Integer, Integer> {
		private static final long serialVersionUID = 2511101612333272343L;

		@Override
		public Integer call(final Integer a, final Integer b) {
			return a + b;
		}
	}

	/** Combine two multisets into one */
	static class CombineMultisets implements
			Function2<Multiset<Itemset>, Multiset<Itemset>, Multiset<Itemset>> {
		private static final long serialVersionUID = 6780310628444189003L;

		@Override
		public Multiset<Itemset> call(final Multiset<Itemset> multiset1,
				final Multiset<Itemset> multiset2) throws Exception {
			multiset1.addAll(multiset2);
			return multiset1;
		}
	}

	private SparkEMStep() {
	}

}
