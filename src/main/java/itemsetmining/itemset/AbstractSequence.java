package itemsetmining.itemset;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractSequence extends AbstractCollection<Integer>
		implements Serializable {
	private static final long serialVersionUID = 686688001826219278L;

	protected List<Integer> items;

	/**
	 * Add given items to this sequence
	 *
	 * @param items
	 *            an item that should be added to this sequence
	 */
	@Override
	public boolean add(final Integer item) {
		return this.items.add(item);
	}

	/**
	 * Get item at specified position in this sequence
	 *
	 * @param index
	 *            index of the element to return
	 */
	public int get(final int index) {
		return this.items.get(index);
	}

	/**
	 * Add item to this sequence
	 *
	 * @param items
	 *            a collection of items that should be added to this sequence
	 */
	@Override
	public boolean addAll(final Collection<? extends Integer> items) {
		return this.items.addAll(items);
	}

	/**
	 * Add items to this sequence
	 *
	 * @param items
	 *            an array of items that should be added to this sequence
	 */
	public void add(final Integer... items) {
		for (final Integer set : items)
			this.items.add(set);
	}

	/**
	 * Check if this sequence contains given sequence
	 *
	 * @param sequence
	 */
	public boolean contains(final AbstractSequence seq) {
		int pos = 0;
		boolean containsItem;
		for (final Integer item : seq.items) {
			containsItem = false;
			for (int i = pos; i < this.items.size(); i++) {
				if (this.items.get(i) == item) {
					pos = i + 1;
					containsItem = true;
					break;
				}
			}
			if (!containsItem)
				return false;
		}
		return true;
	}

	/**
	 * Check if first BitSet contains second BitSet
	 */
	public boolean contains(final BitSet set1, final BitSet set2) {
		final BitSet copy = (BitSet) set2.clone();
		copy.and(set1);
		return copy.equals(set2);
	}

	/**
	 * Return the items in this sequence covered by the given sequence, allowing
	 * for multiple covering matches if the first match is already fully covered
	 *
	 * <p>
	 * This is intended to allow the covering of 1 2 1 2 1 2 by 1 2.
	 *
	 * @param sequence
	 * @return BitSet of items in order with the covered items set true
	 */
	public BitSet getCovered(final AbstractSequence seq,
			final BitSet alreadyCoveredItems) {

		int index = 0;
		while (true) {
			final BitSet coveredItems = getCovered(seq, index);
			if (coveredItems.isEmpty())
				return coveredItems;
			if (contains(alreadyCoveredItems, coveredItems))
				index = coveredItems.nextSetBit(index) + 1;
			else
				return coveredItems;
		}

	}

	/**
	 * Return the items in this sequence covered by the given sequence
	 *
	 * @param sequence
	 * @return BitSet of items in order with the covered items set true
	 */
	// TODO remove containsItem check? This should always be true...
	public BitSet getCovered(final AbstractSequence seq, final int startIndex) {
		int pos = startIndex;
		boolean containsItem;
		final BitSet coveredItems = new BitSet(this.size());
		for (final Integer item : seq.items) {
			containsItem = false;
			for (int i = pos; i < this.items.size(); i++) {
				if (this.items.get(i) == item) {
					coveredItems.set(i);
					pos = i + 1;
					containsItem = true;
					break;
				}
			}
			if (!containsItem) {
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
		return this.items.size();
	}

	@Override
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
		if (!(obj instanceof AbstractSequence))
			return false;
		final AbstractSequence other = (AbstractSequence) obj;
		return items.equals(other.items);
	}

	@Override
	public Iterator<Integer> iterator() {
		return items.iterator();
	}

}
