package itemsetmining.transaction;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TransactionGenerator {

	private static HashMap<Itemset, Double> itemsets = Maps.newHashMap();

	public static void main(final String[] args) throws IOException {

		if (args.length != 3) {
			System.err
					.println("Usage <problemName> <noTransactions> <noisePercentage>");
			System.exit(-1);
		}

		final int noTransactions = Integer.parseInt(args[1]);
		final double noisePercent = Double.parseDouble(args[2]);

		generateDatabase(args[0], noTransactions, noisePercent,
				"src/main/resources/");

	}

	/** Create transactions using interesting itemsets that highlight problems */
	public static void generateDatabase(final String name,
			final int noTransactions, final double noisePercent,
			final String outDir) throws IOException {

		// Here [1 2] is the champagne & caviar problem
		// (not generated when support is too high)
		if (name.equals("caviar")) {

			// Champagne & Caviar
			final Itemset s12 = new Itemset(1, 2);
			final double p12 = 0.1;
			itemsets.put(s12, p12);

			// Other transactions
			final Itemset s3 = new Itemset(3);
			final double p3 = 0.8;
			itemsets.put(s3, p3);

			final Itemset s4 = new Itemset(4);
			final double p4 = 0.5;
			itemsets.put(s4, p4);

		}
		// Here [1 2 3] would be seen as a frequent itemset
		// as both [1 2] and [3] are frequent
		else if (name.equals("freerider")) {

			final Itemset s12 = new Itemset(1, 2);
			final Itemset s3 = new Itemset(3);
			final double p12 = 0.5;
			final double p3 = 0.5;
			itemsets.put(s12, p12);
			itemsets.put(s3, p3);

		}
		// Here [1 2 3] is known as a cross-support pattern
		// (spuriously generated when support is too low)
		else if (name.equals("cross-supp")) {

			final Itemset s1 = new Itemset(1);
			final double p1 = 0.95;
			itemsets.put(s1, p1);

			final Itemset s2 = new Itemset(2, 3);
			final double p2 = 0.2;
			itemsets.put(s2, p2);

		}
		// This one probably doesn't make sense
		else if (name.equals("overlap")) {

			final Itemset s1 = new Itemset(2, 3, 5);
			final double p1 = 0.5;
			itemsets.put(s1, p1);

			final Itemset s2 = new Itemset(2, 5);
			final double p2 = 0.5;
			itemsets.put(s2, p2);

		} else
			throw new IllegalArgumentException("Incorrect problem name.");

		// Add more itemsets if large scale
		for (int i = 10; i < 10 + noisePercent * noTransactions; i++) {
			itemsets.put(new Itemset(i), 0.5);
		}

		// Set output file
		final File outFile = new File(outDir + name + ".txt");
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		for (int i = 0; i < noTransactions; i++) {

			// Generate transaction from distribution
			final Set<Integer> transaction = sampleFromDistribution();
			for (final int item : transaction) {
				out.print(item + " ");
			}
			if (!transaction.isEmpty())
				out.println();

		}
		out.close();

		// Print file to screen
		final FileReader reader = new FileReader(outFile);
		final LineIterator it = new LineIterator(reader);
		while (it.hasNext()) {
			System.out.println(it.nextLine());
		}
		LineIterator.closeQuietly(it);
	}

	/** Randomly generate itemset with its probability */
	private static Set<Integer> sampleFromDistribution() {

		final Set<Integer> transaction = Sets.newHashSet();
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			if (Math.random() < entry.getValue()) {
				transaction.addAll(entry.getKey().getItems());
			}
		}

		return transaction;
	}
}
