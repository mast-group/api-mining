package itemsetmining.util;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;
import itemsetmining.transaction.TransactionList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/**
 * Read last EM step of partial itemset log and output interesting itemsets
 * along with interestingness and probability and write to end of log file.
 */
public class PartialLogFixer {

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage <transactionDB> <logFile>");
			System.exit(-1);
		}

		System.out.println("Reading itemsets from last parameter EM step for "
				+ args[1] + "...");
		final HashMap<Itemset, Double> itemsets = readLastEMStepItemsets(new File(
				args[1]));
		System.out.println("done. Number of itemsets: " + itemsets.size());

		System.out.println("\nWriting sorted itemsets to " + args[1] + "...");
		sortItemsetsInterestingness(itemsets, new File(args[0]), new File(
				args[1]));
		System.out.println("All done. Exiting.");

	}

	public static HashMap<Itemset, Double> readLastEMStepItemsets(
			final File logFile) throws IOException {
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();

		final ReversedLinesFileReader reader = new ReversedLinesFileReader(
				logFile);
		String line = reader.readLine();
		while (line != null) {

			if (line.contains("Parameter Optimal Itemsets:")) {
				final Matcher m = Pattern
						.compile(
								"\\{((?:[0-9]|,| )+?)\\}=([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)")
						.matcher(line);
				while (m.find()) {
					final Itemset itemset = new Itemset();
					final String[] items = m.group(1).split(", ");
					for (final String item : items)
						itemset.add(Integer.parseInt(item));
					final double prob = Double.parseDouble(m.group(2));
					itemsets.put(itemset, prob);
				}
				break;
			}
			line = reader.readLine();

		}
		reader.close();

		return itemsets;
	}

	public static void sortItemsetsInterestingness(
			final HashMap<Itemset, Double> itemsets, final File transactionDB,
			final File logFile) throws IOException {

		// Read in transaction database
		final TransactionList transactions = ItemsetMining
				.readTransactions(transactionDB);

		// Determine most frequent singletons
		final Multiset<Integer> singletons = ItemsetMining
				.scanDatabaseToDetermineFrequencyOfSingleItems(transactionDB);

		// Apply the algorithm to build the itemset tree
		System.out.println("Building itemset tree...");
		final ItemsetTree tree = new ItemsetTree(singletons);
		tree.buildTree(transactionDB);

		// Sort itemsets by interestingness
		System.out.println("Sorting itemsets by interestingness...");
		final HashMap<Itemset, Double> intMap = ItemsetMiningCore
				.calculateInterestingness(itemsets, transactions, tree);
		final Map<Itemset, Double> sortedItemsets = ItemsetMiningCore
				.sortItemsets(itemsets, intMap);

		System.out.println("Writing out to file...");
		final FileWriter out = new FileWriter(logFile, true);
		out.write("\n============= INTERESTING ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : sortedItemsets.entrySet()) {
			out.write(String.format("%s\tprob: %1.5f \tint: %1.5f %n",
					entry.getKey(), entry.getValue(),
					intMap.get(entry.getKey())));
		}
		out.write("\n");
		out.close();
		System.out.println("done.");

	}

}
