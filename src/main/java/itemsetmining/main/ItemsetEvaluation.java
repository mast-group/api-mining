package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.TransactionGenerator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.api.java.JavaSparkContext;
import org.math.plot.Plot2DPanel;

import com.google.common.collect.Sets;

public class ItemsetEvaluation {

	private static final String name = "overlap";
	private static final File dbFile = new File("/tmp/itemset.txt");
	private static final File plotDir = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes");
	private static final InferenceAlgorithm inferenceAlg = new InferGreedy();
	private static final boolean useSpark = false;

	private static final int noSamples = 10;
	private static final int difficultyLevels = 10;

	private static final int noTransactions = 1000;
	private static final int noExtraSets = 5;
	private static final int maxSetSize = 3;

	private static final int maxStructureSteps = 500;
	private static final int maxEMIterations = 20;

	public static void main(final String[] args) throws IOException {

		final double[] levels = new double[difficultyLevels + 1];
		final double[] time = new double[difficultyLevels + 1];
		final double[] precision = new double[difficultyLevels + 1];
		final double[] recall = new double[difficultyLevels + 1];

		FileSystem hdfs = null;
		JavaSparkContext sc = null;
		if (useSpark) {
			sc = SparkItemsetMining.setUpSpark(dbFile.getName());
			hdfs = SparkItemsetMining.setUpHDFS();
		}

		for (int level = 0; level <= difficultyLevels; level++) {
			System.out.println("\n========= Level " + level + " of "
					+ difficultyLevels);

			// Generate real itemsets
			final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
					.generateItemsets(name, level, noExtraSets, maxSetSize);
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
				if (useSpark)
					minedItemsets = SparkItemsetMining.mineItemsets(dbFile,
							hdfs, sc, inferenceAlg, maxStructureSteps,
							maxEMIterations);
				else
					minedItemsets = ItemsetMining.mineItemsets(dbFile,
							inferenceAlg, maxStructureSteps, maxEMIterations);
				final long endTime = System.currentTimeMillis();
				final double tim = (endTime - startTime) / (double) 1000;
				time[level] += tim;

				// Calculate precision and recall
				final double noInBoth = Sets.intersection(
						actualItemsets.keySet(), minedItemsets.keySet()).size();
				final double pr = noInBoth / (double) minedItemsets.size();
				final double rec = noInBoth / (double) actualItemsets.size();
				precision[level] += pr;
				recall[level] += rec;

				// Display precision and recall
				System.out.printf("Precision: %.2f%n", pr);
				System.out.printf("Recall: %.2f%n", rec);
				System.out.printf("Time (s): %.2f%n", tim);
			}
		}

		for (int i = 0; i <= difficultyLevels; i++) {

			// Average over samples
			precision[i] /= noSamples;
			recall[i] /= noSamples;
			time[i] /= noSamples;
			levels[i] = i;

			// Display average precision and recall
			System.out.println("\n========= Difficulty Level: " + i);
			System.out.printf("Average Precision: %.2f%n", precision[i]);
			System.out.printf("Average Recall: %.2f%n", recall[i]);
			System.out.printf("Average Time (s): %.2f%n", time[i]);
		}

		double avgAvgTime = 0;
		for (int i = 0; i <= difficultyLevels; i++)
			avgAvgTime += time[i];
		avgAvgTime /= difficultyLevels;
		System.out.printf("\nAverage Average Time (s): %.2f%n", avgAvgTime);

		// Plot precision and recall
		final Plot2DPanel plot = new Plot2DPanel();
		plot.addLinePlot("Precision", Color.red, levels, precision);
		plot.addLinePlot("Recall", Color.blue, levels, recall);
		plot.setAxisLabels("difficulty", "precision/recall");
		plot.addLegend("SOUTH");
		plot.setFixedBounds(0, 0, difficultyLevels);
		plot.setFixedBounds(1, 0, 1);

		// Display
		final JFrame frame = new JFrame("Results");
		frame.setSize(1600, 1600);
		frame.setContentPane(plot);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Save
		final BufferedImage image = new BufferedImage(frame.getWidth(),
				frame.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics2D = image.createGraphics();
		frame.paint(graphics2D);
		ImageIO.write(image, "jpeg", new File(plotDir + "/" + name + ".jpg"));

		// // Plot time
		// final Plot2DPanel plot2 = new Plot2DPanel();
		// plot2.addScatterPlot("", Color.blue, levels, time);
		// plot2.setAxisLabels("levels", "time (s)");
		//
		// // Display
		// final JFrame frame2 = new JFrame("Results");
		// frame2.setSize(1600, 1600);
		// frame2.setContentPane(plot2);
		// frame2.setVisible(true);
		// frame2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

	}
}
