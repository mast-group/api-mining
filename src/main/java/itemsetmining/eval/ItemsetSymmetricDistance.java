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
				"ISM-alice_nostop-28.05.2015-10:17:38.log",
				"ISM-SIGN-27.05.2015-15:12:45.log",
				"ISM-GAZELLE1-01.06.2015-17:15:17.log",
				"ISM-libraries_filtered-01.06.2015-18:18:29.log" };
		final String[] FSMlogs = new String[] { "alice_nostop.txt", "SIGN.txt",
				"GAZELLE1.txt", "libraries_filtered_small.txt" };

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

			// Get top interesting sequences
			final Set<Sequence> topIntItemsets = filterSingletons(intItemsets);

			// Calculate redundancy
			double avgMinDiff = calculateRedundancy(topIntItemsets);
			System.out.println("\nAvg min edit dist: " + avgMinDiff);

			// Calculate spuriousness
			double avgMaxSpur = calculateSpuriousness(topIntItemsets);
			System.out.println("Avg no. subseq: " + avgMaxSpur);

			// Calculate no. items
			int noItems = countNoItems(topIntItemsets);
			System.out.println("No. items: " + noItems);

			// Calculate size
			double avgSize = calculateAverageSize(topIntItemsets);
			System.out.println("Avg subseq size: " + avgSize);

			// Read in frequent sequences
			final SortedMap<Sequence, Integer> freqItemsets = FrequentItemsetMining
					.readFrequentSequences(new File(baseDir + "FIM/"
							+ FSMlogs[i]));
			System.out.println("\nFSM Sequences\n------------");
			System.out.println("No sequences: " + freqItemsets.size());
			System.out.println("No items: "
					+ countNoItems(freqItemsets.keySet()));

			// Get top frequent sequences
			final Set<Sequence> topFreqItemsets = filterSingletons(freqItemsets);

			// Calculate redundancy
			avgMinDiff = calculateRedundancy(topFreqItemsets);
			System.out.println("\nAvg min edit dist: " + avgMinDiff);

			// Calculate spuriousness
			avgMaxSpur = calculateSpuriousness(topFreqItemsets);
			System.out.println("Avg no. subseq: " + avgMaxSpur);

			// Calculate no. items
			noItems = countNoItems(topFreqItemsets);
			System.out.println("No. items: " + noItems);

			// Calculate size
			avgSize = calculateAverageSize(topFreqItemsets);
			System.out.println("Avg subseq size: " + avgSize);

			System.out.println();

		}

	}

	private static <V> double calculateRedundancy(
			final Set<Sequence> topItemsets) {

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

	private static double calculateAverageSize(final Set<Sequence> topItemsets) {

		double avgSize = 0;
		for (final Sequence seq : topItemsets)
			avgSize += seq.size();
		return avgSize / topItemsets.size();
	}

	private static <V> double calculateSpuriousness(
			final Set<Sequence> topItemsets) {

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

	/** Filter out singletons */
	private static <V> Set<Sequence> filterSingletons(
			final Map<Sequence, V> itemsets) {

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

		return topItemsets;
	}

	private static int isSubseq(final Sequence seq1, final Sequence seq2) {
		if (seq2.contains(seq1))
			return 1;
		return 0;
	}

}