package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionDatabase;

import java.util.HashMap;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;

import com.google.common.collect.Multiset;

/** Class to hold the various parallel itemset cache functions for Spark */
public class SparkCacheFunctions {

	/** Spark parallel initialize Cache */
	static TransactionDatabase parallelInitializeCache(
			final TransactionDatabase transactions,
			final Multiset<Integer> singletons) {

		final long noTransactions = transactions.size();
		final JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 8003006706694722226L;

							@Override
							public Transaction call(
									final Transaction transaction)
									throws Exception {
								transaction.initializeCache(singletons,
										noTransactions);
								return transaction;
							}

						});

		// Update cache
		transactions.updateTransactionCache(updatedTransactions);
		return transactions;
	}

	/** Spark parallel update Cache probabilities */
	static TransactionDatabase parallelUpdateCacheProbabilities(
			final TransactionDatabase transactions,
			final HashMap<Itemset, Double> newItemsets) {

		final JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 2790115026883265577L;

							@Override
							public Transaction call(
									final Transaction transaction)
									throws Exception {
								transaction
										.updateCacheProbabilities(newItemsets);
								return transaction;
							}

						});

		// Update cache
		transactions.updateTransactionCache(updatedTransactions);
		return transactions;
	}

	/** Spark parallel add itemset to Cache */
	static TransactionDatabase parallelAddItemsetCache(
			final TransactionDatabase transactions, final Itemset candidate,
			final double prob) {

		final JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 3436396242612335594L;

							@Override
							public Transaction call(
									final Transaction transaction)
									throws Exception {
								transaction.addItemsetCache(candidate, prob,
										subsets);
								return transaction;
							}

						});

		// Update cache
		transactions.updateTransactionCache(updatedTransactions);
		return transactions;
	}

	private SparkCacheFunctions() {
	}

}
