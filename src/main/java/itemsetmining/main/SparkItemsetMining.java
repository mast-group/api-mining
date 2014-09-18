package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionRDD;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class SparkItemsetMining extends ItemsetMiningCore {

	private static final boolean USE_KRYO = true;

	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "Dataset filename")
		private final File dataset = new File(
				"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/plants.dat");

		@Parameter(names = { "-s", "--maxSteps" }, description = "Max structure steps")
		int maxStructureSteps = 100_000;

		@Parameter(names = { "-i", "--iterations" }, description = "Max iterations")
		int maxEMIterations = 1_000;

		@Parameter(names = { "-c", "--cores" }, description = "No cores")
		int noCores = 16;

		@Parameter(names = { "-l", "--log-level" }, description = "Log level", converter = LogLevelConverter.class)
		Level logLevel = Level.FINE;

		@Parameter(names = { "-r", "--runtime" }, description = "Max Runtime (min)")
		long maxRunTime = 12 * 60; // 12hrs

		@Parameter(names = { "-t", "--timestamp" }, description = "Timestamp Logfile", arity = 1)
		boolean timestampLog = true;
	}

	public static void main(final String[] args) throws IOException {

		// Use greedy inference algorithm for Spark
		final InferenceAlgorithm inferenceAlg = new InferGreedy();

		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);

		try {
			jc.parse(args);

			// Set up spark and HDFS
			final JavaSparkContext sc = setUpSpark(params.dataset.getName(),
					params.noCores);
			final FileSystem hdfs = setUpHDFS();

			// Set loglevel, runtime and timestamp
			LOG_LEVEL = params.logLevel;
			MAX_RUNTIME = params.maxRunTime * 60 * 1_000;
			TIMESTAMP_LOG = params.timestampLog;

			mineItemsets(params.dataset, hdfs, sc, inferenceAlg,
					params.maxStructureSteps, params.maxEMIterations);

		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}

	}

	public static HashMap<Itemset, Double> mineItemsets(final File inputFile,
			final FileSystem hdfs, final JavaSparkContext sc,
			final InferenceAlgorithm inferenceAlg, final int maxStructureSteps,
			final int maxEMIterations) throws IOException {

		// Set up logging
		setUpFileLogger(inputFile);

		// Copy transaction database to hdfs
		final String datasetPath = "hdfs://cup04.inf.ed.ac.uk:54310/"
				+ inputFile.getName();
		hdfs.copyFromLocalFile(new Path(inputFile.getAbsolutePath()), new Path(
				datasetPath));
		hdfs.setReplication(new Path(datasetPath), (short) 8);
		try { // Wait for file to replicate
			Thread.sleep(10 * 1000);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		// Read in transaction database
		final int noCores = Integer.parseInt(sc.getConf()
				.get("spark.cores.max"));
		final JavaRDD<Transaction> db = sc.textFile(datasetPath, 2 * noCores)
				.map(new ParseTransaction()).cache();

		// Determine most frequent singletons
		final Map<Integer, Integer> singletonsMap = db
				.flatMap(new GetTransactionItems())
				.mapToPair(new PairItemCount())
				.reduceByKey(new SparkEMStep.SumCounts()).collectAsMap();

		// Convert singletons map to Multiset (as Spark map is not serializable)
		final Multiset<Integer> singletons = HashMultiset.create();
		for (final Entry<Integer, Integer> entry : singletonsMap.entrySet())
			singletons.add(entry.getKey(), entry.getValue());

		// Apply the algorithm to build the itemset tree
		final ItemsetTree tree = new ItemsetTree(singletons);
		tree.buildTree(datasetPath, hdfs);
		if (LOG_LEVEL.equals(Level.FINE))
			tree.printStatistics(logger);

		// Run inference to find interesting itemsets
		final TransactionRDD transactions = new TransactionRDD(db, db.count());
		logger.fine("\n============= ITEMSET INFERENCE =============\n");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				singletons, tree, inferenceAlg, maxStructureSteps,
				maxEMIterations);
		logger.info("\n============= INTERESTING ITEMSETS =============\n");
		final HashMap<Itemset, Double> intMap = calculateInterestingness(
				itemsets, transactions);
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			logger.info(String.format("%s\tprob: %1.5f \tint: %1.5f %n",
					entry.getKey(), entry.getValue(),
					intMap.get(entry.getKey())));
		}
		logger.info("\n");

		return itemsets;
	}

	/** Set up Spark */
	public static JavaSparkContext setUpSpark(final String dataset,
			final int noCores) {

		final SparkConf conf = new SparkConf();
		conf.setMaster("spark://cup04.inf.ed.ac.uk:7077")
				.setAppName("Itemset Mining: " + dataset)
				.setSparkHome("/disk/data1/jfowkes/spark-1.1.0-bin-hadoop1")
				.setJars(
						new String[] { "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/miltository/projects/itemset-mining/target/itemset-mining-1.1-SNAPSHOT.jar" });
		conf.set("spark.cores.max", Integer.toString(noCores));
		conf.set("spark.executor.memory", "20g");
		conf.set("spark.default.parallelism", "8");
		conf.set("spark.shuffle.manager", "SORT");
		// conf.set("spark.eventLog.enabled", "true"); uses GB of space!!!

		if (USE_KRYO) {
			conf.set("spark.serializer",
					"org.apache.spark.serializer.KryoSerializer");
			conf.set("spark.kryo.registrator",
					"itemsetmining.util.ClassRegistrator");
		}

		final JavaSparkContext sc = new JavaSparkContext(conf);
		sc.setCheckpointDir("hdfs://cup04.inf.ed.ac.uk:54310/checkpoint/");

		return sc;
	}

	/** Set up HDFS */
	public static FileSystem setUpHDFS() throws IOException {
		// TODO use classloader for conf file?
		final String hdfsConfFile = "/disk/data1/jfowkes/hadoop-1.0.4/conf/core-site.xml";
		final Configuration conf = new Configuration();
		conf.addResource(new Path(hdfsConfFile));
		return FileSystem.get(conf);
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
			final String[] lineSplit = line.split(" ");

			// for each item in the transaction
			for (int i = 0; i < lineSplit.length; i++) {
				// convert the item to integer and add it to the structure
				transaction.add(Integer.parseInt(lineSplit[i]));
			}

			return transaction;
		}
	}

	/** Set up logging to file */
	protected static void setUpFileLogger(final File dataset) {
		LogManager.getLogManager().reset();
		logger.setLevel(LOG_LEVEL);
		String timeStamp = "";
		if (TIMESTAMP_LOG)
			timeStamp = "-"
					+ new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss")
							.format(new Date());
		FileHandler handler = null;
		try { // Limit log file to 1MB
			handler = new FileHandler(LOG_DIR
					+ FilenameUtils.getBaseName(dataset.getName()) + "-"
					+ CANDGEN_NAME + timeStamp + ".log", 1048576, 1);
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

	/** Convert string level to level class */
	public static class LogLevelConverter implements IStringConverter<Level> {
		@Override
		public Level convert(final String value) {
			if (value.equals("SEVERE"))
				return Level.SEVERE;
			else if (value.equals("WARNING"))
				return Level.WARNING;
			else if (value.equals("INFO"))
				return Level.INFO;
			else if (value.equals("CONFIG"))
				return Level.CONFIG;
			else if (value.equals("FINE"))
				return Level.FINE;
			else if (value.equals("FINER"))
				return Level.FINER;
			else if (value.equals("FINEST"))
				return Level.FINEST;
			else
				throw new RuntimeException("Incorrect Log Level.");
		}
	}

}
