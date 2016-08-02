package apimining.pam.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import apimining.pam.main.InferenceAlgorithms.InferGreedy;
import apimining.pam.main.InferenceAlgorithms.InferenceAlgorithm;
import apimining.pam.sequence.Sequence;
import apimining.pam.util.Logging;

public class PAM extends PAMCore {

	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "ARFF file with call sequences")
		String arffFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/calls/hadoop.arff";

		@Parameter(names = { "-o", "--outFile" }, description = "Output File")
		String outFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/hadoop/PAM_seqs.txt";

		@Parameter(names = { "-s", "--maxSteps" }, description = "Max structure steps")
		int maxStructureSteps = 100_000;

		@Parameter(names = { "-i", "--iterations" }, description = "Max iterations")
		int maxEMIterations = 10_000;

		@Parameter(names = { "-l", "--log-level" }, description = "Log level", converter = LogLevelConverter.class)
		Level logLevel = Level.FINE;

		@Parameter(names = { "-r", "--runtime" }, description = "Max Runtime (min)")
		long maxRunTime = 72 * 60; // 12hrs

		@Parameter(names = { "-t", "--timestamp" }, description = "Timestamp Logfile", arity = 1)
		boolean timestampLog = true;

		@Parameter(names = { "-v", "--verbose" }, description = "Log to console instead of logfile")
		boolean verbose = false;
	}

	public static void main(final String[] args) throws Exception {

		// Main fixed parameters
		final InferenceAlgorithm inferenceAlg = new InferGreedy();

		// Runtime parameters
		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);

		try {
			jc.parse(args);

			// Set loglevel, runtime, timestamp and log file
			LOG_LEVEL = params.logLevel;
			MAX_RUNTIME = params.maxRunTime * 60 * 1_000;
			File logFile = null;
			if (!params.verbose)
				logFile = Logging.getLogFileName("ISM", params.timestampLog, LOG_DIR, params.arffFile);

			// Mine interesting API call sequences
			System.out.println("Processing " + FilenameUtils.getBaseName(params.arffFile) + "...");
			mineAPICallSequences(params.arffFile, params.outFile, inferenceAlg, params.maxStructureSteps,
					params.maxEMIterations, logFile);

		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}

	}

	/**
	 * Mine API call sequences using PAM
	 *
	 * @param arffFile
	 *            API calls in ARF Format. Attributes are fqCaller and fqCalls
	 *            as space separated string of API calls.
	 */
	public static void mineAPICallSequences(final String arffFile, final String outFile,
			final InferenceAlgorithm inferenceAlgorithm, final int maxStructureSteps, final int maxEMIterations,
			final File logFile) throws Exception {

		final File fout = new File(outFile);
		if (fout.getParentFile() != null)
			fout.getParentFile().mkdirs();

		System.out.print("  Creating temporary transaction DB... ");
		final File transactionDB = File.createTempFile("APICallDB", ".txt");
		final BiMap<String, Integer> dictionary = HashBiMap.create();
		generateTransactionDatabase(arffFile, dictionary, transactionDB);
		System.out.println("done.");

		System.out.print("  Mining interesting sequences... ");
		final Map<Sequence, Double> sequences = PAMCore.mineInterestingSequences(transactionDB, inferenceAlgorithm,
				maxStructureSteps, maxEMIterations, logFile);
		transactionDB.delete();
		System.out.println("done.");

		decodeInterestingSequences(sequences, dictionary, outFile);
	}

	private static void generateTransactionDatabase(final String arffFile, final BiMap<String, Integer> dictionary,
			final File transactionDB) throws IOException {

		int mID = 0;
		boolean found = false;
		final PrintWriter out = new PrintWriter(transactionDB);
		final LineIterator it = FileUtils.lineIterator(new File(arffFile));
		while (it.hasNext()) {
			final String line = it.nextLine();

			if (found) {
				for (final String raw_call : line.split(",")[1].replace("\'", "").split(" ")) {
					final String call = raw_call.trim();
					if (call.isEmpty()) // skip empty strings
						continue;
					if (dictionary.containsKey(call)) {
						final int ID = dictionary.get(call);
						out.print(ID + " -1 ");
					} else {
						out.print(mID + " -1 ");
						dictionary.put(call, mID);
						mID++;
					}
				}
				out.println("-2");
			}

			if (line.contains("@data"))
				found = true;

		}
		it.close();
		out.close();
	}

	private static void decodeInterestingSequences(final Map<Sequence, Double> sequences,
			final BiMap<String, Integer> dictionary, final String outFile) throws IOException {

		final PrintWriter out = new PrintWriter(outFile);
		for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
			out.println(String.format("prob: %1.5f", entry.getValue()));
			out.print("[");
			String prefix = "";
			for (final int item : entry.getKey()) {
				out.print(prefix + dictionary.inverse().get(item));
				prefix = ", ";
			}
			out.print("]");
			out.println();
			out.println();
		}
		out.close();

	}

	/** Convert string level to level class */
	public static class LogLevelConverter implements IStringConverter<Level> {
		@Override
		public Level convert(final String value) {
			if (value.equals("SEVERE"))
				return Level.SEVERE;
			else if (value.equals("WARNING"))
				return Level.WARNING;
			else if (value.equals("INFO"))
				return Level.INFO;
			else if (value.equals("CONFIG"))
				return Level.CONFIG;
			else if (value.equals("FINE"))
				return Level.FINE;
			else if (value.equals("FINER"))
				return Level.FINER;
			else if (value.equals("FINEST"))
				return Level.FINEST;
			else
				throw new RuntimeException("Incorrect Log Level.");
		}
	}

}
