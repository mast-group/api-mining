package itemsetmining.itemset;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.Sets;

public class Itemset implements Serializable {
	private static final long serialVersionUID = 4667217256957834826L;

	/** the set of items **/
	private final HashSet<Integer> itemset = Sets.newHashSet();

	/** the support of this itemset */
	// TODO remove support from Itemset class?
	private int support = -1;

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

	/**
	 * Get the support of this itemset
	 */
	public int getSupport() {
		return support;
	}

	/**
	 * Set the support of this itemset
	 * 
	 * @param support
	 *            the support
	 */
	public void setSupport(final int support) {
		this.support = support;
	}

	@Override
	public String toString() {
		return itemset.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemset == null) ? 0 : itemset.hashCode());
		result = prime * result + support;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Itemset other = (Itemset) obj;
		if (itemset == null) {
			if (other.itemset != null)
				return false;
		} else if (!itemset.equals(other.itemset))
			return false;
		if (support != other.support)
			return false;
		return true;
	}

}
