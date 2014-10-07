package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.io.output.WriterOutputStream;

import com.beust.jcommander.internal.Sets;

public class ItemsetScaling {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File("/tmp/");
	private static final int noRuns = 1;

	/** Itemset Distribution Settings */
	private static final double P = 0.305747126437;
	private static final int NO_ITEMS = 70;
	private static final double MU = -3.72051635628;
	private static final double SIGMA = 0.994304782717;

	/** For transaction scaling */
	private static final int noItemsets = 10;

	/** For itemset scaling */
	private static final int noTransactions = 1_000;

	/** Spark Settings */
	private static final Level LOG_LEVEL = Level.FINE;
	private static final long MAX_RUNTIME = 6 * 60; // 6hrs
	private static final int maxStructureSteps = 10_000;
	private static final int maxEMIterations = 100;

	public static void main(final String[] args) throws IOException,
			InterruptedException {

		// Run with spark
		// final int[] cores = new int[] { 1, 4, 16, 64, 128 };
		// for (final int noCores : cores)
		// Makes sense as 10^3 * PMIN = 10
		// scalingTransactions(true, 16, new int[] { 1_000, 10_000, 100_000,
		// 1_000_000, 10_000_000, 100_000_000 });

		// Here itemset sizes are relative
		// scalingItemsets(true, 64, new double[] { 0.05, 0.1, 0.15, 0.2 });

		generateSyntheticDatabase(
				133,
				35000,
				new File(
						"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/plants_synthetic.dat"));
	}

	public static void scalingTransactions(final boolean useSpark,
			final int noCores, final int[] trans) throws IOException,
			InterruptedException {

		final double[] time = new double[trans.length];
		final int[] actualTrans = new int[trans.length];
		final DecimalFormat formatter = new DecimalFormat("0.0E0");

		// Save to file
		String name = InetAddress.getLocalHost().getHostName();
		if (useSpark)
			name = "Spark";
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ name + "_scaling_" + noCores + ".txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Generate real itemsets
		final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
				.generateBackgroundItemsets(noItemsets, P, NO_ITEMS, MU, SIGMA);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : actualItemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}
		System.out.print("\n");

		// Itemset stats
		System.out.println("No itemsets: " + actualItemsets.size());
		System.out
				.println("No items: " + countNoItems(actualItemsets.keySet()));

		transloop: for (int i = 0; i < trans.length; i++) {

			final int tran = trans[i];
			System.out.println("\n========= " + formatter.format(tran)
					+ " Transactions");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					tran, dbFile);
			actualTrans[i] = countNoTransactions(dbFile);
			System.out.println("Actual transactions: " + actualTrans[i]);
			System.out.println("Avg items per transaction: "
					+ getAverageItemsPerTransaction(dbFile));

