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

public class BackgroundPrecisionRecall {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** FIM Issues to incorporate */
	private static final String name = "Background";
	private static final int noIterations = 500;

	/** Previously mined Itemsets to use for background distribution */
	private static final File itemsetLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/plants-20.10.2014-11:12:45.log");
	private static final int noTransactions = 10_000;
	private static final int noSpecialItemsets = 30;

	/** MTV/FIM Settings */
	private static final int noMTVIterations = 250;
	private static final double minSup = 0.05750265949;
	private static final double minSupMTV = 0.05750265949;

	/** Spark Settings */
	private static final int sparkCores = 64;
	private static final Level LOG_LEVEL = Level.FINE;
	private static final long MAX_RUNTIME = 12 * 60; // 12hrs
	private static final int maxStructureSteps = 100_000_000;

	public static void main(final String[] args) throws IOException {

		// Read in background distribution
		final HashMap<Itemset, Double> backgroundItemsets = BackgroundPrecisionRecall
				.readIIMItemsets(itemsetLog);

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

		precisionRecall(itemsets, "IIM");
		precisionRecall(itemsets, "MTV");
		precisionRecall(itemsets, "FIM");

	}

	public static void precisionRecall(final HashMap<Itemset, Double> itemsets,
			final String algorithm) throws IOException {

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ algorithm + "_" + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		System.out.println("\nSpecial Itemsets: " + noSpecialItemsets);

		// Mine itemsets
		Set<Itemset> minedItemsets = null;
		final File logFile = Logging.getLogFileName(algorithm, true, saveDir,
				dbFile);
		final long startTime = System.currentTimeMillis();
		if (algorithm.equals("spark"))
			minedItemsets = runSpark(sparkCores, noIterations).keySet();
		else if (algorithm.equals("MTV"))
			minedItemsets = MTVItemsetMining.mineItemsets(dbFile, minSupMTV,
					noMTVIterations, logFile).keySet();
		else if (algorithm.equals("FIM")) {
			FrequentItemsetMining
					.mineFrequentItemsetsFPGrowth(dbFile.getAbsolutePath(),
							logFile.getAbsolutePath(), minSup);
			minedItemsets = FrequentItemsetMining.readFrequentItemsets(logFile)
					.keySet();
		} else if (algorithm.equals("IIM"))
			minedItemsets = ItemsetMining
					.mineItemsets(dbFile, new InferGreedy(), maxStructureSteps,
							noIterations, logFile).keySet();
		else
			throw new RuntimeException("Incorrect algorithm name.");
		final long endTime = System.currentTimeMillis();
		final double time = (endTime - startTime) / (double) 1000;

		// Calculate sorted precision and recall
		final int len = minedItemsets.size();
		System.out.println("No. mined itemsets: " + len);
		final double[] precision = new double[len];
		final double[] recall = new double[len];
		for (int k = 1; k < minedItemsets.size(); k++) {

			final Set<Itemset> topKMined = Sets.newHashSet();
			for (final Itemset set : minedItemsets) {
				topKMined.add(set);
				if (topKMined.size() == k)
					break;
			}

			final double noInBoth = Sets.intersection(itemsets.keySet(),
					topKMined).size();
			final double pr = noInBoth / (double) topKMined.size();
			final double rec = noInBoth / (double) itemsets.size();
			precision[k] += pr;
			recall[k] += rec;
		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Special Frequency: " + noSpecialItemsets);
		System.out.println("Time: " + time);
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
