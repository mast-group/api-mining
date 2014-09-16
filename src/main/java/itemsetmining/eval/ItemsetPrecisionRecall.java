package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ItemsetPrecisionRecall {

	/** Main Settings */
	private static final String name = "caviar";
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/ItemsetEval");
	private static final InferenceAlgorithm inferenceAlg = new InferGreedy();
	private static final boolean useFIM = false;

	/** Itemset Settings */
	private static final int noSamples = 1;
	private static final int noNoisyItemsets = 18;
	private static final int noSpecialItemsets = 2;
	private static final double MU = 0.910658459511;
	private static final double SIGMA = 1.02333623562;
	private static final int difficultyLevels = 0;
	private static final int noTransactions = 1_000;

	/** Spark Settings */
	private static final boolean useSpark = true;
	private static final int sparkCores = 64;
	protected static Level LOG_LEVEL = Level.INFO;
	protected static long MAX_RUNTIME = 1 * 60; // 1hr
	private static final int maxStructureSteps = 10_000;
	private static final int maxEMIterations = 100;

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

		for (int level = 0; level <= noLevels; level++) {

			int difficultyLevel;
			int extraSets;
			if (type.equals("difficulty")) {
				difficultyLevel = level;
				extraSets = noNoisyItemsets;
				System.out.println("\n========= Difficulty level " + level
						+ " of " + noLevels);
			} else if (type.equals("robustness")) {
				difficultyLevel = 0;
				extraSets = level + 1;
				System.out.println("\n========= No. Noisy Itemsets: "
						+ extraSets);
			} else
				throw new RuntimeException("Incorrect argument.");

			// Generate real itemsets
			final HashMap<Itemset, Double> exampleItemsets = TransactionGenerator
					.generateExampleItemsets(name, noSpecialItemsets,
							difficultyLevel);

			// Generate some noise
			final HashMap<Itemset, Double> noisyItemsets = TransactionGenerator
					.getNoisyItemsets(extraSets, MU, SIGMA);

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
					noTransactions, dbFile);

			for (int sample = 0; sample < noSamples; sample++) {
				System.out.println("\n========= Sample " + (sample + 1)
						+ " of " + noSamples);

				// Mine itemsets
				HashMap<Itemset, Double> minedItemsets = null;
				final long startTime = System.currentTimeMillis();
				if (useFIM)
					minedItemsets = MTVItemsetMining.mineItemsets(dbFile, 0,
							actualItemsets.size() + 5);
				else if (useSpark) {
					minedItemsets = runSpark(sparkCores);
				} else
					minedItemsets = ItemsetMining.mineItemsets(dbFile,
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
		if (useFIM)
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

	private static HashMap<Itemset, Double> runSpark(final int noCores)
			throws IOException {
		final String cmd[] = new String[8];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/miltository/projects/itemset-mining/run-spark.sh";
		cmd[1] = "-f " + dbFile;
		cmd[2] = " -s " + maxStructureSteps;
		cmd[3] = " -i " + maxEMIterations;
		cmd[4] = " -c " + noCores;
		cmd[5] = " -l " + LOG_LEVEL;
		cmd[6] = " -r " + MAX_RUNTIME;
		cmd[7] = " -t false";
		MTVItemsetMining.runScript(cmd);

		final File output = new File(ItemsetMining.LOG_DIR
				+ FilenameUtils.getBaseName(dbFile.getName()) + ".log");
		final HashMap<Itemset, Double> itemsets = readSparkOutput(output);

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
