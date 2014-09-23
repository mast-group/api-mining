package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

public class ItemsetJaccard {

	private static final String LOGDIR = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/";
	private static final String FIMDIR = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/FIM/";

	public static void main(final String[] args) throws IOException {

		// Read in interesting itemsets
		final String logfile = "plants-CombSimp-18.09.2014-14:11:33.log";
		final Set<Itemset> intItemsets = ItemsetPrecisionRecall
				.readSparkOutput(new File(LOGDIR + logfile)).keySet();

		// Read in frequent itemsets
		final String outFile = "plants-fim.txt";
		final Set<Itemset> freqItemsets = FrequentItemsetMining
				.readFrequentItemsets(new File(FIMDIR + outFile)).keySet();

		// Measure Jaccard between the two sets of itemsets
		for (final Itemset fset : freqItemsets) {
			System.out.println("FIM: " + fset);
			for (final Itemset iset : intItemsets) {
				final double jaccard = jaccard(iset, fset);
				System.out.println("jaccard: " + jaccard);
			}
			System.out.println("====");
		}

	}

	private static <T> double jaccard(final Collection<T> set1,
			final Collection<T> set2) {
		final int sizeUnion = CollectionUtils.union(set1, set2).size();
		final int sizeIntersection = CollectionUtils.intersection(set1, set2)
				.size();
		return (sizeUnion - sizeIntersection) / (double) sizeUnion;
	}

}