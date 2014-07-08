package itemsetmining.itemset;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

public abstract class AbstractItemset extends AbstractCollection<Integer>
		implements Serializable {
	private static final long serialVersionUID = -6482941473374203517L;

	/** the set of items **/
	protected BitSet items;

	/**
	 * Get the items as a Set
	 * 
	 * @return the items
	 * @deprecated slow
	 */
	@Deprecated
	public Set<Integer> getItems() {
		final Set<Integer> listItems = Sets.newHashSet();
		for (int i = items.nextSetBit(0); i >= 0; i = items.nextSetBit(i + 1))
			listItems.add(i);
		return listItems;
	}

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
	 * Add an item to this itemset
	 * 
	 * @param item
	 *            an item that should be added to this itemset
	 * @return
	 */
	@Override
	public boolean add(final Integer item) {
		this.items.set(item);
		return true;
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            a collection of items that should be added to this itemset
	 */
	@Override
	public boolean addAll(final Collection<? extends Integer> items) {
		for (final int item : items)
			this.items.set(item);
		return true;
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
	@Override
	public int size() {
		return items.cardinality();
	}

	@Override
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
