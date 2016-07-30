package apimining.pam.transaction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.LineIterator;

import apimining.pam.sequence.Sequence;

public class TransactionGenerator {

	private static final boolean VERBOSE = false;

	/**
	 * Generate transactions from set of interesting sequences
	 *
	 * @return set of sequences added to transaction
	 */
	public static HashMap<Sequence, Double> generateTransactionDatabase(
			final Map<Sequence, Double> sequences, final int noTransactions,
			final File outFile) throws IOException {

		// Set random number seeds
		final Random random = new Random(1);
		final Random randomI = new Random(10);

		// Storage for sequences actually added
		final HashMap<Sequence, Double> addedSequences = new HashMap<>();

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		int count = 0;
		while (count < noTransactions) {

			// Generate transaction from distribution
			final Transaction transaction = sampleFromDistribution(random,
					sequences, addedSequences, randomI);
			for (final int item : transaction) {
				out.print(item + " -1 ");
			}
			if (!transaction.isEmpty()) {
				out.print("-2");
				out.println();
				count++;
			}

		}
		out.close();

		// Print file to screen
		if (VERBOSE) {
			final FileReader reader = new FileReader(outFile);
			final LineIterator it = new LineIterator(reader);
			while (it.hasNext()) {
				System.out.println(it.nextLine());
			}
			LineIterator.closeQuietly(it);
		}

		return addedSequences;
	}

	/**
	 * Randomly generate sequence with its probability, randomly interleaving
	 * subsequences
	 */
	public static Transaction sampleFromDistribution(final Random random,
			final Map<Sequence, Double> sequences,
			final HashMap<Sequence, Double> addedSequences, final Random randomI) {

		final ArrayList<Integer> transaction = new ArrayList<>();
		for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
			if (random.nextDouble() < entry.getValue()) {
				interleave(transaction, entry.getKey(), randomI);
				addedSequences.put(entry.getKey(), entry.getValue());
			}
		}

		return new Transaction(transaction);
	}

	/** Randomly interleave sequence into transaction */
	private static void interleave(final ArrayList<Integer> transaction,
			final Sequence seq, final Random randomI) {
		if (transaction.size() == 0) {
			transaction.addAll(seq);
		} else {
			int prev = 0;
			for (final Integer item : seq) {
				final int insertionPoint = randomI
						.nextInt((transaction.size() - prev) + 1) + prev;
				transaction.add(insertionPoint, item);
				prev = insertionPoint + 1;
			}
		}
	}

}
