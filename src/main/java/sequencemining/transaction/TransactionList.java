package sequencemining.transaction;

import java.util.List;

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
	public long size() {
		return transactions.size();
	}

}
