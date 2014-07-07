package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.api.java.JavaSparkContext;

import cern.colt.Arrays;

public class ItemsetScaling {

	private static final File dbFile = new File("/tmp/itemset.txt");
	private static final InferenceAlgorithm inferenceAlg = new InferGreedy();

	private static final int noSamples = 1;

	private static final int noTransactions = 1000;
	private static final int noExtraSets = 5;
	private static final int maxSetSize = 3;

	private static final int maxStructureSteps = 500;
	private static final int maxEMIterations = 20;

	public static void main(final String[] args) throws IOException {

		// Run with and without spark
		// scalingTransactions(false, 6);
		// scalingTransactions(true, 8);

		// scalingItemsets(false, 4);
		// scalingItemsets(true, 4);

		scalingItems(false, 7);
		// scalingItems(true, 7);

	}

	public static void scalingTransactions(final boolean useSpark,
			final int noLogTransactions) throws IOException {

		final double[] trans = new double[noLogTransactions];
		final double[] time = new double[noLogTransactions];

		FileSystem hdfs = null;
		JavaSparkContext sc = null;
		if (useSpark) {
			sc = SparkItemsetMining.setUpSpark(dbFile.getName());
			hdfs = SparkItemsetMining.setUpHDFS();
		}

		// Generate real itemsets
		final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
				.getNoisyItemsets(noExtraSets, maxSetSize);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : actualItemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}
		System.out.print("\n");

		for (int i = 0; i < noLogTransactions; i++) {

			final int tran = (int) Math.pow(10, i + 1);
			System.out.println("\n========= 10^" + (i + 1) + " Transactions");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					tran, dbFile);

			for (int sample = 0; sample < noSamples; sample++) {
				System.out.println("\n========= Sample " + (sample + 1)
						+ " of " + noSamples);

				// Mine itemsets
				final long startTime = System.currentTimeMillis();
				if (useSpark)
					SparkItemsetMining.mineItemsets(dbFile, hdfs, sc,
							inferenceAlg, maxStructureSteps, maxEMIterations);
				else
					ItemsetMining.mineItemsets(dbFile, inferenceAlg,
							maxStructureSteps, maxEMIterations);

				final long endTime = System.currentTimeMillis();
				final double tim = (endTime - startTime) / (double) 1000;
				time[i] += tim;

				System.out.printf("Time (s): %.2f%n", tim);
			}
		}

		for (int i = 0; i < noLogTransactions; i++) {

			// Average over samples
			time[i] /= noSamples;
			trans[i] = (int) Math.pow(10, i + 1);

			// Display average precision and recall
			System.out.println("\n========= No Transactions: " + trans[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		// Plot time
		String name = InetAddress.getLocalHost().getHostName();
		if (useSpark)
			name = "Spark";
		System.out.println("\n========" + name + "========");
		System.out.println("Transactions:" + Arrays.toString(trans));
		System.out.println("Time: " + Arrays.toString(time));
	}

	public static void scalingItemsets(final boolean useSpark,
			final int noLogSets) throws IOException {

		scalingItemsOrItemsets(useSpark, noLogSets, true);
	}

	public static void scalingItems(final boolean useSpark,
			final int logMaxSetSize) throws IOException {

		scalingItemsOrItemsets(useSpark, logMaxSetSize, false);
	}

	public static void scalingItemsOrItemsets(final boolean useSpark,
			final int param, final boolean scaleItemsets) throws IOException {

		final double[] itemsets = new double[param];
		final double[] time = new double[param];

		FileSystem hdfs = null;
		JavaSparkContext sc = null;
		if (useSpark) {
			sc = SparkItemsetMining.setUpSpark(dbFile.getName());
			hdfs = SparkItemsetMining.setUpHDFS();
		}

		// Generate real itemsets
		for (int i = 0; i < param; i++) {

			int noSets;
			int maxSets;
			if (scaleItemsets) {
				noSets = (int) ((5 * (i + 1)) / 100. * noTransactions);
				maxSets = maxSetSize;
				System.out.println("\n========= " + noSets + " Itemsets");
			} else {
				noSets = noExtraSets;
				maxSets = 10 * (i + 1);
				System.out.println("\n========= Max Itemset size: " + maxSets);
			}

			final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
					.getNoisyItemsets(noSets, maxSets);
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
					SparkItemsetMining.mineItemsets(dbFile, hdfs, sc,
							inferenceAlg, maxStructureSteps, maxEMIterations);
				else
					ItemsetMining.mineItemsets(dbFile, inferenceAlg,
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
			if (scaleItemsets)
				itemsets[i] = (int) ((5 * (i + 1)) / 100. * noTransactions);
			else
				itemsets[i] = 10 * (i + 1);

			// Display
			if (scaleItemsets)
				System.out.println("\n========= No Itemsets: " + itemsets[i]);
			else
				System.out.println("\n========= Max Sets: " + itemsets[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		String name = InetAddress.getLocalHost().getHostName();
		if (useSpark)
			name = "Spark";
		System.out.println("\n========" + name + "========");
		if (scaleItemsets)
			System.out.println("Itemsets: " + Arrays.toString(itemsets));
		else
			System.out.println("Max Sets: " + Arrays.toString(itemsets));
		System.out.println("Time: " + Arrays.toString(time));
	}

}
