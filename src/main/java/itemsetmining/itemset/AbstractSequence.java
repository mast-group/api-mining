package itemsetmining.itemset;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractSequence extends AbstractCollection<Itemset>
		implements Serializable {
	private static final long serialVersionUID = 686688001826219278L;

	protected List<Itemset> itemsets;

	/**
	 * Add given itemset to this sequence
	 *
	 * @param itemset
	 *            an itemset that should be added to this sequence
	 * @return
	 */
	@Override
	public boolean add(final Itemset set) {
		return this.itemsets.add(set);
	}

	/**
	 * Add itemsets to this sequence
	 *
	 * @param itemsets
	 *            a collection of itemsets that should be added to this sequence
	 */
	@Override
	public boolean addAll(final Collection<? extends Itemset> itemsets) {
		return this.itemsets.addAll(itemsets);
	}

	/**
	 * Add items to this itemset
	 *
	 * @param items
	 *            an array of items that should be added to this itemset
	 */
	public void add(final Itemset... itemsets) {
		for (final Itemset set : itemsets)
			this.itemsets.add(set);
	}

	/**
	 * Check if this sequence contains given sequence
	 *
	 * @param sequence
	 */
	public boolean contains(final AbstractSequence seq) {
		int pos = 0;
		boolean containsSet;
		for (final Itemset set : seq.itemsets) {
			containsSet = false;
			for (int i = pos; i < this.itemsets.size(); i++) {
				if (this.itemsets.get(i).contains(set)) {
					pos = i + 1;
					containsSet = true;
					break;
				}
			}
			if (!containsSet)
				return false;
		}
		return true;
	}

	/**
	 * Return the items in this sequence covered by the given sequence
	 *
	 * @param sequence
	 * @return BitSet of items in order with the covered items set true
	 */
	// TODO remove containsSet check? This should always be true...
	public BitSet getCovered(final AbstractSequence seq) {
		int pos = 0;
		int itemPos = 0;
		boolean containsSet;
		final BitSet coveredItems = new BitSet(this.size());
		for (final Itemset set : seq.itemsets) {
			containsSet = false;
			for (int i = pos; i < this.itemsets.size(); i++) {
				final Itemset thisSet = this.itemsets.get(i);
				if (thisSet.contains(set)) {
					for (final int item : thisSet) {
						if (set.contains(item))
							coveredItems.set(itemPos);
						itemPos++;
					}
					pos = i + 1;
					containsSet = true;
					break;
				} else {
					itemPos += thisSet.size();
				}
			}
			if (!containsSet) {
				coveredItems.clear();
				return coveredItems;
			}
		}
		return coveredItems;
	}

	/**
	 * Number of items in this sequence
	 */
	@Override
	public int size() {
		int noItems = 0;
		for (final Itemset set : itemsets)
			noItems += set.size();
		return noItems;
	}

	@Override
	public boolean isEmpty() {
		return itemsets.isEmpty();
	}

	@Override
	public String toString() {
		return itemsets.toString();
	}

	@Override
	public int hashCode() {
		return itemsets.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AbstractSequence))
			return false;
		final AbstractSequence other = (AbstractSequence) obj;
		return itemsets.equals(other.itemsets);
	}

	@Override
	public Iterator<Itemset> iterator() {
		return itemsets.iterator();
	}

}
