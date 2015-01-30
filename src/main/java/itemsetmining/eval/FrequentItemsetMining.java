package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRules;
import ca.pfv.spmf.algorithms.frequentpatterns.apriori.AlgoApriori;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

public class FrequentItemsetMining {

	public static void main(final String[] args) throws IOException {

		// FIM parameters
		final String dataset = "uganda";
		final double minSupp = 0.001; // relative support
		final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/"
				+ dataset + ".dat";
		final String saveFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/FIM/"
				+ dataset + ".txt";

		mineFrequentItemsetsApriori(dbPath, saveFile, minSupp);
		// generateAssociationRules(itemsets, dbSize, null, 0, 0);
		final SortedMap<Itemset, Integer> freqItemsets = readFrequentItemsets(new File(
				saveFile));
		System.out.println("\nFIM Itemsets");
		System.out.println("No itemsets: " + freqItemsets.size());

	}

	/** Run FPGrowth algorithm */
	public static SortedMap<Itemset, Integer> mineFrequentItemsetsFPGrowth(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(dataset, saveFile, minSupp);
		// algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		return toMap(patterns);
	}

	/**
	 * Run Apriori algorithm
	 *
	 * @deprecated appears to be broken
	 */
	@Deprecated
	public static SortedMap<Itemset, Integer> mineFrequentItemsetsApriori(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final AlgoApriori algo = new AlgoApriori();
		final Itemsets patterns = algo.runAlgorithm(minSupp, dataset, saveFile);
		// algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		return toMap(patterns);
	}

	/** Generate association rules from FIM itemsets */
	public static AssocRules generateAssociationRules(final Itemsets patterns,
			final int databaseSize, final String saveFile,
			final double minConf, final double minLift) throws IOException {

		final AlgoAgrawalFaster94 algo = new AlgoAgrawalFaster94();
		final AssocRules rules = algo.runAlgorithm(patterns, saveFile,
				databaseSize, minConf, minLift);
		if (saveFile == null)
			rules.printRulesWithLift(databaseSize);

		return rules;
	}

	/** Convert frequent itemsets to sorted Map<Itemset, Integer> */
	public static SortedMap<Itemset, Integer> toMap(final Itemsets patterns) {
		if (patterns == null) {
			return null;
		} else {
			final HashMap<Itemset, Integer> itemsets = Maps.newHashMap();
			for (final List<ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset> level : patterns
					.getLevels()) {
				for (final ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset itemset : level)
					itemsets.put(new Itemset(itemset.getItems()),
							itemset.getAbsoluteSupport());
			}
			// Sort itemsets by support
			final Ordering<Itemset> comparator = Ordering.natural().reverse()
					.onResultOf(Functions.forMap(itemsets))
					.compound(Ordering.usingToString());
			return ImmutableSortedMap.copyOf(itemsets, comparator);
		}
	}

	/** Read in frequent itemsets */
	public static SortedMap<Itemset, Integer> readFrequentItemsets(
			final File output) throws IOException {
		final HashMap<Itemset, Integer> itemsets = Maps.newHashMap();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split("#SUP:");
				final String[] items = splitLine[0].split(" ");
				final Itemset itemset = new Itemset();
				for (final String item : items)
					itemset.add(Integer.parseInt(item.trim()));
				final int supp = Integer.parseInt(splitLine[1].trim());
				itemsets.put(itemset, supp);
			}
		}
		// Sort itemsets by support
		final Ordering<Itemset> comparator = Ordering.natural().reverse()
				.onResultOf(Functions.forMap(itemsets))
				.compound(Ordering.usingToString());
		return ImmutableSortedMap.copyOf(itemsets, comparator);
	}

}
