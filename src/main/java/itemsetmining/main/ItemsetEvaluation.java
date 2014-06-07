package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.main.InferenceAlgorithms.inferGreedy;
import itemsetmining.transaction.TransactionGenerator;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JFrame;

import org.math.plot.Plot2DPanel;

import com.google.common.collect.Sets;

public class ItemsetEvaluation {

	private static final String name = "caviar";
	private static final File dbFile = new File(
			"/disk/data2/jfowkes/Transactions/caviar.txt");
	private static final InferenceAlgorithm inferenceAlg = new inferGreedy();

	private static final int noSamples = 10;
	private static final int difficultyLevels = 10;

	private static final int noTransactions = 1000;
	private static final int noExtraSets = 5;
	private static final int maxSetSize = 3;

	private static final int maxRandomWalks = 1000;
	private static final int maxStructureIterations = 100;

	public static void main(final String[] args) throws IOException {

		final double[] levels = new double[difficultyLevels];
		final double[] time = new double[difficultyLevels];
		final double[] precision = new double[difficultyLevels];
		final double[] recall = new double[difficultyLevels];

		for (int sample = 0; sample < noSamples; sample++) {
			System.out.println("\n========= Sample: " + (sample + 1) + " of "
					+ noSamples);
			for (int level = 0; level < difficultyLevels; level++) {

				// Generate real itemsets
				final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
						.generateItemsets(name, level, noExtraSets, maxSetSize);

				// Generate transaction database
				TransactionGenerator.generateTransactionDatabase(
						actualItemsets, noTransactions, dbFile);

				// Mine itemsets
				final long startTime = System.currentTimeMillis();
				final HashMap<Itemset, Double> minedItemsets = ItemsetMining
						.mineItemsets(dbFile, inferenceAlg, maxRandomWalks,
								maxStructureIterations);
				final long endTime = System.currentTimeMillis();
				time[level] += (endTime - startTime) / (double) 1000;

				// Calculate precision and recall
				final double noInBoth = Sets.intersection(
						actualItemsets.keySet(), minedItemsets.keySet()).size();
				final double pr = noInBoth / (double) minedItemsets.size();
				final double rec = noInBoth / (double) actualItemsets.size();
				precision[level] += pr;
				recall[level] += rec;
			}
		}

		for (int i = 0; i < difficultyLevels; i++) {

			// Average over samples
			precision[i] /= noSamples;
			recall[i] /= noSamples;
			time[i] /= noSamples;
			levels[i] = i;

			// Display precision and recall
			System.out.println("\n========= Difficulty Level: " + i);
			System.out.println("Average Precision: " + precision[i]);
			System.out.println("Average Precision: " + precision[i]);
			System.out.println("Average Time: (s)" + time[i]);
		}

		// Plot precision and recall
		final Plot2DPanel plot = new Plot2DPanel();
		plot.addScatterPlot("", Color.red, recall, precision);
		plot.setAxisLabels("recall", "precision");
		plot.setFixedBounds(0, 0, 1);
		plot.setFixedBounds(1, 0, 1);

		// Display
		final JFrame frame = new JFrame("Results");
		frame.setSize(800, 800);
		frame.setContentPane(plot);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Plot time
		final Plot2DPanel plot2 = new Plot2DPanel();
		plot2.addScatterPlot("", Color.blue, levels, time);
		plot2.setAxisLabels("levels", "time (s)");

		// Display
		final JFrame frame2 = new JFrame("Results");
		frame2.setSize(800, 800);
		frame2.setContentPane(plot2);
		frame2.setVisible(true);
		frame2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

	}
}
