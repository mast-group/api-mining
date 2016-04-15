package sequencemining.transaction;

import java.util.List;

/** Wrapper class for storing a database of transactions */
public abstract class TransactionDatabase {

	/** Set to true if candidate generation iteration limit exceeded */
	private boolean iterationLimitExceeded = false;

	/** Average cost across the transactions */
	private double averageCost = Double.POSITIVE_INFINITY;

	/** Set the average cost */
	public void setAverageCost(final double averageCost) {
		this.averageCost = averageCost;
	}

	/** Get the average cost */
	public double getAverageCost() {
		return averageCost;
	}

	public void setIterationLimitExceeded() {
		iterationLimitExceeded = true;
	}

	public boolean getIterationLimitExceeded() {
		return iterationLimitExceeded;
	}

	/** Get a list of transactions */
	public abstract List<Transaction> getTransactionList();

	// /** Get a JavaRDD of transactions */
	// public abstract JavaRDD<Transaction> getTransactionRDD();
	//
	// /** Update the transaction cache */
	// public abstract void updateTransactionCache(
	// final JavaRDD<Transaction> updatedTransactions);

	/** Get the number of transactions in this database */
	public abstract long size();

}
