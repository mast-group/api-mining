package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.ItemsetMiningCore;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.collect.Sets;

public class ItemsetSymmetricDistance {

	private static final int topN = 100;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";

	public static void main(final String[] args) throws IOException {

		final String[] IIMlogs = new String[] {
				"plants-20.10.2014-11:12:45.log",
				"mammals-24.10.2014-14:24:38.log",
				"abstracts-21.10.2014-13:46:14.log",
				"IIM-uganda_en_3m_filtered-14.01.2015-16:36:50.log" };
		final String[] FIMlogs = new String[] { "plants.txt", "mammals.txt",
				"abstracts.txt", "uganda.txt" };

		for (int i = 0; i < IIMlogs.length; i++) {

			System.out.println("===== Dataset: "
					+ FIMlogs[i].substring(0, FIMlogs[i].lastIndexOf('.')));

			// Read in interesting itemsets
			final Map<Itemset, Double> intItemsets = ItemsetMiningCore
					.readIIMItemsets(new File(baseDir + "Logs/" + IIMlogs[i]));
			System.out.println("\nIIM Itemsets\n-----------");
			System.out.println("No itemsets: " + intItemsets.size());
			System.out.println("No items: "
					+ ItemsetScaling.countNoItems(intItemsets.keySet()));

			// Measure symmetric difference between the two sets of itemsets
			double avgMinDiff = calculateRedundancy(intItemsets);
			System.out.println("\nAvg min sym diff: " + avgMinDiff);

			// Read in MTV itemsets
			final LinkedHashMap<Itemset, Double> mtvItemsets = MTVItemsetMining
					.readMTVItemsets(new File(baseDir + "MTV/" + FIMlogs[i]));
			System.out.println("\nMTV Itemsets\n------------");
			System.out.println("No itemsets: " + mtvItemsets.size());
			System.out.println("No items: "
					+ ItemsetScaling.countNoItems(mtvItemsets.keySet()));

			// Measure symmetric difference between the two sets of itemsets
			avgMinDiff = calculateRedundancy(mtvItemsets);
			System.out.println("\nAvg min sym diff: " + avgMinDiff);

			// Read in frequent itemsets
			final SortedMap<Itemset, Integer> freqItemsets = FrequentItemsetMining
					.readFrequentItemsets(new File(baseDir + "FIM/"
							+ FIMlogs[i]));
			System.out.println("\nFIM Itemsets\n------------");
			System.out.println("No itemsets: " + freqItemsets.size());
			System.out.println("No items: "
					+ ItemsetScaling.countNoItems(freqItemsets.keySet()));

			// Measure symmetric difference between the two sets of itemsets
			avgMinDiff = calculateRedundancy(freqItemsets);
			System.out.println("\nAvg min sym diff: " + avgMinDiff);

			System.out.println();

		}

	}

	private static <V> double calculateRedundancy(final Map<Itemset, V> itemsets) {

		// Filter out singletons
		int count = 0;
		final Set<Itemset> topItemsets = Sets.newHashSet();
		for (final Itemset set : itemsets.keySet()) {
			if (set.size() != 1) {
				topItemsets.add(set);
				count++;
			}
			if (count == topN)
				break;
		}
		if (count < 100)
			System.out.println("Not enough non-singleton itemsets in set: "
					+ count);

		int avgMinDiff = 0;
		for (final Itemset set1 : topItemsets) {

			int minDiff = Integer.MAX_VALUE;
			for (final Itemset set2 : topItemsets) {
				if (!set1.equals(set2)) {
					final int diff = cardSymDiff(set1, set2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= topItemsets.size();

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