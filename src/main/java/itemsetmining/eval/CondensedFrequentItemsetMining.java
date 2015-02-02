package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharmMFI;
import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharm_Bitset;
import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;
import ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemsets;
import ca.pfv.spmf.tools.other_dataset_tools.FixTransactionDatabaseTool;

import com.google.common.collect.Maps;

public class CondensedFrequentItemsetMining {

	private static final String TMPDB = "/tmp/fixed-dataset.dat";
	private static final FixTransactionDatabaseTool dbTool = new FixTransactionDatabaseTool();

	public static void main(final String[] args) throws IOException {

		// FPGrowth parameters
		final String dataset = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/plants.dat";
		final double minSupp = 0.111; // relative support
		final String saveFile = "/tmp/plants-closed-fim.txt";

		mineClosedFrequentItemsetsCharm(dataset, saveFile, minSupp);
		// mineMaximalFrequentItemsetsCharm(dataset, saveFile, minSupp);

	}

	/** Run Charm closed FIM algorithm */
	public static HashMap<Itemset, Integer> mineClosedFrequentItemsetsCharm(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		// Remove transaction duplicates and sort items ascending
		dbTool.convert(dataset, TMPDB);

		final TransactionDatabase database = new TransactionDatabase();
		database.loadFile(TMPDB);

		final AlgoCharm_Bitset algo = new AlgoCharm_Bitset();
		final Itemsets patterns = algo.runAlgorithm(saveFile, database,
				minSupp, true, 10000);
		// algo.printStats();
		// patterns.printItemsets(database.size());

		return toMap(patterns);
	}

	/** Run Charm maximal closed FIM algorithm */
	public static HashMap<Itemset, Integer> mineMaximalFrequentItemsetsCharm(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		// Remove transaction duplicates and sort items ascending
		dbTool.convert(dataset, TMPDB);

		final TransactionDatabase database = new TransactionDatabase();
		database.loadFile(TMPDB);

		final AlgoCharm_Bitset algo_closed = new AlgoCharm_Bitset();
		final Itemsets closed_patterns = algo_closed.runAlgorithm(null,
				database, minSupp, true, 10000);

		final AlgoCharmMFI algo = new AlgoCharmMFI();
		final Itemsets patterns = algo.runAlgorithm(saveFile, closed_patterns);

		return toMap(patterns);
	}

	/** Convert frequent itemsets to HashMap<Itemset, Integer> */
	public static HashMap<Itemset, Integer> toMap(final Itemsets patterns) {
		final HashMap<Itemset, Integer> itemsets = Maps.newHashMap();
		for (final List<ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemset> level : patterns
				.getLevels()) {
			for (final ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemset itemset : level)
				itemsets.put(new Itemset(itemset.getItems()),
						itemset.getAbsoluteSupport());
		}
		return itemsets;
	}

}
