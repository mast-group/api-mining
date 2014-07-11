package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionDatabase;
import itemsetmining.transaction.TransactionRDD;

import java.util.HashMap;
import java.util.Set;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;

/** Class to hold the various parallel itemset cache functions for Spark */
public class SparkCacheFunctions {

	/** Spark parallel initialize Cache */
	static TransactionDatabase parallelInitializeCache(
			final TransactionDatabase transactions,
			final Set<Integer> singletons, final double prob) {

		JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 8003006706694722226L;

							@Override
							public Transaction call(Transaction transaction)
									throws Exception {
								transaction.initializeCache(singletons, prob);
								return transaction;
							}

						});

		return new TransactionRDD(updatedTransactions, transactions.size());
	}

	/** Spark parallel update Cache probabilities */
	static TransactionDatabase parallelUpdateCacheProbabilities(
			final TransactionDatabase transactions,
			final HashMap<Itemset, Double> newItemsets) {

		JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 2790115026883265577L;

							@Override
							public Transaction call(Transaction transaction)
									throws Exception {
								transaction
										.updateCacheProbabilities(newItemsets);
								return transaction;
							}

						});

		return new TransactionRDD(updatedTransactions, transactions.size());
	}

	/** Spark parallel add itemset to Cache */
	static TransactionRDD parallelAddItemsetCache(
			final TransactionDatabase transactions, final Itemset candidate,
			final double prob) {

		JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 3436396242612335594L;

							@Override
							public Transaction call(Transaction transaction)
									throws Exception {
								transaction.addItemsetCache(candidate, prob);
								return transaction;
							}

						});

		return new TransactionRDD(updatedTransactions, transactions.size());
	}

	/** Spark parallel remove itemset from Cache */
	static TransactionRDD parallelRemoveItemsetCache(
			final TransactionDatabase transactions, final Itemset candidate,
			final double prob) {

		JavaRDD<Transaction> updatedTransactions = transactions
				.getTransactionRDD().map(
						new Function<Transaction, Transaction>() {
							private static final long serialVersionUID = 1629357166651253429L;

							@Override
							public Transaction call(Transaction transaction)
									throws Exception {
								transaction.removeItemsetCache(candidate, prob);
								return transaction;
							}

						});

		return new TransactionRDD(updatedTransactions, transactions.size());
	}

	private SparkCacheFunctions() {
	}

}
