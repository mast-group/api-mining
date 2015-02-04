package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;
import itemsetmining.transaction.TransactionGenerator;
import itemsetmining.util.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.collect.Sets;

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
	private static final int maxStructureSteps = 100_000_000;

	public static void main(final String[] args) throws IOException {

		// Read in background distribution
		final Map<Itemset, Double> backgroundItemsets = ItemsetMiningCore
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
			minedItemsets = ItemsetPrecisionRecall.runSpark(sparkCores,
					noIterations).keySet();
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
		for (int k = 1; k <= minedItemsets.size(); k++) {

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
			precision[k - 1] = pr;
			recall[k - 1] = rec;
		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Special Frequency: " + noSpecialItemsets);
		System.out.println("Time: " + time);
		System.out.println("Precision (all): " + Arrays.toString(precision));
		System.out.println("Recall (special): " + Arrays.toString(recall));

	}

}
