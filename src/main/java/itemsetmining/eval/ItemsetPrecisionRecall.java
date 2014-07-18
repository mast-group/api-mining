package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.SparkItemsetMining;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.api.java.JavaSparkContext;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ItemsetPrecisionRecall {

	private static final String name = "cross-supp";
	private static final File dbTmpFile = new File("/tmp/itemset.txt");
	private static final File saveDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/ItemsetEval");
	private static final InferenceAlgorithm inferenceAlg = new InferGreedy();
	private static final boolean useSpark = false;
	private static final boolean useMTV = false;

	private static final int noSamples = 10;
	private static final int difficultyLevels = 10;

	private static final int noTransactions = 1000;
	private static final int noExtraSets = 5;
	private static final int maxSetSize = 3;

	private static final int maxStructureSteps = 500;
	private static final int maxEMIterations = 20;

	public static void main(final String[] args) throws IOException {

		precisionRecall("difficulty", difficultyLevels);
		// precisionRecall("robustness", 20);

	}

	public static void precisionRecall(final String type, final int noLevels)
			throws IOException {

		final double[] levels = new double[noLevels + 1];
		final double[] time = new double[noLevels + 1];
		final double[] precision = new double[noLevels + 1];
		final double[] recall = new double[noLevels + 1];

		FileSystem hdfs = null;
		JavaSparkContext sc = null;
		if (useSpark) {
			sc = SparkItemsetMining.setUpSpark(dbTmpFile.getName());
			hdfs = SparkItemsetMining.setUpHDFS();
		}

		for (int level = 0; level <= noLevels; level++) {

			int difficultyLevel;
			int extraSets;
			if (type.equals("difficulty")) {
				difficultyLevel = level;
				extraSets = noExtraSets;
				System.out.println("\n========= Difficulty level " + level
						+ " of " + noLevels);
			} else if (type.equals("robustness")) {
				difficultyLevel = 0;
				extraSets = level + 1;
				System.out.println("\n========= Extra Sets: " + extraSets);
			} else
				throw new RuntimeException("Incorrect argument.");

			// Generate real itemsets
			final HashMap<Itemset, Double> exampleItemsets = TransactionGenerator
					.generateExampleItemsets(name, difficultyLevel);

			// Generate some noise
			final HashMap<Itemset, Double> noisyItemsets = TransactionGenerator
					.getNoisyItemsets(extraSets, maxSetSize);

			// Combine the two
			final HashMap<Itemset, Double> actualItemsets = Maps
					.newHashMap(exampleItemsets);
			actualItemsets.putAll(noisyItemsets);

			System.out.print("\n============= ACTUAL ITEMSETS =============\n");
			for (final Entry<Itemset, Double> entry : actualItemsets.entrySet()) {
				System.out.print(String.format("%s\tprob: %1.5f %n",
						entry.getKey(), entry.getValue()));
			}
			System.out.print("\n");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					noTransactions, dbTmpFile);

			for (int sample = 0; sample < noSamples; sample++) {
				System.out.println("\n========= Sample " + (sample + 1)
						+ " of " + noSamples);

				// Mine itemsets
				HashMap<Itemset, Double> minedItemsets = null;
				final long startTime = System.currentTimeMillis();
				if (useMTV)
					minedItemsets = MTVItemsetMining.mineItemsets(dbTmpFile, 0,
							actualItemsets.size() + 5);
				else if (useSpark)
					minedItemsets = SparkItemsetMining.mineItemsets(dbTmpFile,
							hdfs, sc, inferenceAlg, maxStructureSteps,
							maxEMIterations);
				else
					minedItemsets = ItemsetMining.mineItemsets(dbTmpFile,
							inferenceAlg, maxStructureSteps, maxEMIterations);
				final long endTime = System.currentTimeMillis();
				final double tim = (endTime - startTime) / (double) 1000;
				time[level] += tim;

				// Calculate precision and recall for example sets
				final Set<Itemset> minedLessNoise = Sets.difference(
						minedItemsets.keySet(), noisyItemsets.keySet());
				final double noInBoth = Sets.intersection(
						exampleItemsets.keySet(), minedItemsets.keySet())
						.size();
				final double pr = noInBoth / (double) minedLessNoise.size();
				final double rec = noInBoth / (double) exampleItemsets.size();
				precision[level] += pr;
				recall[level] += rec;

				// Display precision and recall
				System.out.printf("Precision: %.2f%n", pr);
				System.out.printf("Recall: %.2f%n", rec);
				System.out.printf("Time (s): %.2f%n", tim);
			}
		}

		for (int i = 0; i <= noLevels; i++) {

			// Average over samples
			precision[i] /= noSamples;
			recall[i] /= noSamples;
			time[i] /= noSamples;
			levels[i] = i;

			// Display average precision and recall
			if (type.equals("difficulty"))
				System.out.println("\n========= Difficulty Level: " + i);
			if (type.equals("robustness"))
				System.out.println("\n========= Extra Sets: " + (i + 1));
			System.out.printf("Average Precision: %.2f%n", precision[i]);
			System.out.printf("Average Recall: %.2f%n", recall[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		// Output precision and recall
		if (type.equals("difficulty"))
			System.out.println("Levels: " + Arrays.toString(levels));
		if (type.equals("robustness"))
			System.out.println("No extra sets -1: " + Arrays.toString(levels));
		System.out.println("\n======== " + name + " ========");
		System.out.println("Time: " + Arrays.toString(time));
		System.out.println("Precision: " + Arrays.toString(precision));
		System.out.println("Recall : " + Arrays.toString(recall));

		// and save to file
		String prefix = "";
		if (useMTV)
			prefix += "mtv_";
		if (useSpark)
			prefix += "spark_";
		final PrintWriter out = new PrintWriter(saveDir + "/" + prefix + name
				+ "_" + type + ".txt");
		out.println(Arrays.toString(levels));
		out.println(Arrays.toString(time));
		out.println(Arrays.toString(precision));
		out.println(Arrays.toString(recall));
		out.close();
	}

}
