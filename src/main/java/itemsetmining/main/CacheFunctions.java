package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;
import itemsetmining.util.ParallelThreadPool;

import java.util.List;
import java.util.Set;

/** Class to hold the various serial/parallel itemset cache functions */
public class CacheFunctions {

	/** Serial initialize Cache */
	static void serialInitializeCache(final List<Transaction> transactions,
			final Set<Integer> singletons, final double prob) {

		for (final Transaction transaction : transactions)
			transaction.initializeCache(singletons, prob);
	}

	/** Serial add itemset to Cache */
	static void serialAddItemsetCache(final List<Transaction> transactions,
			final Itemset candidate, final double prob) {

		for (final Transaction transaction : transactions)
			transaction.addItemsetCache(candidate, prob);

	}

	/** Serial remove itemset from Cache */
	static void serialRemoveItemsetCache(final List<Transaction> transactions,
			final Itemset candidate, final double prob) {

		for (final Transaction transaction : transactions)
			transaction.removeItemsetCache(candidate, prob);

	}

	/** Parallel initialize Cache */
	static void parallelInitializeCache(final List<Transaction> transactions,
			final Set<Integer> singletons, final double prob) {

		final ParallelThreadPool ptp = new ParallelThreadPool();
		for (final Transaction transaction : transactions) {

			ptp.pushTask(new Runnable() {
				@Override
				public void run() {
					transaction.initializeCache(singletons, prob);
				}
			});
		}
		ptp.waitForTermination();

	}

	/** Parallel add itemset to Cache */
	static void parallelAddItemsetCache(final List<Transaction> transactions,
			final Itemset candidate, final double prob) {

		final ParallelThreadPool ptp = new ParallelThreadPool();
		for (final Transaction transaction : transactions) {

			ptp.pushTask(new Runnable() {
				@Override
				public void run() {
					transaction.addItemsetCache(candidate, prob);
				}
			});
		}
		ptp.waitForTermination();

	}

	/** Parallel remove itemset from Cache */
	static void parallelRemoveItemsetCache(
			final List<Transaction> transactions, final Itemset candidate,
			final double prob) {

		final ParallelThreadPool ptp = new ParallelThreadPool();
		for (final Transaction transaction : transactions) {

			ptp.pushTask(new Runnable() {
				@Override
				public void run() {
					transaction.removeItemsetCache(candidate, prob);
				}
			});
		}
		ptp.waitForTermination();

	}

	private CacheFunctions() {
	}

}
