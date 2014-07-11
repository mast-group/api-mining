package itemsetmining.transaction;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;

/** Wrapper class for storing transaction database as a Spark RDD */
public class TransactionRDD extends TransactionDatabase {

	private JavaRDD<Transaction> transactions;
	private final long noTransactions;

	public TransactionRDD(final JavaRDD<Transaction> transactions,
			final long noTransactions) {
		this.transactions = transactions;
		this.noTransactions = noTransactions;
	}

	@Override
	public List<Transaction> getTransactionList() {
		throw new UnsupportedOperationException("This is a RDD not a List!!");
	}

	@Override
	public JavaRDD<Transaction> getTransactionRDD() {
		return transactions;
	}

	@Override
	public void updateTransactionCache(
			final JavaRDD<Transaction> updatedTransactions) {
		transactions = updatedTransactions;
	}

	@Override
	public long size() {
		return noTransactions;
	}

}
