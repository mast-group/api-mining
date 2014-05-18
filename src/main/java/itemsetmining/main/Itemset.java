package itemsetmining.main;

import java.util.HashSet;

import com.google.common.collect.Sets;

// TODO implement equals and hashcode?
public class Itemset {

	/** the set of items **/
	private HashSet<Integer> itemset = Sets.newHashSet();

	/** the support of this itemset */
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
	 *            an array of items that should be added to the new itemset
	 */
	public Itemset(int... items) {
		add(items);
	}

	/**
	 * Add items to this itemset
	 * 
	 * @param items
	 *            an array of items that should be added to this itemset
	 */
	public void add(int... items) {
		for (int item : items)
			itemset.add(item);
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
	public void setSupport(int support) {
		this.support = support;
	}

	@Override
	public String toString() {
		return itemset.toString();
	}

}
