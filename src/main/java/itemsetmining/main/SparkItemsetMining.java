package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.transaction.Transaction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class SparkItemsetMining {

	private static final String DATASET = "caviar_big.txt";
	private static final double SINGLETON_PRIOR_PROB = 0.5;

	private static final int MAX_RANDOM_WALKS = 1000;
	private static final int MAX_STRUCTURE_ITERATIONS = 1000;

	private static final int OPTIMIZE_PARAMS_EVERY = 50;
	private static final double OPTIMIZE_TOL = 1e-10;

	public static void main(final String[] args) throws IOException {

		// Set up Spark
		final SparkConf conf = new SparkConf();
		conf.setMaster("spark://cup04.inf.ed.ac.uk:7077")
				.setAppName("itemsets")
				.setSparkHome("/disk/data1/jfowkes/spark-1.0.0-bin-hadoop1")
				.setJars(
						new String[] { "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/miltository/projects/itemset-mining/target/itemset-mining-1.1-SNAPSHOT.jar" });
		conf.set("spark.executor.memory", "10g");
		conf.set("spark.default.parallelism", "8");
		// TODO fix Kryo issue
		// conf.set("spark.serializer",
		// "org.apache.spark.serializer.KryoSerializer");
		// conf.set("spark.kryo.registrator",
		// "itemsetmining.util.ClassRegistrator");
		final JavaSparkContext sc = new JavaSparkContext(conf);

		// Read in transaction database
		final JavaRDD<Transaction> transactions = sc
				.textFile("hdfs://cup04.inf.ed.ac.uk:54310/" + DATASET, 96)
				.map(new ParseTransaction()).cache();
		final double noTransactions = transactions.count();

		// Determine most frequent singletons
		final Map<Integer, Integer> singletons = transactions
				.flatMap(new GetTransactionItems())
				.mapToPair(new PairItemCount()).reduceByKey(new SumCounts())
				.collectAsMap();
		// TODO don't do this stupid conversion
		final Multiset<Integer> support = HashMultiset.create();
		for (final Entry<Integer, Integer> entry : singletons.entrySet()) {
			support.add(entry.getKey(), entry.getValue());
		}
		singletons.clear();

		// Apply the algorithm to build the itemset tree // TODO sparkify
		final URL url = ItemsetMining.class.getClassLoader().getResource(
				DATASET);
		final File input = new File(java.net.URLDecoder.decode(url.getPath(),
				"UTF-8"));
		final ItemsetTree tree = new ItemsetTree();
		tree.buildTree(input, support);
		tree.printStatistics();
		// System.out.println("THIS IS THE TREE:");
		// tree.printTree();

		// Run inference to find interesting itemsets
		System.out.println("============= ITEMSET INFERENCE =============");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				noTransactions, support.elementSet(), tree);
		System.out
				.println("\n============= INTERESTING ITEMSETS =============");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			System.out.printf("%s\tprob: %1.5f %n", entry.getKey(),
					entry.getValue());
		}

	}

	/** Learn itemsets model using structural EM */
	public static HashMap<Itemset, Double> structuralEM(
			final JavaRDD<Transaction> transactions,
			final double noTransactions, final Set<Integer> singletons,
			final ItemsetTree tree) {

		// Intialize with equiprobable singleton sets
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();
		for (final int singleton : singletons) {
			itemsets.put(new Itemset(singleton), SINGLETON_PRIOR_PROB);
		}
		System.out.println(" Initial itemsets: " + itemsets);
		double averageCost = Double.POSITIVE_INFINITY;

		// Initial parameter optimization step
		averageCost = expectationMaximizationStep(itemsets, transactions,
				noTransactions);

		// Structural EM
		for (int iteration = 1; iteration <= MAX_STRUCTURE_ITERATIONS; iteration++) {

			// Learn structure
			System.out.println("\n+++++ Structural Optimization Step "
					+ iteration);
			averageCost = learnStructureStep(averageCost, itemsets,
					transactions, noTransactions, tree);
			System.out.printf(" Average cost: %.2f\n", averageCost);

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0)
				averageCost = expectationMaximizationStep(itemsets,
						transactions, noTransactions);

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
			final JavaRDD<Transaction> transactions, final double noTransactions) {

		System.out.println("\n***** Parameter Optimization Step");
		System.out.println(" Structure Optimal Itemsets: " + itemsets);

		double averageCost = 0;
		HashMap<Itemset, Double> prevItemsets = itemsets;

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Set up storage
			final HashMap<Itemset, Double> newItemsets = Maps.newHashMap();

			// Map: Parallel E-step and M-step combined
			final HashMap<Itemset, Double> parItemsets = prevItemsets;
			final JavaPairRDD<Itemset, Double> coveringWithCost = transactions
					.flatMapToPair(new PairFlatMapFunction<Transaction, Itemset, Double>() {
						private static final long serialVersionUID = -4944391752990605173L;

						@Override
						public Set<Tuple2<Itemset, Double>> call(
								final Transaction transaction) {
							return inferGreedy(transaction, parItemsets);
						}
					});

			// Reduce: get Itemset counts
			final List<Tuple2<Itemset, Integer>> coveringWithCounts = coveringWithCost
					.keys().mapToPair(new PairItemsetCount())
					.reduceByKey(new SumCounts()).collect();

			// Reduce: sum Itemset costs
			averageCost = coveringWithCost.values().reduce(new SumCost())
					/ noTransactions;

			// Normalise probabilities
			for (final Tuple2<Itemset, Integer> tuple : coveringWithCounts) {
				newItemsets.put(tuple._1, tuple._2 / noTransactions);
			}

			// If set has stabilised calculate norm(p_prev - p_new)
			if (prevItemsets.size() == newItemsets.size()) {
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
		System.out.println(" Parameter Optimal Itemsets: " + itemsets);
		System.out.printf(" Average cost: %.2f\n", averageCost);
		return averageCost;
	}

	public static double learnStructureStep(final double averageCost,
			final HashMap<Itemset, Double> itemsets,
			final JavaRDD<Transaction> transactions,
			final double noTransactions, final ItemsetTree tree) {

		// Try and find better itemset to add
		System.out.print(" Structural candidate itemsets: ");

		int iteration;
		for (iteration = 0; iteration < MAX_RANDOM_WALKS; iteration++) {

			// Candidate itemset
			final Itemset set = tree.randomWalk();
			System.out.print(set + ", ");

			// Skip empty candidates and candidates already present
			if (!set.isEmpty() && !itemsets.keySet().contains(set)) {

				System.out.print("\n potential candidate: " + set);

				// Estimate itemset probability (M-step assuming always
				// included)
				double p = transactions.map(
						new Function<Transaction, Integer>() {
							private static final long serialVersionUID = 1L;

							@Override
							public Integer call(final Transaction transaction)
									throws Exception {
								if (transaction.getItems().containsAll(
										set.getItems())) {
									return 1;
								}
								return 0;
							}

						}).reduce(new SumCounts());
				p = p / noTransactions;

				// Add itemset
				itemsets.put(set, p);

				// Map: Parallel E-step and M-step combined
				final JavaPairRDD<Itemset, Double> coveringWithCost = transactions
						.flatMapToPair(new PairFlatMapFunction<Transaction, Itemset, Double>() {
							private static final long serialVersionUID = -4944391752990605173L;

							@Override
							public Set<Tuple2<Itemset, Double>> call(
									final Transaction transaction) {
								return inferGreedy(transaction, itemsets);
							}
						});

				// Reduce: sum Itemset costs
				final double curCost = coveringWithCost.values().reduce(
						new SumCost())
						/ noTransactions;
				System.out.printf(", cost: %.2f", curCost);

				if (curCost < averageCost) { // found better set of itemsets
					System.out.print("\n Candidate Accepted.\n");
					return curCost;
				} // otherwise keep trying
				itemsets.remove(set);
				System.out.print("\n Structural candidate itemsets: ");
			}

		}
		System.out.println();

		return averageCost;
	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm.
	 * <p>
	 * This is an O(log(n))-approximation algorithm where n is the number of
	 * elements in the transaction.
	 * 
	 * @return
	 */
	private static Set<Tuple2<Itemset, Double>> inferGreedy(
			final Transaction transaction,
			final HashMap<Itemset, Double> itemsets) {

		// Covering with cost
		final Set<Tuple2<Itemset, Double>> covering = Sets.newHashSet();

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
				covering.add(new Tuple2<Itemset, Double>(bestSet, bestCost));
				coveredItems.addAll(bestSet.getItems());
			} else { // Allow incomplete coverings
				// TODO no covering is bad?
				break;
			}

		}

		return covering;
	}

	/** Read in transactions */
	private static class ParseTransaction implements
			Function<String, Transaction> {
		private static final long serialVersionUID = -9092218383491621520L;

		@Override
		public Transaction call(final String line) {

			// create a structure for storing the transaction
			final Transaction transaction = new Transaction();

			// split the transaction into items
			final String[] lineSplited = line.split(" ");

			// for each item in the transaction
			for (int i = 0; i < lineSplited.length; i++) {
				// convert the item to integer and add it to the structure
				transaction.add(Integer.parseInt(lineSplited[i]));
			}

			return transaction;
		}
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

	/** Add together counts */
	private static class SumCounts implements
			Function2<Integer, Integer, Integer> {
		private static final long serialVersionUID = 2511101612333272343L;

		@Override
		public Integer call(final Integer a, final Integer b) {
			return a + b;
		}
	}

	/** Add together itemset costs */
	private static class SumCost implements Function2<Double, Double, Double> {
		private static final long serialVersionUID = -6157566765215482009L;

		@Override
		public Double call(final Double a, final Double b) {
			return a + b;
		}
	}

	private static class PairItemCount implements
			PairFunction<Integer, Integer, Integer> {
		private static final long serialVersionUID = 8400960661406105632L;

		@Override
		public Tuple2<Integer, Integer> call(final Integer item) {
			return new Tuple2<Integer, Integer>(item, 1);
		}
	}

	private static class GetTransactionItems implements
			FlatMapFunction<Transaction, Integer> {
		private static final long serialVersionUID = -7433022039627649227L;

		@Override
		public Iterable<Integer> call(final Transaction transaction)
				throws Exception {
			return transaction.getItems();
		}
	}

}
