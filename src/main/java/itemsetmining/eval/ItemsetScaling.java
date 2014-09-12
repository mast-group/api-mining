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
	private static final double MU = 0.910658459511;
	private static final double SIGMA = 1.02333623562;

	/** For transaction scaling */
	private static final int noItemsets = 50;

	/** For itemset scaling */
	private static final int noTransactions = 100_000;

	/** Spark Settings */
	protected static Level LOG_LEVEL = Level.INFO;
	protected static long MAX_RUNTIME = 2 * 60; // 2hrs
	private static final int maxStructureSteps = 10_000;
	private static final int maxEMIterations = 100;

	public static void main(final String[] args) throws IOException,
			InterruptedException {

		// Run without spark
		// scalingTransactions(false, -1, 5);

		// Run with spark
		// final int[] cores = new int[] { 16, 32, 48, 64 };
		// for (final int noCores : cores)
		scalingTransactions(true, 64, 6);

		// scalingItemsets(false, 64, 4);
		// scalingItemsets(true, 64, 4);

		// scalingItems(false, 64, 7);
		// scalingItems(true, 64, 7);

	}

	public static void scalingTransactions(final boolean useSpark,
			final int noCores, final int noLogTransactions) throws IOException,
			InterruptedException {

		final double[] trans = new double[noLogTransactions];
		final double[] time = new double[noLogTransactions];

		// Generate real itemsets
		final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
				.getNoisyItemsets(noItemsets, MU, SIGMA);
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

		for (int i = 0; i < noLogTransactions; i++) {

			final int power = i + 3;

			final int tran = (int) Math.pow(10, power);
			System.out.println("\n========= 10^" + power + " Transactions");
			out.println("\n========= 10^" + power + " Transactions");

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

		for (int i = 0; i < noLogTransactions; i++) {

			final int power = i + 3;

			// Average over samples
			time[i] /= noSamples;
			trans[i] = (int) Math.pow(10, power);

			// Display average precision and recall
			System.out.println("\n========= No Transactions: " + trans[i]);
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
			final int noCores, final int param) throws IOException {

		final double[] itemsets = new double[param];
		final double[] time = new double[param];

		// Generate real itemsets
		for (int i = 0; i < param; i++) {

			final int noSets = (int) ((5 * (i + 1)) / 100. * noTransactions);
			System.out.println("\n========= " + noSets + " Itemsets");

			final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
					.getNoisyItemsets(noSets, MU, SIGMA);
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

		for (int i = 0; i < param; i++) {

			// Average over samples
			time[i] /= noSamples;
			itemsets[i] = (int) ((5 * (i + 1)) / 100. * noTransactions);

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
		cmd[5] = " -l" + LOG_LEVEL;
		cmd[6] = " -r" + MAX_RUNTIME;
		cmd[7] = " -t false";
		MTVItemsetMining.runScript(cmd);
	}
}
