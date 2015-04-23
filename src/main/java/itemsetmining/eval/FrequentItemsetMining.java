package itemsetmining.eval;

import itemsetmining.itemset.Sequence;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoPrefixSpan;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPattern;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

public class FrequentItemsetMining {

	public static void main(final String[] args) throws IOException {

		// FIM parameters
		final String dataset = "GAZELLE1";
		final double minSupp = 0.002; // relative support
		final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/"
				+ dataset + ".txt";
		final String saveFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/FIM/"
				+ dataset + ".txt";

		mineFrequentSequencesPrefixSpan(dbPath, saveFile, minSupp);
		final SortedMap<Sequence, Integer> freqItemsets = readFrequentSequences(new File(
				saveFile));
		System.out.println("\nFSM Sequences");
		System.out.println("No sequences: " + freqItemsets.size());

	}

	/** Run PrefixSpan algorithm */
	public static SortedMap<Sequence, Integer> mineFrequentSequencesPrefixSpan(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final SequenceDatabase sequenceDatabase = new SequenceDatabase();
		sequenceDatabase.loadFile(dataset);

		final AlgoPrefixSpan algo = new AlgoPrefixSpan();
		algo.setShowSequenceIdentifiers(false);
		final SequentialPatterns patterns = algo.runAlgorithm(sequenceDatabase,
				minSupp, saveFile);
		// algo.printStatistics(sequenceDatabase.size());

		return toMap(patterns);
	}

	/** Convert frequent sequences to sorted Map<Sequence, Integer> */
	public static SortedMap<Sequence, Integer> toMap(
			final SequentialPatterns patterns) {
		if (patterns == null) {
			return null;
		} else {
			final HashMap<Sequence, Integer> sequences = Maps.newHashMap();
			for (final List<SequentialPattern> level : patterns.levels) {
				for (final SequentialPattern pattern : level) {
					final Sequence seq = new Sequence();
					for (final Itemset set : pattern.getItemsets())
						seq.add(set.get(0)); // Assumes a seq is just singleton
												// itemsets
					sequences.put(seq, pattern.getAbsoluteSupport());
				}
			}
			// Sort patterns by support
			final Ordering<Sequence> comparator = Ordering.natural().reverse()
					.onResultOf(Functions.forMap(sequences))
					.compound(Ordering.usingToString());
			return ImmutableSortedMap.copyOf(sequences, comparator);
		}
	}

	/** Read in frequent sequences */
	public static SortedMap<Sequence, Integer> readFrequentSequences(
			final File output) throws IOException {
		final HashMap<Sequence, Integer> sequences = Maps.newHashMap();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split("#SUP:");
				final String[] items = splitLine[0].split(" -1 ");
				final Sequence seq = new Sequence();
				for (final String item : items)
					seq.add(Integer.parseInt(item.trim()));
				final int supp = Integer.parseInt(splitLine[1].trim());
				sequences.put(seq, supp);
			}
		}
		// Sort sequences by support
		final Ordering<Sequence> comparator = Ordering.natural().reverse()
				.onResultOf(Functions.forMap(sequences))
				.compound(Ordering.usingToString());
		return ImmutableSortedMap.copyOf(sequences, comparator);
	}

}
