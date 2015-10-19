package sequencemining.main;

// import itemsetmining.itemset.Itemset;
// import itemsetmining.itemset.ItemsetTree;
// import itemsetmining.main.InferenceAlgorithms.InferGreedy;
// import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
// import itemsetmining.transaction.Transaction;
// import itemsetmining.transaction.TransactionRDD;
// import itemsetmining.util.Logging;
//
// import java.io.File;
// import java.io.FileInputStream;
// import java.io.IOException;
// import java.text.SimpleDateFormat;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Map.Entry;
// import java.util.logging.Level;
// import java.util.Properties;
//
// import org.apache.hadoop.conf.Configuration;
// import org.apache.hadoop.fs.FileSystem;
// import org.apache.hadoop.fs.Path;
// import org.apache.spark.SparkConf;
// import org.apache.spark.api.java.JavaRDD;
// import org.apache.spark.api.java.JavaSparkContext;
// import org.apache.spark.api.java.function.Function;
//
// import scala.Tuple2;
//
// import com.beust.jcommander.IStringConverter;
// import com.beust.jcommander.JCommander;
// import com.beust.jcommander.Parameter;
// import com.beust.jcommander.ParameterException;
// import com.google.common.collect.HashMultiset;
// import com.google.common.collect.Multiset;
//
// public class SparkItemsetMining extends ItemsetMiningCore {
//
// /** Main function parameters */
// public static class Parameters {
//
// @Parameter(names = { "-f", "--file" }, description = "Dataset filename")
// private final File dataset = new File("example.dat");
//
// @Parameter(names = { "-j", "--jar" }, description = "IIM Standalone jar")
// private final String IIMJar = "itemset-mining/target/itemset-mining-1.0.jar";
//
// @Parameter(names = { "-s", "--maxSteps" }, description = "Max structure
// steps")
// int maxStructureSteps = 100_000;
//
// @Parameter(names = { "-i", "--iterations" }, description = "Max iterations")
// int maxEMIterations = 1_000;
//
// @Parameter(names = { "-c", "--cores" }, description = "No cores")
// int noCores = 16;
//
// @Parameter(names = { "-l", "--log-level" }, description = "Log level",
// converter = LogLevelConverter.class)
// Level logLevel = Level.FINE;
//
// @Parameter(names = { "-r", "--runtime" }, description = "Max Runtime (min)")
// long maxRunTime = 12 * 60; // 12hrs
//
// @Parameter(names = { "-t", "--timestamp" }, description = "Timestamp
// Logfile", arity = 1)
// boolean timestampLog = true;
//
// @Parameter(names = { "-v", "--verbose" }, description = "Print to console
// instead of logfile")
// private boolean verbose = false;
// }
//
// public static void main(final String[] args) throws IOException {
//
// // Use greedy inference algorithm for Spark
// final InferenceAlgorithm inferenceAlg = new InferGreedy();
//
// final Parameters params = new Parameters();
// final JCommander jc = new JCommander(params);
//
// try {
// jc.parse(args);
//
// // Set up spark and HDFS
// final JavaSparkContext sc = setUpSpark(params.dataset.getName(),
// params.IIMJar, params.noCores);
// final FileSystem hdfs = setUpHDFS();
//
// // Set loglevel, runtime, timestamp and log file
// LOG_LEVEL = params.logLevel;
// MAX_RUNTIME = params.maxRunTime * 60 * 1_000;
// File logFile = null;
// if(!params.verbose)
// logFile = Logging.getLogFileName("IIM",
// params.timestampLog, LOG_DIR, params.dataset);
//
// mineItemsets(params.dataset, hdfs, sc, inferenceAlg,
// params.maxStructureSteps, params.maxEMIterations, logFile);
//
// } catch (final ParameterException e) {
// System.out.println(e.getMessage());
// jc.usage();
// }
//
// }
//
// public static Map<Itemset, Double> mineItemsets(final File inputFile,
// final FileSystem hdfs, final JavaSparkContext sc,
// final InferenceAlgorithm inferenceAlg, final int maxStructureSteps,
// final int maxEMIterations, final File logFile) throws IOException {
//
// // Set up logging
// if (logFile != null)
// Logging.setUpFileLogger(logger, LOG_LEVEL, logFile);
// else
// Logging.setUpConsoleLogger(logger, LOG_LEVEL);
//
// // Echo input parameters
// logger.info("========== SPARK INTERESTING ITEMSET MINING ============");
// logger.info("\n Time: "
// + new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss")
// .format(new Date()));
// logger.info("\n Inputs: -f " + inputFile + " -s " + maxStructureSteps
// + " -i " + maxEMIterations + " -c "
// + sc.getLocalProperty("spark.cores.max") + " -r " + MAX_RUNTIME
// / 60_000 + "\n");
//
// // Load Spark and HDFS Properties
// Properties prop = new Properties();
// prop.load(SparkItemsetMining.class.getResourceAsStream("/spark.properties"));
//
// // Copy transaction database to hdfs
// final String datasetPath = prop.getProperty("HDFSMaster")
// + inputFile.getName();
// hdfs.copyFromLocalFile(new Path(inputFile.getAbsolutePath()), new Path(
// datasetPath));
// hdfs.setReplication(new Path(datasetPath),
// Short.parseShort(prop.getProperty("MachinesInCluster")));
// try { // Wait for file to replicate
// Thread.sleep(10 * 1000);
// } catch (final InterruptedException e) {
// e.printStackTrace();
// }
//
// // Read in transaction database
// final int noCores = Integer.parseInt(sc.getConf()
// .get("spark.cores.max"));
// final JavaRDD<Transaction> db = sc.textFile(datasetPath, 2 * noCores)
// .map(new ParseTransaction()).cache();
//
// // Determine most frequent singletons
// final Map<Integer, Integer> singletonsMap = db.flatMap(t -> t)
// .mapToPair(i -> new Tuple2<Integer, Integer>(i, 1))
// .reduceByKey((a, b) -> a + b).collectAsMap();
//
// // Convert singletons map to Multiset (as Spark map is not serializable)
// final Multiset<Integer> singletons = HashMultiset.create();
// for (final Entry<Integer, Integer> entry : singletonsMap.entrySet())
// singletons.add(entry.getKey(), entry.getValue());
//
// // Apply the algorithm to build the itemset tree
// final ItemsetTree tree = new ItemsetTree(singletons);
// tree.buildTree(datasetPath, hdfs);
// if (LOG_LEVEL.equals(Level.FINE))
// tree.printStatistics(logger);
//
// // Run inference to find interesting itemsets
// final TransactionRDD transactions = new TransactionRDD(db, db.count());
// logger.fine("\n============= ITEMSET INFERENCE =============\n");
// final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
// singletons, tree, inferenceAlg, maxStructureSteps,
// maxEMIterations);
//
// // Sort itemsets by interestingness
// final HashMap<Itemset, Double> intMap = calculateInterestingness(
// itemsets, transactions, tree);
// final Map<Itemset, Double> sortedItemsets = sortItemsets(itemsets,
// intMap);
//
// logger.info("\n============= INTERESTING ITEMSETS =============\n");
// for (final Entry<Itemset, Double> entry : sortedItemsets.entrySet()) {
// logger.info(String.format("%s\tprob: %1.5f \tint: %1.5f %n",
// entry.getKey(), entry.getValue(),
// intMap.get(entry.getKey())));
// }
// logger.info("\n");
//
// return sortedItemsets;
// }
//
// /** Set up Spark */
// public static JavaSparkContext setUpSpark(final String dataset, final String
// IIMJar,
// final int noCores) throws IOException {
//
// // Load Spark and HDFS Properties
// Properties prop = new Properties();
// prop.load(SparkItemsetMining.class.getResourceAsStream("/spark.properties"));
//
// final SparkConf conf = new SparkConf();
// conf.setMaster(prop.getProperty("SparkMaster"))
// .setAppName("Itemset Mining: " + dataset)
// .setSparkHome(prop.getProperty("SparkHome"))
// .setJars(new String[] {IIMJar});
// conf.set("spark.cores.max", Integer.toString(noCores));
// conf.set("spark.executor.memory", "20g");
// conf.set("spark.default.parallelism", "8");
// conf.set("spark.shuffle.manager", "SORT");
// // conf.set("spark.eventLog.enabled", "true"); uses GB of space!!!
//
// // Use Kryo for serialization - much faster!
// conf.set("spark.serializer",
// "org.apache.spark.serializer.KryoSerializer");
// conf.set("spark.kryo.registrator",
// "itemsetmining.util.ClassRegistrator");
//
// final JavaSparkContext sc = new JavaSparkContext(conf);
// sc.setCheckpointDir(prop.getProperty("HDFSMaster")
// + "checkpoint/");
// return sc;
// }
//
// /** Set up HDFS */
// public static FileSystem setUpHDFS() throws IOException {
//
// // Load Spark and HDFS Properties
// Properties prop = new Properties();
// prop.load(SparkItemsetMining.class.getResourceAsStream("/spark.properties"));
//
// final Configuration conf = new Configuration();
// conf.addResource(new Path(prop.getProperty("HDFSConfFile")));
// return FileSystem.get(conf);
// }
//
// /** Read in transactions */
// private static class ParseTransaction implements
// Function<String, Transaction> {
// private static final long serialVersionUID = -9092218383491621520L;
//
// @Override
// public Transaction call(final String line) {
//
// // create a structure for storing the transaction
// final Transaction transaction = new Transaction();
//
// // split the transaction into items
// final String[] lineSplit = line.split(" ");
//
// // for each item in the transaction
// for (int i = 0; i < lineSplit.length; i++) {
// // convert the item to integer and add it to the structure
// transaction.add(Integer.parseInt(lineSplit[i]));
// }
//
// return transaction;
// }
// }
//
// /** Convert string level to level class */
// public static class LogLevelConverter implements IStringConverter<Level> {
// @Override
// public Level convert(final String value) {
// if (value.equals("SEVERE"))
// return Level.SEVERE;
// else if (value.equals("WARNING"))
// return Level.WARNING;
// else if (value.equals("INFO"))
// return Level.INFO;
// else if (value.equals("CONFIG"))
// return Level.CONFIG;
// else if (value.equals("FINE"))
// return Level.FINE;
// else if (value.equals("FINER"))
// return Level.FINER;
// else if (value.equals("FINEST"))
// return Level.FINEST;
// else
// throw new RuntimeException("Incorrect Log Level.");
// }
// }
//
// }
