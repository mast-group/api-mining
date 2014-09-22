package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.TransactionGenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetPrecisionRecall {

	/** Main Settings */
	private static final String name = "caviar";
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File logDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/");
	private static final File saveDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/ItemsetEval");
	private static final InferenceAlgorithm inferenceAlg = new InferGreedy();
	private static final boolean useFIM = false;

	/** Itemset Settings */
	private static final int noSamples = 1;
	private static final int noNoisyItemsets = 45;
	private static final int noSpecialItemsets = 5;
	private static final double MU = 0.912831416393;
	private static final double SIGMA = 0.403254861612;
	private static final double PMIN = 0.01;
	private static final double PMAX = 0.1;
	private static final int noTransactions = 100_000;

	/** Spark Settings */
	private static final boolean useSpark = true;
	private static final int sparkCores = 64;
	private static final Level LOG_LEVEL = Level.FINE;
	private static final long MAX_RUNTIME = 3 * 60; // 1hr
	private static final int maxStructureSteps = 10_000;
	private static final int maxEMIterations = 100;

	public static void main(final String[] args) throws IOException {

		precisionRecall("difficulty", new int[] { 10 });
		// precisionRecall("robustness", 20);

	}

	public static void precisionRecall(final String type, final int[] levels)
			throws IOException {

		final int noLevels = levels.length;
		final double[] time = new double[noLevels + 1];
		final double[] precision = new double[noLevels + 1];
		final double[] recall = new double[noLevels + 1];
		final double[] accuracy = new double[noLevels + 1];

		for (int i = 0; i < noLevels; i++) {

			int difficultyLevel;
			int extraSets;
			if (type.equals("difficulty")) {
				difficultyLevel = levels[i];
				extraSets = noNoisyItemsets;
				System.out.println("\n========= Difficulty level "
						+ difficultyLevel + " from " + Arrays.toString(levels));
			} else if (type.equals("robustness")) {
				difficultyLevel = 0;
				extraSets = levels[i];
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
					.getNoisyItemsets(extraSets, MU, SIGMA, PMIN, PMAX);

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
				time[i] += tim;

				// Calculate precision and recall
				final double noInBoth = Sets.intersection(
						actualItemsets.keySet(), minedItemsets.keySet()).size();
				final double noExamplesInBoth = Sets.intersection(
						exampleItemsets.keySet(), minedItemsets.keySet())
						.size();
				final double pr = noInBoth / (double) minedItemsets.size();
				final double rec = noInBoth / (double) actualItemsets.size();
				final double acc = noExamplesInBoth
						/ (double) exampleItemsets.size();
				precision[i] += pr;
				recall[i] += rec;
				accuracy[i] += acc;

				// Display precision and recall
				System.out.printf("Precision All: %.2f%n", pr);
				System.out.printf("Recall All: %.2f%n", rec);
				System.out.printf("Accuracy Special: %.2f%n", acc);
				System.out.printf("Time (s): %.2f%n", tim);
			}
		}

		for (int i = 0; i < noLevels; i++) {

			// Average over samples
			precision[i] /= noSamples;
			recall[i] /= noSamples;
			accuracy[i] /= noSamples;
			time[i] /= noSamples;

			// Display average precision and recall
			if (type.equals("difficulty"))
				System.out
						.println("\n========= Difficulty Level: " + levels[i]);
			if (type.equals("robustness"))
				System.out.println("\n========= Extra Sets: " + levels[i]);
			System.out.printf("Average Precision All: %.2f%n", precision[i]);
			System.out.printf("Average Recall All: %.2f%n", recall[i]);
			System.out.printf("Average Acccuracy Special: %.2f%n", accuracy[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		// Output precision and recall
		if (type.equals("difficulty"))
			System.out.println("Levels: " + Arrays.toString(levels));
		if (type.equals("robustness"))
			System.out.println("No extra sets -1: " + Arrays.toString(levels));
		System.out.println("\n======== " + name + " ========");
		System.out.println("Time: " + Arrays.toString(time));
		System.out.println("Precision All: " + Arrays.toString(precision));
		System.out.println("Recall All: " + Arrays.toString(recall));
		System.out.println("Accuracy Special: " + Arrays.toString(accuracy));

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
		out.println(Arrays.toString(accuracy));
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
