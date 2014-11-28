package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.TransactionGenerator;
import itemsetmining.util.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetPrecisionRecall {

	/** Main Settings */
	private static final String algorithm = "IIM";
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** FIM Issues to incorporate */
	private static final String name = "caviar";
	private static final int noIterations = 300;

	/** Previously mined Itemsets to use for background distribution */
	private static final File itemsetLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/plants-20.10.2014-11:12:45.log");
	private static final int noTransactions = 10_000;

	/** MTV/FIM Settings */
	private static final double minSup = 0.0099;
	private static final double minSupMTV = 0.0099;

	/** Spark Settings */
	private static final int sparkCores = 64;
	private static final Level LOG_LEVEL = Level.FINE;
	private static final long MAX_RUNTIME = 12 * 60; // 12hrs
	private static final int maxStructureSteps = 100_000_000;

	public static void main(final String[] args) throws IOException {

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ algorithm + "_" + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Read in background distribution
		final HashMap<Itemset, Double> backgroundItemsets = ItemsetPrecisionRecall
				.readIIMItemsets(itemsetLog);

		precisionRecall(backgroundItemsets, new int[] { 30 });

	}

	public static void precisionRecall(
			final HashMap<Itemset, Double> backgroundItemsets,
			final int[] specialFrequency) throws IOException {

		final int len = specialFrequency.length;
		final double[] time = new double[len];
		final double[] precision = new double[len];
		final double[] recall = new double[len];

		for (int i = 0; i < len; i++) {
			final int noSpecialItemsets = specialFrequency[i];
			System.out.println("\nSpecial Itemsets: " + noSpecialItemsets);

			// Set up transaction DB
			final HashMap<Itemset, Double> specialItemsets = TransactionGenerator
					.generateExampleItemsets(name, noSpecialItemsets, 0);
			backgroundItemsets.putAll(specialItemsets);

			// Generate transaction DB
			final HashMap<Itemset, Double> itemsets = TransactionGenerator
					.generateTransactionDatabase(backgroundItemsets,
							noTransactions, dbFile);
			System.out.print("\n============= ACTUAL ITEMSETS =============\n");
			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
				System.out.print(String.format("%s\tprob: %1.5f %n",
						entry.getKey(), entry.getValue()));
			}
			System.out.print("\n");
			System.out.println("No itemsets: " + itemsets.size());
			ItemsetScaling.printTransactionDBStats(dbFile);

			// Mine itemsets
			Set<Itemset> minedItemsets = null;
			final File logFile = Logging.getLogFileName(algorithm, true,
					saveDir, dbFile);
			final long startTime = System.currentTimeMillis();
			if (algorithm.equals("spark"))
				minedItemsets = runSpark(sparkCores, noIterations).keySet();
			else if (algorithm.equals("MTV"))
				minedItemsets = MTVItemsetMining.mineItemsets(dbFile,
						minSupMTV, noIterations, logFile).keySet();
			else if (algorithm.equals("FIM"))
				minedItemsets = FrequentItemsetMining
						.mineFrequentItemsetsFPGrowth(dbFile.getAbsolutePath(),
								logFile.getAbsolutePath(), minSup).keySet();
			else if (algorithm.equals("IIM"))
				minedItemsets = ItemsetMining.mineItemsets(dbFile,
						new InferGreedy(), maxStructureSteps, noIterations,
						logFile).keySet();
			else
				throw new RuntimeException("Incorrect algorithm name.");
			final long endTime = System.currentTimeMillis();
			final double tim = (endTime - startTime) / (double) 1000;
			time[i] += tim;

			// Calculate precision and recall
			System.out.println("No. mined itemsets: " + minedItemsets.size());
			final double noInBoth = Sets.intersection(itemsets.keySet(),
					minedItemsets).size();
			final double noSpecialInBoth = Sets.intersection(
					specialItemsets.keySet(), minedItemsets).size();
			final double pr = noInBoth / (double) minedItemsets.size();
			final double rec = noSpecialInBoth
					/ (double) specialItemsets.size();
			precision[i] += pr;
			recall[i] += rec;

			// Display precision and recall
			System.out.printf("Precision: %.2f%n", pr);
			System.out.printf("Recall: %.2f%n", rec);
			System.out.printf("Time (s): %.2f%n", tim);

		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Special Frequency: "
				+ Arrays.toString(specialFrequency));
		System.out.println("Time: " + Arrays.toString(time));
		System.out.println("Precision (all): " + Arrays.toString(precision));
		System.out.println("Recall (special): " + Arrays.toString(recall));

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
		final HashMap<Itemset, Double> itemsets = readIIMItemsets(output);

		final String timestamp = new SimpleDateFormat("-dd.MM.yyyy-HH:mm:ss")
				.format(new Date());
		final File newLog = new File(ItemsetMining.LOG_DIR + "/" + name
				+ timestamp + ".log");
		Files.move(output, newLog);

		return itemsets;
	}

	/** Read output itemsets from file */
	static HashMap<Itemset, Double> readIIMItemsets(final File output)
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
