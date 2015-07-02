package itemsetmining.eval;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoPrefixSpan;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPattern;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.AlgoSPADE;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.AlgoSPAM_AGP;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.candidatePatternsGeneration.CandidateGenerator;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.candidatePatternsGeneration.CandidateGenerator_Qualitative;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.creators.AbstractionCreator;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.creators.AbstractionCreator_Qualitative;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.idLists.creators.IdListCreator_FatBitmap;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;
import itemsetmining.sequence.Sequence;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

public class FrequentSequenceMining {

	public static void main(final String[] args) throws IOException {

		// FIM parameters
		final String dataset = "libraries_filtered";
		final double minSupp = 0.016; // relative support
		final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/libraries/"
				+ dataset + ".dat";
		final String saveFile = "/disk/data1/jfowkes/logs/" + dataset + ".txt";

		mineFrequentSequencesSPAM(dbPath, saveFile, minSupp);
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

	/** Run SPADE algorithm */
	public static SortedMap<Sequence, Integer> mineFrequentSequencesSPADE(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final boolean verbose = true;

		final AbstractionCreator abstractionCreator = AbstractionCreator_Qualitative
				.getInstance();
		final ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase sequenceDatabase = new ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase(
				abstractionCreator, IdListCreator_FatBitmap.getInstance());
		sequenceDatabase.loadFile(dataset, minSupp);

		final AlgoSPADE algo = new AlgoSPADE(minSupp, true, abstractionCreator);
		final CandidateGenerator candidateGenerator = CandidateGenerator_Qualitative
				.getInstance();
		algo.runAlgorithmParallelized(sequenceDatabase, candidateGenerator,
				true, verbose, saveFile, false);
		// algo.printStatistics();

		return null;
	}

	/** Run SPAM algorithm */
	public static SortedMap<Sequence, Integer> mineFrequentSequencesSPAM(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final boolean verbose = true;

		final AbstractionCreator abstractionCreator = AbstractionCreator_Qualitative
				.getInstance();
		final ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase sequenceDatabase = new ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase(
				abstractionCreator, IdListCreator_FatBitmap.getInstance());
		sequenceDatabase.loadFile(dataset, minSupp);

		final AlgoSPAM_AGP algorithm = new AlgoSPAM_AGP(minSupp);
		algorithm
				.runAlgorithm(sequenceDatabase, true, verbose, saveFile, false);
		// algo.printStatistics();

		return null;
	}

	/** Run BIDE algorithm */
	public static SortedMap<Sequence, Integer> mineFrequentClosedSequencesBIDE(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final SequenceDatabase sequenceDatabase = new SequenceDatabase();
		sequenceDatabase.loadFile(dataset);

		// Convert to absolute support (rounding down)
		final int absMinSupp = (int) (sequenceDatabase.size() * minSupp);

		final AlgoBIDEPlus algo = new AlgoBIDEPlus();
		algo.setShowSequenceIdentifiers(false);
		final SequentialPatterns patterns = algo.runAlgorithm(sequenceDatabase,
				saveFile, absMinSupp);
		// algo.printStatistics(sequenceDatabase.size());

		return toMap(patterns);
	}

	/** Convert frequent sequences to sorted Map<Sequence, Integer> */
	public static SortedMap<Sequence, Integer> toMap(
			final SequentialPatterns patterns) {
		if (patterns == null) {
			return null;
		} else {
			final HashMap<Sequence, Integer> sequences = new HashMap<>();
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

	/** Read in frequent sequences (sorted by support) */
	public static SortedMap<Sequence, Integer> readFrequentSequences(
			final File output) throws IOException {
		final HashMap<Sequence, Integer> sequences = new HashMap<>();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split("#SUP:");
				final String[] items = splitLine[0].trim().split("-1");
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
