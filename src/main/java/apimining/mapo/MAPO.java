package apimining.mapo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

import apimining.fsminer.FrequentSequenceMiner;
import apimining.fsminer.Sequence;

public class MAPO {

	public static void main(final String[] args) throws Exception {

		final String project = "andengine";
		final String arffFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/train/calls/"
				+ project + ".arff";
		final String outFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/train/"
				+ project + "/mapo/";
		mineAPICallSequences(arffFile, outFolder, 0.2);

	}

	/**
	 * Mine API call sequences using MAPO
	 *
	 * @param arffFile
	 *            API calls in ARF Format. Attributes are fqCaller and fqCalls
	 *            as space separated string of API calls.
	 */
	public static void mineAPICallSequences(final String arffFile, final String outFolder, final double threshold)
			throws Exception {

		new File(outFolder).mkdirs();

		System.out.print("===== Clustering call sequences... ");
		final Multimap<Integer, String> clusteredCallSeqs = APICallClustererMAPO.clusterAPICallSeqs(arffFile,
				threshold);
		System.out.println("done. Number of clusters: " + clusteredCallSeqs.keySet().size());

		double minSupp = 0.1;
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

			int count = 0;
			for (final Collection<String> callSeqs : clusteredCallSeqs.asMap().values()) {

				System.out.println("+++++ Processing cluster #" + count);

				System.out.print("  Creating temporary transaction DB... ");
				final File transactionDB = File.createTempFile("APICallDB", ".txt");
				final BiMap<String, Integer> dictionary = HashBiMap.create();
				generateTransactionDatabase(callSeqs, dictionary, transactionDB);
				System.out.println("done.");

				System.out.print("  Mining frequent sequences... ");
				final File freqSeqs = File.createTempFile("APICallSeqs", ".txt");
				FrequentSequenceMiner.mineFrequentSequencesSPAM(transactionDB.getAbsolutePath(),
						freqSeqs.getAbsolutePath(), minSupp);
				transactionDB.delete();
				System.out.println("done.");

				final File outFile = new File(outFolder + "/Cluster" + count + "FreqCallSeqs.txt");
				decodeFrequentSequences(freqSeqs, dictionary, outFile);
				freqSeqs.delete();

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

	private static void decodeFrequentSequences(final File seqFile, final BiMap<String, Integer> dictionary,
			final File outFile) throws IOException {

		final SortedMap<Sequence, Integer> freqSeqs = FrequentSequenceMiner.readFrequentSequences(seqFile);

		final PrintWriter out = new PrintWriter(outFile);
		for (final Entry<Sequence, Integer> entry : freqSeqs.entrySet()) {
			out.println("supp: " + entry.getValue());
			for (final int item : entry.getKey())
				out.print(dictionary.inverse().get(item) + " ");
			out.println();
			out.println();
		}
		out.close();

	}

}
