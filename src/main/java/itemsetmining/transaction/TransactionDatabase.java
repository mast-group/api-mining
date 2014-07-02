package itemsetmining.transaction;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;

/** Wrapper class for storing a database of transactions */
public abstract class TransactionDatabase {

	/** Get a list of transactions */
	public abstract List<Transaction> getTransactionList();

	/** Get a JavaRDD of transactions */
	public abstract JavaRDD<Transaction> getTransactionRDD();

	/** Get the number of transactions in this database */
	public abstract long size();

}
