package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

public class ItemsetScaling {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/ItemsetEval");

	/** Scaling Settings */
	private static final int noSamples = 1;
	private static final double MU = 0.912831416393;
	private static final double SIGMA = 0.403254861612;
	private static final double PMIN = 0.01;
	private static final double PMAX = 0.1;

	/** For transaction scaling */
	private static final int noItemsets = 30;

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
		// final int[] cores = new int[] { 16, 32, 48, 64 };
		// for (final int noCores : cores)
		// Makes sense as 10^3 * PMIN = 10
		scalingTransactions(true, 64, new int[] { 1_000, 10_000, 100_000,
				1_000_000, 10_000_000, 100_000_000 });

		// Here itemset sizes are relative
		scalingItemsets(true, 64, new double[] { 0.05, 0.1, 0.15, 0.2 });

	}

	public static void scalingTransactions(final boolean useSpark,
			final int noCores, final int[] trans) throws IOException,
			InterruptedException {

		final double[] time = new double[trans.length];
		final DecimalFormat formatter = new DecimalFormat("0.0E0");

		// Generate real itemsets
		final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
				.getNoisyItemsets(noItemsets, MU, SIGMA, PMIN, PMAX);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : actualItemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}
		System.out.print("\n");

		// Save to file
		String name = InetAddress.getLocalHost().getHostName();
		if (useSpark)
			name = "Spark";
		String prefix = "";
		if (useSpark)
			prefix += "spark_";
		final PrintWriter out = new PrintWriter(new FileOutputStream(saveDir
				+ "/" + prefix + name + "_scaling.txt"), true);

		for (int i = 0; i < trans.length; i++) {

			final int tran = trans[i];
			System.out.println("\n========= " + formatter.format(tran)
					+ " Transactions");
			out.println("\n========= " + formatter.format(tran)
					+ " Transactions");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					tran, dbFile);

			for (int sample = 0; sample < noSamples; sample++) {
				System.out.println("\n========= Sample " + (sample + 1)
						+ " of " + noSamples);
				out.println("\n========= Sample " + (sample + 1) + " of "
						+ noSamples);

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
				out.printf("Time (s): %.2f%n", tim);
			}
		}

		for (int i = 0; i < trans.length; i++) {

			// Average over samples
			time[i] /= noSamples;

			// Display average precision and recall
			System.out.println("\n========= No Transactions: "
					+ formatter.format(trans[i]));
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		// Plot time
		System.out.println("\n========" + name + "========");
		System.out.println("Transactions:" + Arrays.toString(trans));
		System.out.println("Time: " + Arrays.toString(time));

		// and save to file
		out.println(Arrays.toString(trans));
		out.println(Arrays.toString(time));
		out.close();
	}

	public static void scalingItemsets(final boolean useSpark,
			final int noCores, final double[] relItemsets) throws IOException {

		final int itemsets[] = new int[relItemsets.length];
		for (int i = 0; i < relItemsets.length; i++)
			itemsets[i] = (int) (relItemsets[i] * noTransactions);
		final double[] time = new double[itemsets.length];

		// Generate real itemsets
		for (int i = 0; i < itemsets.length; i++) {

			final int noSets = itemsets[i];
			System.out.println("\n========= " + noSets + " Itemsets");

			final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
					.getNoisyItemsets(noSets, MU, SIGMA, PMIN, PMAX);
			System.out.print("\n============= ACTUAL ITEMSETS =============\n");
			for (final Entry<Itemset, Double> entry : actualItemsets.entrySet()) {
				System.out.print(String.format("%s\tprob: %1.5f %n",
						entry.getKey(), entry.getValue()));
			}
			System.out.print("\n");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					noTransactions, dbFile);

			for (int sample = 0; sample < noSamples; sample++) {
				System.out.println("\n========= Sample " + (sample + 1)
						+ " of " + noSamples);

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

			// Average over samples
			time[i] /= noSamples;

			// Display
			System.out.println("\n========= No Itemsets: " + itemsets[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		String name = InetAddress.getLocalHost().getHostName();
		if (useSpark)
			name = "Spark";
		System.out.println("\n========" + name + "========");
		System.out.println("Itemsets: " + Arrays.toString(itemsets));
		System.out.println("Time: " + Arrays.toString(time));
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
}
