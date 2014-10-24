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
		final String logfile = "plants-20.10.2014-11:12:45.log";
		final Set<Itemset> intItemsets = ItemsetPrecisionRecall
				.readSparkOutput(new File(LOGDIR + logfile)).keySet();
		System.out.println("\nIIM Itemsets");
		System.out.println("No itemsets: " + intItemsets.size());
		System.out.println("No items: "
				+ ItemsetScaling.countNoItems(intItemsets));

		// Measure symmetric difference between the two sets of itemsets
		double avgMinDiff = calculateRedundancy(intItemsets);
		System.out.println("\nAvg min sym diff: " + avgMinDiff);

		// Read in frequent itemsets
		final String outFile = "plants-fim.txt";
		final Set<Itemset> freqItemsets = FrequentItemsetMining
				.readFrequentItemsets(new File(FIMDIR + outFile)).keySet();
		System.out.println("\nFIM Itemsets");
		System.out.println("No itemsets: " + freqItemsets.size());
		System.out.println("No items: "
				+ ItemsetScaling.countNoItems(freqItemsets));

		// Measure symmetric difference between the two sets of itemsets
		avgMinDiff = calculateRedundancy(freqItemsets);
		System.out.println("\nAvg min sym diff: " + avgMinDiff);

	}

	private static double calculateRedundancy(final Set<Itemset> itemsets) {

		int count = 0;
		int avgMinDiff = 0;
		for (final Itemset set1 : itemsets) {
			count++;
			if (count % 100 == 0)
				System.out.println("Itemset " + count + " of "
						+ itemsets.size());
			int minDiff = Integer.MAX_VALUE;
			for (final Itemset set2 : itemsets) {
				if ((!set1.equals(set2))
						&& (set1.size() == 1 || set2.size() == 1)) {
					final int diff = cardSymDiff(set1, set2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= itemsets.size();

		return avgMinDiff;
	}

	private static <T> int cardSymDiff(final Collection<T> set1,
			final Collection<T> set2) {
		final int sizeUnion = CollectionUtils.union(set1, set2).size();
		final int sizeIntersection = CollectionUtils.intersection(set1, set2)
				.size();
		return (sizeUnion - sizeIntersection);
	}

}