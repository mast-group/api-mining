package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.TransactionGenerator;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.math.plot.Plot2DPanel;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;

public class ItemsetEvaluation {

	private static final String name = "caviar";
	private static final File dbFile = new File("/home/jari/caviar.txt");

	public static void main(final String[] args) throws IOException {

		final List<Integer> steps = Lists.newArrayList();
		for (int i = 0; i <= 10; i++)
			steps.add(i);

		final int noTransactions = 100;

		final List<Double> precision = Lists.newArrayList();
		final List<Double> recall = Lists.newArrayList();
		for (final int noExtraElems : steps) {

			// Generate real itemsets
			final HashMap<Itemset, Double> actualItemsets = TransactionGenerator
					.generateItemsets(name, noExtraElems);

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(actualItemsets,
					noTransactions, dbFile);

			// Mine itemsets
			final HashMap<Itemset, Double> minedItemsets = ItemsetMining
					.mineItemsets(dbFile);

			// Calculate precision and recall
			final double noInBoth = Sets.intersection(actualItemsets.keySet(),
					minedItemsets.keySet()).size();
			final double pr = noInBoth / (double) minedItemsets.size();
			final double rec = noInBoth / (double) actualItemsets.size();
			precision.add(pr);
			recall.add(rec);
		}

		// Display precision and recall
		for (int i = 0; i < steps.size(); i++) {
			System.out.println("\n=========");
			System.out.println("no. Transactions: " + steps.get(i));
			System.out.println("Precision: " + precision.get(i));
			System.out.println("Recall: " + recall.get(i));
		}

		// Plot precision and recall
		final Plot2DPanel plot = new Plot2DPanel();
		plot.addScatterPlot("", Color.red, Doubles.toArray(recall),
				Doubles.toArray(precision));
		plot.setAxisLabels("recall", "precision");
		plot.setFixedBounds(0, 0, 1);
		plot.setFixedBounds(1, 0, 1);

		// Display
		final JFrame frame = new JFrame("Results");
		frame.setSize(800, 800);
		frame.setContentPane(plot);
		frame.setVisible(true);

	}
}
