package itemsetmining.itemset;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

public class Itemset implements Serializable {
	private static final long serialVersionUID = 4667217256957834826L;

	/** the set of items **/
	private final BitSet itemset;

	/**
	 * Get the items as a Set
	 * 
	 * @return the items
	 * @deprecated slow
	 */
	@Deprecated
	public Set<Integer> getItems() {
		final Set<Integer> listItems = Sets.newHashSet();
		for (int i = itemset.nextSetBit(0); i >= 0; i = itemset
				.nextSetBit(i + 1))
			listItems.add(i);
		return listItems;
	}

	/**
	 * Get the items as a BitSet
	 * 
	 * @return the items
	 */
	public BitSet getBitSet() {
		return itemset;
	}

	/**
	 * Constructor
	 */
	public Itemset() {
		itemset = new BitSet();
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            a collection of items that should be added to the new itemset
	 */
	public Itemset(final Collection<Integer> items) {
		itemset = new BitSet(items.size());
		add(items);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            an array of items that should be added to the new itemset
	 */
	public Itemset(final int... items) {
		itemset = new BitSet(items.length);
		add(items);
	}

	/**
	 * Add given itemset to this itemset
	 * 
	 * @param itemset
	 *            an itemset that should be added to this itemset
	 */
	public void add(final Itemset set) {
		final BitSet bs = set.getBitSet();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
			this.itemset.set(i);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            a collection of items that should be added to this itemset
	 */
	public void add(final Collection<Integer> items) {
		for (final int item : items)
			itemset.set(item);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            an array of items that should be added to this itemset
	 */
	public void add(final int... items) {
		for (final int item : items)
			itemset.set(item);
	}

	/**
	 * Check if item is contained in this itemset
	 */
	public boolean contains(final int item) {
		return itemset.get(item);
	}

	/**
	 * Check if this itemset contains given itemset
	 * 
	 * @param itemset
	 */
	public boolean contains(final Itemset set) {
		final BitSet setItems = set.getBitSet();
		final BitSet copy = (BitSet) setItems.clone();
		copy.and(itemset);
		return copy.equals(setItems);
	}

	/**
	 * Number of items in this transaction
	 */
	public int size() {
		return itemset.cardinality();
	}

	public boolean isEmpty() {
		return itemset.isEmpty();
	}

	public boolean intersects(final Itemset set) {
		return itemset.intersects(set.itemset);
	}

	@Override
	public String toString() {
		return itemset.toString();
	}

	@Override
	public int hashCode() {
		return itemset.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Itemset))
			return false;
		final Itemset other = (Itemset) obj;
		return itemset.equals(other.itemset);
	}

}
