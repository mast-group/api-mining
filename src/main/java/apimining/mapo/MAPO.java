package apimining.mapo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

public class MAPO {

	public static void main(final String[] args) throws Exception {

		final String arffFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/srclibs/calls/hadoop.arff";
		final String outFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/srclibs/netty/hadoop/";
		mineAPICallSequences(arffFile, outFolder, 10, 0.01);

	}

	/**
	 * Mine API call sequences using MAPO
	 *
	 * @param arffFile
	 *            API calls in ARF Format. Attributes are fqCaller and fqCalls
	 *            as space separated string of API calls.
	 */
	public static void mineAPICallSequences(final String arffFile, final String outFolder, final int noClusters,
			final double minSupp) throws Exception {

		final Multimap<Integer, String> clusteredCallSeqs = APICallClusterer.clusterAPICallSeqs(arffFile, noClusters);

		int count = 0;
		for (final Collection<String> callSeqs : clusteredCallSeqs.asMap().values()) {

			final File transactionDB = File.createTempFile("APICallDB", ".txt");
			final BiMap<String, Integer> dictionary = HashBiMap.create();
			generateTransactionDatabase(callSeqs, dictionary, transactionDB);

			final File freqSeqs = File.createTempFile("APICallSeqs", ".txt");
			FrequentSequenceMining.mineFrequentClosedSequencesBIDE(transactionDB.getAbsolutePath(),
					freqSeqs.getAbsolutePath(), minSupp);

			final File outFile = new File(outFolder + "/Cluster" + count + "FreqCallSeqs.txt");
			decodeFrequentSequences(freqSeqs, dictionary, outFile);

			count++;
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

		final SortedMap<Sequence, Integer> freqSeqs = FrequentSequenceMining.readFrequentSequences(seqFile);

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
