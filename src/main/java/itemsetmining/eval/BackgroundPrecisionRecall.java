package itemsetmining.eval;

import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.sequence.Sequence;
import itemsetmining.main.SequenceMining;
import itemsetmining.main.SequenceMiningCore;
import itemsetmining.util.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class BackgroundPrecisionRecall {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/sequence.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** FSM Issues to incorporate */
	private static final String name = "Background";
	private static final int noIterations = 5_000;
	private static final int maxStructureSteps = 100_000;

	/** Previously mined Sequences to use for background distribution */
	private static final File sequenceLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Logs/ISM-SIGN-27.05.2015-15:12:45.log");
	private static final int noTransactions = 10_000;

	/** FSM Settings */
	private static final double minSup = 0.05;

	public static void main(final String[] args) throws IOException {

		// Read in background distribution
		final Map<Sequence, Double> sequences = SequenceMiningCore
				.readISMSequences(sequenceLog);

		// final HashMap<Sequence, Double> sequences = TransactionGenerator
		// .generateTransactionDatabase(backgroundSequences,
		// noTransactions, dbFile);
		// System.out.print("\n============= ACTUAL SEQUENCES =============\n");
		// for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
		// System.out.print(String.format("%s\tprob: %1.5f %n",
		// entry.getKey(), entry.getValue()));
		// }
		System.out.println("\nNo itemsets: " + sequences.size());
		printTransactionDBStats(dbFile);

		precisionRecall(sequences, "ISM");
		// precisionRecall(sequences, "FSM");

	}

	public static void precisionRecall(final Map<Sequence, Double> itemsets,
			final String algorithm) throws IOException {

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ algorithm + "_Markov_" + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Mine itemsets
		Set<Sequence> minedSequences = null;
		final File logFile = Logging.getLogFileName(algorithm, true, saveDir,
				dbFile);
		final long startTime = System.currentTimeMillis();
		if (algorithm.equals("FSM")) {
			// FrequentItemsetMining
			// .mineFrequentSequencesPrefixSpan(dbFile.getAbsolutePath(),
			// logFile.getAbsolutePath(), minSup);
			minedSequences = FrequentSequenceMining
					.readFrequentSequences(
							new File(
									"/disk/data1/jfowkes/logs/FSM-sequence-28.05.2015-10:16:27.log"))
					.keySet();
		} else if (algorithm.equals("ISM")) {
			final Map<Sequence, Double> minedIntSeqs = SequenceMining
					.mineSequences(dbFile, new InferGreedy(),
							maxStructureSteps, noIterations, logFile);
			// final Map<Sequence, Double> minedIntSeqs = ItemsetMining
			// .readISMSequences(new File(
			// "/disk/data1/jfowkes/logs/ISM-sequence-27.05.2015-17:06:40.log"));
			final Ordering<Sequence> comparator = Ordering.natural().reverse()
					.onResultOf(Functions.forMap(minedIntSeqs))
					.compound(Ordering.usingToString());
			minedSequences = ImmutableSortedMap
					.copyOf(minedIntSeqs, comparator).keySet();
		} else
			throw new RuntimeException("Incorrect algorithm name.");
		final long endTime = System.currentTimeMillis();
		final double time = (endTime - startTime) / (double) 1000;

		// Calculate sorted precision and recall
		final int len = minedSequences.size();
		System.out.println("No. mined itemsets: " + len);
		final double[] precision = new double[len];
		final double[] recall = new double[len];
		for (int k = 1; k <= minedSequences.size(); k++) {

			final Set<Sequence> topKMined = Sets.newHashSet();
			for (final Sequence set : minedSequences) {
				topKMined.add(set);
				if (topKMined.size() == k)
					break;
			}

			final double noInBoth = Sets.intersection(itemsets.keySet(),
					topKMined).size();
			final double pr = noInBoth / (double) topKMined.size();
			final double rec = noInBoth / (double) itemsets.size();
			precision[k - 1] = pr;
			recall[k - 1] = rec;
		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Time: " + time);
		System.out.println("Precision (all): " + Arrays.toString(precision));
		System.out.println("Recall (special): " + Arrays.toString(recall));

	}

	/** Print useful statistics for the transaction database */
	public static void printTransactionDBStats(final File dbFile)
			throws IOException {

		int noTransactions = 0;
		double sparsity = 0;
		final Set<Integer> singletons = new HashSet<>();
		final LineIterator it = FileUtils.lineIterator(dbFile, "UTF-8");
		while (it.hasNext()) {
			final String[] items = it.nextLine().replace("-2", "")
					.split(" -1 ");
			for (final String item : items)
				singletons.add(Integer.parseInt(item));
			sparsity += items.length;
			noTransactions++;
		}
		LineIterator.closeQuietly(it);

		System.out.println("\nDatabase: " + dbFile);
		System.out.println("Items: " + singletons.size());
		System.out.println("Transactions: " + noTransactions);
		System.out.println("Avg. items per transaction: " + sparsity
				/ noTransactions + "\n");

	}

}
