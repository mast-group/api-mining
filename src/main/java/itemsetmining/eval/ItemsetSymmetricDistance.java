package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

public class ItemsetSymmetricDistance {

	private static final String LOGDIR = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/";
	private static final String FIMDIR = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/FIM/";

	public static void main(final String[] args) throws IOException {

		// Read in interesting itemsets
		final String logfile = "plants-CombSupp-24.09.2014-17:20:05.log";
		final Set<Itemset> intItemsets = ItemsetPrecisionRecall
				.readSparkOutput(new File(LOGDIR + logfile)).keySet();
		System.out.println("\nIIM Itemsets");
		System.out.println("No itemsets: " + intItemsets.size());
		System.out.println("No items: "
				+ ItemsetScaling.countNoItems(intItemsets));

		// Read in frequent itemsets
		final String outFile = "plants-fim.txt";
		final Set<Itemset> freqItemsets = FrequentItemsetMining
				.readFrequentItemsets(new File(FIMDIR + outFile)).keySet();
		System.out.println("\nFIM Itemsets");
		System.out.println("No itemsets: " + freqItemsets.size());
		System.out.println("No items: "
				+ ItemsetScaling.countNoItems(freqItemsets));

		// Measure symmetric difference between the two sets of itemsets
		int count = 0;
		double avgMinDiff = 0;
		for (final Itemset fset : freqItemsets) {
			count++;
			if (count % 10000 == 0)
				System.out
						.println("FI " + count + " of " + freqItemsets.size());
			int minDiff = Integer.MAX_VALUE;
			for (final Itemset iset : intItemsets) {
				final int diff = cardSymDiff(iset, fset);
				if (diff < minDiff)
					minDiff = diff;
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= freqItemsets.size();
		System.out.println("Avg min sym diff: " + avgMinDiff);

		// Measure symmetric difference between the two sets of itemsets
		// int count2 = 0;
		// double avgMinDiff = 0;
		// for (final Itemset iset : intItemsets) {
		// count2++;
		// System.out.println("Interesting itemset " + count2 + " of "
		// + intItemsets.size());
		// int count = 0;
		// int minDiff = Integer.MAX_VALUE;
		// for (final Itemset fset : freqItemsets) {
		// count++;
		// if (count % 10000 == 0)
		// System.out.println("FI " + count + " of "
		// + freqItemsets.size());
		// final int diff = cardSymDiff(iset, fset);
		// if (diff < minDiff)
		// minDiff = diff;
		// }
		// avgMinDiff += minDiff;
		// }
		// avgMinDiff /= freqItemsets.size();
		// System.out.println("Avg min sym diff: " + avgMinDiff);

	}

	private static <T> int cardSymDiff(final Collection<T> set1,
			final Collection<T> set2) {
		final int sizeUnion = CollectionUtils.union(set1, set2).size();
		final int sizeIntersection = CollectionUtils.intersection(set1, set2)
				.size();
		return (sizeUnion - sizeIntersection);
	}

}