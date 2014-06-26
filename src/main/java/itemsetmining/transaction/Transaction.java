package itemsetmining.transaction;

import itemsetmining.itemset.AbstractItemset;

import java.io.Serializable;
import java.util.BitSet;
import java.util.List;

import com.google.common.collect.Lists;

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

	/**
	 * Get the transaction items as a List
	 * 
	 * @return the items
	 * @deprecated slow
	 */
	@Deprecated
	public List<Integer> getItems() {
		final List<Integer> listItems = Lists.newArrayList();
		for (int i = items.nextSetBit(0); i >= 0; i = items.nextSetBit(i + 1))
			listItems.add(i);
		return listItems;
	}

}