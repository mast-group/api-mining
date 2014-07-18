package itemsetmining.eval;

import java.io.IOException;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.Rules;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class FPGrowthItemsetMining {

	public static void main(final String[] args) throws IOException {

		// FPGrowth parameters
		final String dataset = "/tmp/caviar.txt";
		final boolean associationRules = false;
		final double fpGrowthSupport = 0.25; // relative support
		final double fpGrowthMinConf = 0;
		final double fpGrowthMinLift = 0;

		// Run FPGROWTH algorithm
		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(dataset, null,
				fpGrowthSupport);
		algo.printStats();
		patterns.printItemsets(algo.getDatabaseSize());

		// Generate association rules from FPGROWTH itemsets
		if (associationRules) {
			final AlgoAgrawalFaster94 algo2 = new AlgoAgrawalFaster94();
			final Rules rules2 = algo2.runAlgorithm(patterns, null,
					algo.getDatabaseSize(), fpGrowthMinConf, fpGrowthMinLift);
			rules2.printRulesWithLift(algo.getDatabaseSize());
		}
	}

}
