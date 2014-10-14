package itemsetmining.eval;

import java.io.IOException;

import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharmMFI;
import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharm_Bitset;
import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;
import ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemsets;

public class CondensedFrequentItemsetMining {

	public static void main(final String[] args) throws IOException {

		// FPGrowth parameters
		final String dataset = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/plants.dat";
		final double minSupp = 0.111; // relative support
		final String saveFile = "/tmp/plants-closed-fim.txt";

		mineClosedFrequentItemsetsCharm(dataset, saveFile, minSupp);
		// mineMaximalFrequentItemsetsCharm(dataset, saveFile, minSupp);

	}

	/** Run Charm closed FIM algorithm */
	public static Itemsets mineClosedFrequentItemsetsCharm(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final TransactionDatabase database = new TransactionDatabase();
		database.loadFile(dataset);

		final AlgoCharm_Bitset algo = new AlgoCharm_Bitset();
		final Itemsets patterns = algo.runAlgorithm(saveFile, database,
				minSupp, true, 10000);
		// algo.printStats();
		// patterns.printItemsets(database.size());

		return patterns;
	}

	/** Run Charm maximal closed FIM algorithm */
	public static Itemsets mineMaximalFrequentItemsetsCharm(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		final Itemsets closedFrequent = mineClosedFrequentItemsetsCharm(
				dataset, null, minSupp);

		final AlgoCharmMFI algo = new AlgoCharmMFI();
		final Itemsets patterns = algo.runAlgorithm(saveFile, closedFrequent);

		return patterns;
	}

}
