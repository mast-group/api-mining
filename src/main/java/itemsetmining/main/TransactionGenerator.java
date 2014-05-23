package itemsetmining.main;

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

		if (args.length != 2) {
			System.err.println("Usage <problemName> <noTransactions>");
			System.exit(-1);
		}

		// Create interesting itemsets
		if (args[0].equals("caviar")) {

		} else if (args[0].equals("freerider")) {

			Itemset s12 = new Itemset(1, 2);
			Itemset s3 = new Itemset(3);
			double p12 = 0.25;
			double p3 = 0.25;
			itemsets.put(s12, p12);
			itemsets.put(s3, p3);

		} else if (args[0].equals("unlifted")) {

		} else
			throw new IllegalArgumentException("Incorrect problem name.");

		// Set output file
		final File outFile = new File("src/main/resources/" + args[0] + ".txt");
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		int noTransactions = Integer.parseInt(args[1]);
		for (int i = 0; i < noTransactions; i++) {

			// Generate transaction from distribution
			Set<Integer> transaction = sampleFromDistribution();
			for (int item : transaction) {
				out.print(item + " ");
			}
			if (!transaction.isEmpty())
				out.println();

		}
		out.close();

		// Print file to screen
		FileReader reader = new FileReader(outFile);
		LineIterator it = new LineIterator(reader);
		while (it.hasNext()) {
			System.out.println(it.nextLine());
		}
		LineIterator.closeQuietly(it);
	}

	/** Randomly generate itemset with its probability */
	private static Set<Integer> sampleFromDistribution() {

		Set<Integer> transaction = Sets.newHashSet();
		for (Entry<Itemset, Double> entry : itemsets.entrySet()) {
			if (Math.random() > entry.getValue()) {
				transaction.addAll(entry.getKey().getItems());
			}
		}

		return transaction;
	}
}
