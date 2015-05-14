package itemsetmining.eval;

import itemsetmining.itemset.Sequence;
import itemsetmining.main.ItemsetMiningCore;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class ItemsetSymmetricDistance {

	private static final int topN = 100;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/";

	public static void main(final String[] args) throws IOException {

		final String[] ISMlogs = new String[] {
				"ISM-SIGN-16.04.2015-12:10:24.log",
				"ISM-LEVIATHAN-22.04.2015-17:14:04.log",
				"ISM-GAZELLE1-21.04.2015-16:04:19.log" };
		final String[] FSMlogs = new String[] { "SIGN.txt", "LEVIATHAN.txt",
				"GAZELLE1.txt" };

		for (int i = 0; i < ISMlogs.length; i++) {

			System.out.println("===== Dataset: "
					+ FSMlogs[i].substring(0, FSMlogs[i].lastIndexOf('.')));

			// Read in interesting sequences
			final Map<Sequence, Double> intItemsets = ItemsetMiningCore
					.readISMSequences(new File(baseDir + "Logs/" + ISMlogs[i]));
			System.out.println("\nISM Sequences\n-----------");
			System.out.println("No sequences: " + intItemsets.size());
			System.out.println("No items: "
					+ countNoItems(intItemsets.keySet()));

			// Calculate redundancy
			double avgMinDiff = calculateRedundancy(intItemsets);
			System.out.println("\nAvg min edit dist: " + avgMinDiff);

			// Calculate spuriousness
			double avgMaxSpur = calculateSpuriousness(intItemsets);
			System.out.println("Avg no. subseq: " + avgMaxSpur);

			// Calculate size
			double avgSize = calculateAverageSize(intItemsets);
			System.out.println("Avg subseq size: " + avgSize);

			// Read in frequent sequences
			final SortedMap<Sequence, Integer> freqItemsets = FrequentItemsetMining
					.readFrequentSequences(new File(baseDir + "FIM/"
							+ FSMlogs[i]));
			System.out.println("\nFSM Sequences\n------------");
			System.out.println("No sequences: " + freqItemsets.size());
			System.out.println("No items: "
					+ countNoItems(freqItemsets.keySet()));

			// Calculate redundancy
			avgMinDiff = calculateRedundancy(freqItemsets);
			System.out.println("\nAvg min edit dist: " + avgMinDiff);

			// Calculate spuriousness
			avgMaxSpur = calculateSpuriousness(freqItemsets);
			System.out.println("Avg no. subseq: " + avgMaxSpur);

			// Calculate size
			avgSize = calculateAverageSize(freqItemsets);
			System.out.println("Avg subseq size: " + avgSize);

			System.out.println();

		}

	}

	private static <V> double calculateRedundancy(
			final Map<Sequence, V> itemsets) {

		// Filter out singletons
		int count = 0;
		final Set<Sequence> topItemsets = new HashSet<>();
		for (final Sequence set : itemsets.keySet()) {
			if (set.size() != 1) {
				topItemsets.add(set);
				count++;
			}
			if (count == topN)
				break;
		}
		if (count < 100)
			System.out.println("Not enough non-singleton sequences in set: "
					+ count);

		double avgMinDiff = 0;
		for (final Sequence set1 : topItemsets) {

			int minDiff = Integer.MAX_VALUE;
			for (final Sequence set2 : topItemsets) {
				if (!set1.equals(set2)) {
					final int diff = editDistance(set1, set2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= topItemsets.size();

		return avgMinDiff;
	}

	/**
	 * Calculate the Levenshtein distance between two sequences using the
	 * Wagner-Fischer algorithm
	 *
	 * @see http://en.wikipedia.org/wiki/Levenshtein_distance
	 */
	private static int editDistance(final Sequence s, final Sequence t) {
		final int m = s.size();
		final int n = t.size();

		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t;
		final int[][] d = new int[m + 1][n + 1];

		// the distance of any first string to an empty second string
		for (int i = 1; i <= m; i++)
			d[i][0] = i;

		// the distance of any second string to an empty first string
		for (int j = 1; j <= n; j++)
			d[0][j] = j;

		for (int j = 1; j <= n; j++) {
			for (int i = 1; i <= m; i++) {
				if (s.get(i - 1) == t.get(j - 1)) {
					d[i][j] = d[i - 1][j - 1]; // no operation required
				} else {
					d[i][j] = Math.min(d[i - 1][j] + 1, // a deletion
							Math.min(d[i][j - 1] + 1, // an insertion
									d[i - 1][j - 1] + 1)); // a substitution
				}
			}
		}

		return d[m][n];
	}

	/**
	 * Count the number of distinct items in the set of sequences
	 */
	public static int countNoItems(final Set<Sequence> sequences) {
		final Set<Integer> items = new HashSet<>();
		for (final Sequence seq : sequences)
			items.addAll(seq.getItems());
		return items.size();
	}

	private static <V> double calculateAverageSize(
			final Map<Sequence, V> itemsets) {
		double avgSize = 0;
		for (final Sequence seq : itemsets.keySet())
			avgSize += seq.size();
		return avgSize / itemsets.size();
	}

	private static <V> double calculateSpuriousness(
			final Map<Sequence, V> itemsets) {

		// Filter out singletons
		int count = 0;
		final Set<Sequence> topItemsets = new HashSet<>();
		for (final Sequence set : itemsets.keySet()) {
			if (set.size() != 1) {
				topItemsets.add(set);
				count++;
			}
			if (count == topN)
				break;
		}
		if (count < 100)
			System.out.println("Not enough non-singleton sequences in set: "
					+ count);

		double avgSubseq = 0;
		for (final Sequence set1 : topItemsets) {
			for (final Sequence set2 : topItemsets) {
				if (!set1.equals(set2))
					avgSubseq += isSubseq(set1, set2);
			}
		}
		avgSubseq /= topItemsets.size();

		return avgSubseq;
	}

	private static int isSubseq(final Sequence seq1, final Sequence seq2) {
		if (seq2.contains(seq1))
			return 1;
		return 0;
	}

}