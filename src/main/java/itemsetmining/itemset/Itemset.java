package itemsetmining.itemset;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;

public class Itemset extends AbstractItemset implements Serializable {
	private static final long serialVersionUID = 4667217256957834826L;

	/**
	 * Constructor
	 */
	public Itemset() {
		this.items = new BitSet();
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            a collection of items that should be added to the new itemset
	 */
	public Itemset(final Collection<Integer> items) {
		this.items = new BitSet(items.size());
		addAll(items);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            an array of items that should be added to the new itemset
	 */
	public Itemset(final int... items) {
		this.items = new BitSet(items.length);
		add(items);
	}

}
