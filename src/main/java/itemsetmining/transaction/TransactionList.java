package itemsetmining.transaction;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;

/** Wrapper class for storing transaction database as a list of transactions */
public class TransactionList extends TransactionDatabase {

	private final List<Transaction> transactions;

	public TransactionList(final List<Transaction> transactions) {
		this.transactions = transactions;
	}

	@Override
	public List<Transaction> getTransactionList() {
		return transactions;
	}

	@Override
	public JavaRDD<Transaction> getTransactionRDD() {
		return null;
	}

	@Override
	public int size() {
		return transactions.size();
	}

}
