package itemsetmining.util;

import itemsetmining.main.SequenceMining;
import itemsetmining.main.SequenceMiningCore;
import itemsetmining.sequence.Sequence;
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

/**
 * Read last EM step of partial sequence log and output interesting sequences
 * along with interestingness and probability and write to end of log file.
 */
public class PartialLogFixer {

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage <transactionDB> <logFile>");
			System.exit(-1);
		}

		System.out.println("Reading sequences from last parameter EM step for "
				+ args[1] + "...");
		final HashMap<Sequence, Double> itemsets = readLastEMStepSequences(new File(
				args[1]));
		System.out.println("done. Number of sequences: " + itemsets.size());

		System.out.println("\nWriting sorted sequences to " + args[1] + "...");
		sortSequencesInterestingness(itemsets, new File(args[0]), new File(
				args[1]));
		System.out.println("All done. Exiting.");

	}

	public static HashMap<Sequence, Double> readLastEMStepSequences(
			final File logFile) throws IOException {
		final HashMap<Sequence, Double> sequences = new HashMap<>();

		final ReversedLinesFileReader reader = new ReversedLinesFileReader(
				logFile);
		String line = reader.readLine();
		while (line != null) {

			if (line.contains("Parameter Optimal Sequences:")) {
				final Matcher m = Pattern
						.compile(
								"\\[((?:[0-9]|,| )+?)\\](?:\\^\\(([0-9]+)\\))?=([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)")
						.matcher(line);
				while (m.find()) {
					final Sequence sequence = new Sequence();
					final String[] items = m.group(1).split(", ");
					for (final String item : items)
						sequence.add(Integer.parseInt(item));
					if (m.group(2) != null) { // has occurrence
						for (int i = 0; i < Integer.parseInt(m.group(2)) - 1; i++)
							sequence.incrementOccurence();
					}
					final double prob = Double.parseDouble(m.group(3));
					sequences.put(sequence, prob);
				}
				break;
			}
			line = reader.readLine();

		}
		reader.close();

		return sequences;
	}

	public static void sortSequencesInterestingness(
			final HashMap<Sequence, Double> sequences,
			final File transactionDB, final File logFile) throws IOException {

		// Read in transaction database
		final TransactionList transactions = SequenceMining
				.readTransactions(transactionDB);

		// Sort sequences by interestingness
		System.out.println("Sorting sequences by interestingness...");
		final HashMap<Sequence, Double> intMap = SequenceMiningCore
				.calculateInterestingness(sequences, transactions);
		final Map<Sequence, Double> sortedSequences = SequenceMiningCore
				.sortSequences(sequences, intMap);

		System.out.println("Writing out to file...");
		final FileWriter out = new FileWriter(logFile, true);
		out.write("\n============= INTERESTING SEQUENCES =============\n");
		for (final Entry<Sequence, Double> entry : sortedSequences.entrySet()) {
			out.write(String.format("%s\tprob: %1.5f \tint: %1.5f %n",
					entry.getKey(), entry.getValue(),
					intMap.get(entry.getKey())));
		}
		out.write("\n");
		out.close();
		System.out.println("done.");

	}

}
