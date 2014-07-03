package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionRDD;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class SparkItemsetMining extends ItemsetMining {

	private static final String LOG_FILE = "%t/spark_mining.log";

	public static void main(final String[] args) throws IOException {

		// Main function parameters
		final String dataset = "itemset.txt";
		final String path = "hdfs://cup04.inf.ed.ac.uk:54310/" + dataset;
		// TODO use classloader for this?
		final String hdfsConfFile = "/disk/data1/jfowkes/hadoop-1.0.4/conf/core-site.xml";
		final InferenceAlgorithm inferenceAlg = new InferGreedy();

		// Max iterations
		final int maxStructureSteps = 1000;
		final int maxEMIterations = 10;

		// Set up Spark
		final SparkConf conf = new SparkConf();
		conf.setMaster("spark://cup04.inf.ed.ac.uk:7077")
				.setAppName("Itemset Mining: " + dataset)
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

		mineItemsets(sc, path, hdfsConfFile, inferenceAlg, maxStructureSteps,
				maxEMIterations);

	}

	public static HashMap<Itemset, Double> mineItemsets(
			final JavaSparkContext sc, final String dataset,
			final String hdfsConfFile, final InferenceAlgorithm inferenceAlg,
			final int maxStructureSteps, final int maxEMIterations)
			throws UnsupportedEncodingException, IOException {

		// Set up logging
		setUpConsoleLogger();

		// Read in transaction database
		final JavaRDD<Transaction> db = sc.textFile(dataset, 96)
				.map(new ParseTransaction()).cache();

		// Determine most frequent singletons
		final Map<Integer, Integer> singletons = db
				.flatMap(new GetTransactionItems())
				.mapToPair(new PairItemCount())
				.reduceByKey(new SparkEMStep.SumCounts()).collectAsMap();

		// Apply the algorithm to build the itemset tree
		final ItemsetTree tree = new ItemsetTree();
		tree.buildTree(dataset, hdfsConfFile, singletons);
		// if (LOGLEVEL.equals(Level.FINE))
		tree.printStatistics(logger);

		// Run inference to find interesting itemsets
		final TransactionRDD transactions = new TransactionRDD(db, db.count());
		logger.fine("\n============= ITEMSET INFERENCE =============\n");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				singletons.keySet(), tree, inferenceAlg, maxStructureSteps,
				maxEMIterations);
		logger.info("\n============= INTERESTING ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			logger.info(String.format("%s\tprob: %1.5f %n", entry.getKey(),
					entry.getValue()));
		}
		logger.info("\n");

		return itemsets;
	}

	/** Add together itemset costs */
	static class SumCost implements Function2<Double, Double, Double> {
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
			return transaction;
		}
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

	/** Set up logging to file */
	protected static void setUpFileLogger() {
		LogManager.getLogManager().reset();
		logger.setLevel(LOGLEVEL);
		FileHandler handler = null;
		try {
			handler = new FileHandler(LOG_FILE);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		handler.setLevel(Level.ALL);
		final Formatter formatter = new Formatter() {
			@Override
			public String format(final LogRecord record) {
				return record.getMessage();
			}
		};
		handler.setFormatter(formatter);
		logger.addHandler(handler);
	}

}
