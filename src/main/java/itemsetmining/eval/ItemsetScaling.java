package itemsetmining.eval;

import itemsetmining.itemset.Sequence;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;
import itemsetmining.transaction.TransactionGenerator;
import itemsetmining.util.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.output.TeeOutputStream;

public class ItemsetScaling {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/sequence.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** Set of mined itemsets to use for background */
	private static final String name = "SIGN-based";
	private static final File itemsetLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Logs/ISM-SIGN-27.05.2015-15:12:45.log");

	/** Spark Settings */
	private static final long MAX_RUNTIME = 6 * 60; // 6hrs
	private static final int maxStructureSteps = 100_000;
	private static final int maxEMIterations = 100;

	public static void main(final String[] args) throws IOException,
			InterruptedException {

		// Run
		scalingTransactions(64, new int[] { 1_000, 10_000, 100_000, 1_000_000,
				10_000_000, 100_000_000 });
	}

	public static void scalingTransactions(final int noCores, final int[] trans)
			throws IOException, InterruptedException {

		final double[] time = new double[trans.length];
		final DecimalFormat formatter = new DecimalFormat("0.0E0");

		// Save to file
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ name + "_scaling_" + noCores + ".txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Read in previously mined sequences
		final Map<Sequence, Double> sequences = ItemsetMiningCore
				.readISMSequences(itemsetLog);
		System.out.print("\n============= ACTUAL SEQUENCES =============\n");
		for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}
		System.out.println("\nNo sequences: " + sequences.size());

		transloop: for (int i = 0; i < trans.length; i++) {

			final int tran = trans[i];
			System.out.println("\n========= " + formatter.format(tran)
					+ " Transactions");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(sequences, tran,
					dbFile);
			BackgroundPrecisionRecall.printTransactionDBStats(dbFile);

			// Mine sequences
			final File logFile = Logging.getLogFileName("IIM", true, saveDir,
					dbFile);
			final long startTime = System.currentTimeMillis();
			ItemsetMining.mineSequences(dbFile, new InferGreedy(),
					maxStructureSteps, maxEMIterations, logFile);

			final long endTime = System.currentTimeMillis();
			final double tim = (endTime - startTime) / (double) 1000;
			time[i] += tim;

			System.out.printf("Time (s): %.2f%n", tim);

			if (tim > MAX_RUNTIME * 60)
				break transloop;

		}

		// Print time
		System.out.println("\n========" + name + "========");
		System.out.println("Transactions:" + Arrays.toString(trans));
		System.out.println("Time: " + Arrays.toString(time));

		// and save to file
		out.close();
	}

}
