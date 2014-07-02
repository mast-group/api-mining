package itemsetmining.itemset;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractItemset implements Iterable<Integer> {

	/** the set of items **/
	protected BitSet items;

	/**
	 * Add given itemset to this itemset
	 * 
	 * @param items
	 *            an itemset that should be added to this itemset
	 */
	public void add(final AbstractItemset set) {
		final BitSet bs = set.items;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
			this.items.set(i);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            a collection of items that should be added to this itemset
	 */
	public void add(final Collection<Integer> items) {
		for (final int item : items)
			this.items.set(item);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            an array of items that should be added to this itemset
	 */
	public void add(final int... items) {
		for (final int item : items)
			this.items.set(item);
	}

	/**
	 * Check if item is contained in this itemset
	 */
	public boolean contains(final int item) {
		return items.get(item);
	}

	/**
	 * Check if this itemset contains given itemset
	 * 
	 * @param items
	 */
	public boolean contains(final AbstractItemset set) {
		final BitSet setItems = set.items;
		final BitSet copy = (BitSet) setItems.clone();
		copy.and(items);
		return copy.equals(setItems);
	}

	/**
	 * Count items contained in the union of this itemset and given itemset
	 * 
	 * @param itemset
	 */
	public int countUnion(final AbstractItemset set) {
		final BitSet setItems = set.items;
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

	public boolean intersects(final AbstractItemset set) {
		return items.intersects(set.items);
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
		if (!(obj instanceof AbstractItemset))
			return false;
		final AbstractItemset other = (AbstractItemset) obj;
		return items.equals(other.items);
	}

	@Override
	public Iterator<Integer> iterator() {
		return new BitSetIterator(items);
	}

}
