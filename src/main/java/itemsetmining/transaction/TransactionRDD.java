package itemsetmining.transaction;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;

/** Wrapper class for storing transaction database as a Spark RDD */
public class TransactionRDD extends TransactionDatabase {

	private final JavaRDD<Transaction> transactions;
	private final long noTransactions;

	public TransactionRDD(final JavaRDD<Transaction> transactions,
			final long noTransactions) {
		this.transactions = transactions;
		this.noTransactions = noTransactions;
	}

	@Override
	public List<Transaction> getTransactionList() {
		return null;
	}

	@Override
	public JavaRDD<Transaction> getTransactionRDD() {
		return transactions;
	}

	@Override
	public long size() {
		return noTransactions;
	}

}
