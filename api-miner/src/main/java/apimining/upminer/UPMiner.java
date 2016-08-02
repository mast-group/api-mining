package apimining.upminer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.io.FilenameUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

import apimining.fsminer.FrequentSequenceMiner;
import apimining.fsminer.Sequence;

public class UPMiner {

	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "ARFF file with call sequences")
		String arffFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/calls/hadoop.arff";

		@Parameter(names = { "-o", "--outFolder" }, description = "Output Folder")
		String outFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/hadoop/upminer/";

		@Parameter(names = { "-s", "--support" }, description = "Minimum support threshold")
		double minSupp = 0.3;

	}

	public static void main(final String[] args) throws Exception {

		// Runtime parameters
		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);

		try {
			jc.parse(args);

			// Mine project
			System.out.println("Processing " + FilenameUtils.getBaseName(params.arffFile) + "...");
			mineAPICallSequences(params.arffFile, params.outFolder, 0.2, 0.2, params.minSupp);

		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}

	}

	/**
	 * Mine API call sequences using UP-Miner
	 *
	 * @param arffFile
	 *            API calls in ARF Format. Attributes are fqCaller and fqCalls
	 *            as space separated string of API calls.
	 */
	public static void mineAPICallSequences(final String arffFile, final String outFolder, final double threshold1,
			final double threshold2, double minSupp) throws Exception {

		new File(outFolder).mkdirs();

		System.out.print("===== Clustering call sequences #1... ");
		final Multimap<Integer, String> clusteredCallSeqs1 = APICallClustererUPMiner.clusterAPICallSeqs(arffFile,
				threshold1);
		System.out.println("done. Number of clusters: " + clusteredCallSeqs1.keySet().size());

		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {

			System.out.print("Enter minSupp:");
			try {
				minSupp = Double.parseDouble(br.readLine());
			} catch (final NumberFormatException e) {
				System.err.println("Invalid Format. Using " + minSupp);
			}
			if (minSupp <= 0)
				break;

			final File arffFileFreq = File.createTempFile("FreqCalls", ".arff");
			writeArffHeader(arffFileFreq);

			int count = 0;
			for (final Collection<String> callSeqs : clusteredCallSeqs1.asMap().values()) {

				System.out.println("+++++ Processing cluster #" + count);

				System.out.print("  Creating temporary transaction DB... ");
				final File transactionDB = File.createTempFile("APICallDB", ".txt");
				final BiMap<String, Integer> dictionary = HashBiMap.create();
				generateTransactionDatabase(callSeqs, dictionary, transactionDB);
				System.out.println("done.");

				System.out.print("  Mining frequent sequences... ");
				final File freqSeqs = File.createTempFile("APICallSeqs", ".txt");
				FrequentSequenceMiner.mineFrequentClosedSequencesBIDE(transactionDB.getAbsolutePath(),
						freqSeqs.getAbsolutePath(), minSupp);
				transactionDB.delete();
				System.out.println("done.");

				saveFrequentSequencesArffFile(freqSeqs, dictionary, arffFileFreq);
				freqSeqs.delete();

				count++;
			}

			System.out.print("===== Clustering call sequences #2... ");
			final Multimap<Integer, String> clusteredCallSeqs2 = APICallClustererUPMiner
					.clusterAPICallSeqs(arffFileFreq.getAbsolutePath(), threshold2);
			arffFileFreq.delete();
			System.out.println("done. Number of clusters: " + clusteredCallSeqs2.keySet().size());

			count = 0;
			for (final Collection<String> callSeqs : clusteredCallSeqs2.asMap().values()) {
				final File outFile = new File(outFolder + "/Cluster" + count + "FreqCallSeqs.txt");
				writeClusteredSequences(callSeqs, outFile);
				count++;
			}
		}

	}

	private static void generateTransactionDatabase(final Collection<String> callSeqs,
			final BiMap<String, Integer> dictionary, final File transactionDB) throws IOException {

		final PrintWriter out = new PrintWriter(transactionDB);

		int mID = 0;
		for (final String callSeq : callSeqs) {
			for (final String call : callSeq.split(" ")) {
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
		out.close();
	}

	private static void writeArffHeader(final File arffFile) throws IOException {
		final PrintWriter out = new PrintWriter(arffFile, "UTF-8");
		out.println("@relation TEMP");
		out.println();
		out.println("@attribute fqCaller string");
		out.println("@attribute fqCalls string");
		out.println();
		out.println("@data");
		out.close();
	}

	private static void saveFrequentSequencesArffFile(final File seqFile, final BiMap<String, Integer> dictionary,
			final File arffFile) throws IOException {

		final SortedMap<Sequence, Integer> freqSeqs = FrequentSequenceMiner.readFrequentSequences(seqFile);

		final PrintWriter out = new PrintWriter(new FileWriter(arffFile, true));
		int count = 0;
		for (final Entry<Sequence, Integer> entry : freqSeqs.entrySet()) {
			out.print("'unknown" + count + "','");
			String prefix = "";
			for (final int item : entry.getKey()) {
				out.print(prefix + dictionary.inverse().get(item));
				prefix = " ";
			}
			out.println("'");
			count++;
		}
		out.close();

	}

	private static void writeClusteredSequences(final Collection<String> callSeqs, final File outFile)
			throws IOException {
		final PrintWriter out = new PrintWriter(outFile);
		for (final String seq : callSeqs)
			out.println(seq);
		out.close();
	}

}
