package itemsetmining.transaction;

import itemsetmining.itemset.AbstractItemset;

import java.io.Serializable;
import java.util.BitSet;

/** A transaction is an ordered list of items */
public class Transaction extends AbstractItemset implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/**
	 * Constructor
	 */
	public Transaction() {
		this.items = new BitSet();
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            a collection of items that should be added to the transaction
	 */
	public Transaction(final int... items) {
		this.items = new BitSet(items.length);
		add(items);
	}

}