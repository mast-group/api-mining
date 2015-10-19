package sequencemining.transaction;

import java.util.List;

/** Wrapper class for storing transaction database as a list of transactions */
public class TransactionList extends TransactionDatabase {

	private final List<Transaction> transactions;
	private final String[] cachedDB;

	public TransactionList(final List<Transaction> transactions,
			final String[] cachedDB) {
		this.transactions = transactions;
		this.cachedDB = cachedDB;
	}

	@Override
	public List<Transaction> getTransactionList() {
		return transactions;
	}

	// @Override
	// public JavaRDD<Transaction> getTransactionRDD() {
	// throw new UnsupportedOperationException("This is a list is not a RDD!!");
	// }

	@Override
	public long size() {
		return transactions.size();
	}

	// @Override
	// public void updateTransactionCache(
	// final JavaRDD<Transaction> updatedTransactions) {
	// throw new UnsupportedOperationException("This is a list is not a RDD!!");
	// }

	@Override
	public String[] getCachedDB() {
		return cachedDB;
	}

}
