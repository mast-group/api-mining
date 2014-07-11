package itemsetmining.transaction;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;

/** Wrapper class for storing a database of transactions */
public abstract class TransactionDatabase {

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

	/** Get a list of transactions */
	public abstract List<Transaction> getTransactionList();

	/** Get a JavaRDD of transactions */
	public abstract JavaRDD<Transaction> getTransactionRDD();

	/** Update the transaction cache */
	public abstract void updateTransactionCache(
			final JavaRDD<Transaction> updatedTransactions);

	/** Get the number of transactions in this database */
	public abstract long size();

}