			for (int run = 0; run < noRuns; run++) {
				System.out.println("\n========= Run " + (run + 1) + " of "
						+ noRuns);

				// Mine itemsets
				final long startTime = System.currentTimeMillis();
				if (useSpark)
					runSpark(noCores);
				else
					ItemsetMining.mineItemsets(dbFile, new InferGreedy(),
							maxStructureSteps, maxEMIterations);

				final long endTime = System.currentTimeMillis();
				final double tim = (endTime - startTime) / (double) 1000;
				time[i] += tim;

				System.out.printf("Time (s): %.2f%n", tim);

				if (tim > MAX_RUNTIME * 60 * 60)
					break transloop;

			}
		}

		for (int i = 0; i < trans.length; i++) {

			// Average over runs
			time[i] /= noRuns;

			// Display average precision and recall
			System.out.println("\n========= No Transactions: "
					+ formatter.format(actualTrans[i]));
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		// Print time
		System.out.println("\n========" + name + "========");
		System.out.println("Transactions:" + Arrays.toString(actualTrans));
		System.out.println("Time: " + Arrays.toString(time));

		// and save to file
		out.close();
	}

	public static void scalingItemsets(final boolean useSpark,
			final int noCores, final double[] relItemsets) throws IOException {

		final int itemsets[] = new int[relItemsets.length];
		for (int i = 0; i < relItemsets.length; i++)
			itemsets[i] = (int) (relItemsets[i] * noTransactions);
		final double[] time = new double[itemsets.length];

		// Save to file
		String name = InetAddress.getLocalHost().getHostName();
		if (useSpark)
			name = "Spark";
		final PrintWriter outFile = new PrintWriter(new FileOutputStream(
				saveDir + "/" + name + "_scaling.txt"), true);
		final TeeOutputStream out = new TeeOutputStream(System.out,
				new WriterOutputStream(outFile));
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Generate real itemsets
		for (int i = 0; i < itemsets.length; i++) {

			final int noSets = itemsets[i];
			System.out.println("\n========= " + noSets + " Itemsets");

			final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
					.generateBackgroundItemsets(noSets, P, NO_ITEMS, MU, SIGMA);
			System.out.print("\n============= ACTUAL ITEMSETS =============\n");
			for (final Entry<Itemset, Double> entry : actualItemsets.entrySet()) {
				System.out.print(String.format("%s\tprob: %1.5f %n",
						entry.getKey(), entry.getValue()));
			}
			System.out.print("\n");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					noTransactions, dbFile);

			// Get sparsity ratio of database
			final double ratio = getSparsityRatio(dbFile);
			System.out.println("Average items per transactions: " + ratio);

			for (int run = 0; run < noRuns; run++) {
				System.out.println("\n========= Run " + (run + 1) + " of "
						+ noRuns);

				// Mine itemsets
				final long startTime = System.currentTimeMillis();
				if (useSpark)
					runSpark(noCores);
				else
					ItemsetMining.mineItemsets(dbFile, new InferGreedy(),
							maxStructureSteps, maxEMIterations);

				final long endTime = System.currentTimeMillis();
				final double tim = (endTime - startTime) / (double) 1000;
				time[i] += tim;

				System.out.printf("Time (s): %.2f%n", tim);
			}
		}

		for (int i = 0; i < itemsets.length; i++) {

			// Average over runs
			time[i] /= noRuns;

			// Display
			System.out.println("\n========= No Itemsets: " + itemsets[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		// Print time
		System.out.println("\n========" + name + "========");
		System.out.println("Itemsets: " + Arrays.toString(itemsets));
		System.out.println("Time: " + Arrays.toString(time));

		// and save to file
		out.close();
	}

	/** Get sparsity ration (avg. no. items per transaction) for transaction db */
	private static double getSparsityRatio(final File dbase) throws IOException {

		final String[] lines = FileUtils.readFileToString(dbase).split("\n");

		double sparsity = 0;
		for (final String line : lines) {
			final String[] items = line.trim().split(" ");
			sparsity += items.length;
		}
		sparsity /= lines.length;

		return sparsity;
	}

	private static void runSpark(final int noCores) {
		final String cmd[] = new String[8];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/miltository/projects/itemset-mining/run-spark.sh";
		cmd[1] = "-f " + dbFile;
		cmd[2] = " -s " + maxStructureSteps;
		cmd[3] = " -i " + maxEMIterations;
		cmd[4] = " -c " + noCores;
		cmd[5] = " -l " + LOG_LEVEL;
		cmd[6] = " -r " + MAX_RUNTIME;
		cmd[7] = " -t true";
		MTVItemsetMining.runScript(cmd);
	}

	public static void generateSyntheticDatabase(final int noItemsets,
			final int noTransactions, final File dbPath) throws IOException {
		final HashMap<Itemset, Double> itemsets = TransactionGenerator
				.generateBackgroundItemsets(noItemsets, P, NO_ITEMS, MU, SIGMA);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}
		System.out.print("\n");
		System.out.println("No itemsets: " + itemsets.size());
		System.out.println("No items: " + countNoItems(itemsets.keySet()));
		TransactionGenerator.generateTransactionDatabase(itemsets,
				noTransactions, dbPath);
		System.out.println("No transactions: " + countNoTransactions(dbPath));
		System.out.println("Avg items per transaction: "
				+ getAverageItemsPerTransaction(dbPath));
	}

	/**
	 * Count the number of items in the itemsets (itemsets need not be
	 * independent)
	 */
	@SuppressWarnings("deprecation")
	public static int countNoItems(final Set<Itemset> itemsets) {
		final Set<Integer> items = Sets.newHashSet();
		for (final Itemset itemset : itemsets)
			items.addAll(itemset.getItems());
		return items.size();
	}

	/** Count the number of transactions in the database */
	public static int countNoTransactions(final File dbFile) throws IOException {
		final LineNumberReader lnr = new LineNumberReader(
				new FileReader(dbFile));
		lnr.skip(Long.MAX_VALUE);
		final int noLines = lnr.getLineNumber();
		lnr.close();
		return noLines;
	}

	/** Get the average number of items per transaction in the database */
	public static double getAverageItemsPerTransaction(final File dbFile)
			throws IOException {

		double sparsity = 0, noLines = 0;
		final LineIterator it = FileUtils.lineIterator(dbFile, "UTF-8");
		while (it.hasNext()) {
			final String[] items = it.nextLine().trim().split(" ");
			sparsity += items.length;
			noLines++;
		}
		LineIterator.closeQuietly(it);
		return sparsity /= noLines;
	}

}
