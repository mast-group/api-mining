package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;
import itemsetmining.util.ParallelThreadPool;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Multiset;

/** Class to hold the various serial/parallel itemset cache functions */
public class CacheFunctions {

	/** Serial initialize Cache */
	static void serialInitializeCache(final List<Transaction> transactions,
			final long noTransactions, final Multiset<Integer> singletons) {

		for (final Transaction transaction : transactions)
			transaction.initializeCache(singletons, noTransactions);
	}

	/** Serial update Cache probabilities */
	static void serialUpdateCacheProbabilities(
			final List<Transaction> transactions,
			final HashMap<Itemset, Double> newItemsets) {

		for (final Transaction transaction : transactions)
			transaction.updateCacheProbabilities(newItemsets);
	}

	/** Serial add itemset to Cache */
	static void serialAddItemsetCache(final List<Transaction> transactions,
			final Itemset candidate, final double prob, Set<Itemset> subsets) {

		for (final Transaction transaction : transactions)
			transaction.addItemsetCache(candidate, prob, subsets);

	}

	/** Parallel initialize Cache */
	static void parallelInitializeCache(final List<Transaction> transactions,
			final long noTransactions, final Multiset<Integer> singletons) {

		final ParallelThreadPool ptp = new ParallelThreadPool();
		for (final Transaction transaction : transactions) {

			ptp.pushTask(new Runnable() {
				@Override
				public void run() {
					transaction.initializeCache(singletons, noTransactions);
				}
			});
		}
		ptp.waitForTermination();

	}

	/** Parallel update Cache probabilities */
	static void parallelUpdateCacheProbabilities(
			final List<Transaction> transactions,
			final HashMap<Itemset, Double> newItemsets) {

		final ParallelThreadPool ptp = new ParallelThreadPool();
		for (final Transaction transaction : transactions) {

			ptp.pushTask(new Runnable() {
				@Override
				public void run() {
					transaction.updateCacheProbabilities(newItemsets);
				}
			});
		}
		ptp.waitForTermination();

	}

	/** Parallel add itemset to Cache */
	static void parallelAddItemsetCache(final List<Transaction> transactions,
			final Itemset candidate, final double prob, Set<Itemset> subsets) {

		final ParallelThreadPool ptp = new ParallelThreadPool();
		for (final Transaction transaction : transactions) {

			ptp.pushTask(new Runnable() {
				@Override
				public void run() {
					transaction.addItemsetCache(candidate, prob, subsets);
				}
			});
		}
		ptp.waitForTermination();

	}

	private CacheFunctions() {
	}

}
