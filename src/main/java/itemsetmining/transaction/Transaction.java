package itemsetmining.transaction;

import itemsetmining.itemset.Itemset;

import java.util.BitSet;
import java.util.List;

import com.google.common.collect.Lists;

/** A transaction is an ordered list of items */
public class Transaction {

	private final BitSet items;

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
	 * Add an itemset to this transaction
	 * 
	 * @param itemset
	 *            an itemset that should be added to this transaction
	 */
	public void add(final Itemset set) {
		final BitSet bs = set.getBitSet();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
			this.items.set(i);
	}

	/**
	 * Add items to this transaction
	 * 
	 * @param items
	 *            a collection of items that should be added to this transaction
	 */
	public void add(final int... items) {
		for (final int item : items)
			this.items.set(item);
	}

	/**
	 * Get the items as a List
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

	/**
	 * Check if the transaction contains given itemset
	 */
	public boolean contains(final int item) {
		return items.get(item);
	}

	/**
	 * Check if the transaction contains given itemset
	 * 
	 * @param itemset
	 */
	public boolean contains(final Itemset set) {
		final BitSet setItems = set.getBitSet();
		final BitSet copy = (BitSet) setItems.clone();
		copy.and(items);
		return copy.equals(setItems);
	}

	/**
	 * Count items contained in the union of transaction and given itemset
	 * 
	 * @param itemset
	 */
	public int countUnion(final Itemset set) {
		final BitSet setItems = set.getBitSet();
		final BitSet copy = (BitSet) setItems.clone();
		copy.or(items);
		return copy.cardinality();
	}

	/**
	 * Number of items in this transaction
	 */
	public int size() {
		return items.cardinality();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	@Override
	public String toString() {
		return items.toString();
	}

	@Override
	public int hashCode() {
		return items.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Transaction))
			return false;
		final Transaction other = (Transaction) obj;
		return items.equals(other.items);
	}

}