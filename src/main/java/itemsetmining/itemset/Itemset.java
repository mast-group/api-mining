package itemsetmining.itemset;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.Sets;

public class Itemset implements Serializable {
	private static final long serialVersionUID = 4667217256957834826L;

	/** the set of items **/
	private final HashSet<Integer> itemset = Sets.newHashSet();

	/**
	 * Get the items as array
	 * 
	 * @return the items
	 */
	public HashSet<Integer> getItems() {
		return itemset;
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            a collection of items that should be added to the new itemset
	 */
	public Itemset(final Collection<Integer> items) {
		add(items);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            an array of items that should be added to the new itemset
	 */
	public Itemset(final int... items) {
		add(items);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            a collection of items that should be added to this itemset
	 */
	public void add(final Collection<Integer> items) {
		itemset.addAll(items);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            an array of items that should be added to this itemset
	 */
	public void add(final int... items) {
		for (final int item : items)
			itemset.add(item);
	}

	public boolean isEmpty() {
		return itemset.isEmpty();
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
