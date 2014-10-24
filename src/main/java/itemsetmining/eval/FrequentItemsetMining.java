package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRules;
import ca.pfv.spmf.algorithms.frequentpatterns.apriori.AlgoApriori;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

import com.google.common.collect.Maps;

public class FrequentItemsetMining {

	public static void main(final String[] args) throws IOException {

		// FIM parameters
		final String dataset = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/plants.dat";
		final double minSupp = 0.1024; // relative support
		final String saveFile = "/tmp/plants-fim.txt";

		mineFrequentItemsetsFPGrowth(dataset, saveFile, minSupp);
		// generateAssociationRules(itemsets, dbSize, null, 0, 0);
	}

	/** Run FPGrowth algorithm */
	public static Itemsets mineFrequentItemsetsFPGrowth(final String dataset,
			final String saveFile, final double minSupp) throws IOException {

		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(dataset, saveFile, minSupp);
		// algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		return patterns;
	}

	/** Run Apriori algorithm */
	public static Itemsets mineFrequentItemsetsApriori(final String dataset,
			final String saveFile, final double minSupp) throws IOException {

		final AlgoApriori algo = new AlgoApriori();
		final Itemsets patterns = algo.runAlgorithm(minSupp, dataset, saveFile);
		// algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		return patterns;
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

	/** Read in frequent itemsets */
	public static HashMap<Itemset, Integer> readFrequentItemsets(
			final File output) throws IOException {
		final HashMap<Itemset, Integer> itemsets = Maps.newHashMap();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		for (final String line : lines) {
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

		return itemsets;
	}

}
