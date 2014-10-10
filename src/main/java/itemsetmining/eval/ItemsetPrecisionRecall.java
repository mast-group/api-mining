package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetPrecisionRecall {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File logDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");
	private static final InferenceAlgorithm inferenceAlg = new InferGreedy();

	/** Previously mined Itemsets to use for background distribution */
	private static final String name = "plants-based";
	private static final File itemsetLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/plants-09.10.2014-16:45:44.log");
	private static final int noTransactions = 34781;

	/** Spark Settings */
	private static final boolean useSpark = false;
	private static final int sparkCores = 64;
	private static final Level LOG_LEVEL = Level.FINE;
	private static final long MAX_RUNTIME = 12 * 60; // 12hrs
	private static final int maxStructureSteps = 100_000;

	public static void main(final String[] args) throws IOException {

		precisionRecall(new int[] { 100 });

	}

	public static void precisionRecall(final int[] iterations)
			throws IOException {

		final int lenIterations = iterations.length;
		final double[] time = new double[lenIterations];
		final double[] precision = new double[lenIterations];
		final double[] recall = new double[lenIterations];

		String prefix = "";
		if (useSpark)
			prefix += "spark_";
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ prefix + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		for (int i = 0; i < lenIterations; i++) {
			final int noIterations = iterations[i];

			// Read in previously mined itemsets
			final HashMap<Itemset, Double> itemsets = readSparkOutput(itemsetLog);

			System.out.print("\n============= ACTUAL ITEMSETS =============\n");
			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
				System.out.print(String.format("%s\tprob: %1.5f %n",
						entry.getKey(), entry.getValue()));
			}
			System.out.println("\nNo itemsets: " + itemsets.size());
			System.out.println("No items: "
					+ ItemsetScaling.countNoItems(itemsets.keySet()));

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(itemsets,
					noTransactions, dbFile);
			ItemsetScaling.printTransactionDBStats(dbFile);

			// Mine itemsets
			HashMap<Itemset, Double> minedItemsets = null;
			final long startTime = System.currentTimeMillis();
			if (useSpark) {
				minedItemsets = runSpark(sparkCores, noIterations);
			} else
				minedItemsets = ItemsetMining.mineItemsets(dbFile,
						inferenceAlg, maxStructureSteps, noIterations);
			final long endTime = System.currentTimeMillis();
			final double tim = (endTime - startTime) / (double) 1000;
			time[i] += tim;

			// Calculate precision and recall
			final double noInBoth = Sets.intersection(itemsets.keySet(),
					minedItemsets.keySet()).size();
			final double pr = noInBoth / (double) minedItemsets.size();
			final double rec = noInBoth / (double) itemsets.size();
			precision[i] += pr;
			recall[i] += rec;

			// Display precision and recall
			System.out.printf("Precision: %.2f%n", pr);
			System.out.printf("Recall: %.2f%n", rec);
			System.out.printf("Time (s): %.2f%n", tim);

		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Iterations: " + Arrays.toString(iterations));
		System.out.println("Time: " + Arrays.toString(time));
		System.out.println("Precision: " + Arrays.toString(precision));
		System.out.println("Recall: " + Arrays.toString(recall));

		// and save to file
		out.close();
	}

	private static HashMap<Itemset, Double> runSpark(final int noCores,
			final int noIterations) throws IOException {
		final String cmd[] = new String[8];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/itemset-mining/run-spark.sh";
		cmd[1] = "-f " + dbFile;
		cmd[2] = " -s " + maxStructureSteps;
		cmd[3] = " -i " + noIterations;
		cmd[4] = " -c " + noCores;
		cmd[5] = " -l " + LOG_LEVEL;
		cmd[6] = " -r " + MAX_RUNTIME;
		cmd[7] = " -t false";
		MTVItemsetMining.runScript(cmd);

		final File output = new File(ItemsetMining.LOG_DIR
				+ FilenameUtils.getBaseName(dbFile.getName()) + ".log");
		final HashMap<Itemset, Double> itemsets = readSparkOutput(output);

		final String timestamp = new SimpleDateFormat("-dd.MM.yyyy-HH:mm:ss")
				.format(new Date());
		final File newLog = new File(logDir + "/" + name + timestamp + ".log");
		Files.move(output, newLog);

		return itemsets;
	}

	/** Read Spark output itemsets to file */
	static HashMap<Itemset, Double> readSparkOutput(final File output)
			throws IOException {
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		boolean found = false;
		for (final String line : lines) {

			if (found && !line.trim().isEmpty()) {
				final String[] splitLine = line.split("\t");
				final String[] items = splitLine[0].split(",");
				items[0] = items[0].replace("{", "");
				items[items.length - 1] = items[items.length - 1].replace("}",
						"");
				final Itemset itemset = new Itemset();
				for (final String item : items)
					itemset.add(Integer.parseInt(item.trim()));
				final double prob = Double
						.parseDouble(splitLine[1].split(":")[1]);
				// double intr = Double.parseDouble(splitLine[2].split(":")[1]);
				itemsets.put(itemset, prob);
			}

			if (line.contains("INTERESTING ITEMSETS"))
				found = true;
		}
		return itemsets;
	}

}
