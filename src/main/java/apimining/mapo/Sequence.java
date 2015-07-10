package apimining.mapo;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Sequence extends AbstractCollection<Integer>implements Serializable {
	private static final long serialVersionUID = -2550493924277561085L;

	private final List<Integer> items;

	/**
	 * Constructor
	 */
	public Sequence() {
		this.items = new ArrayList<>();
	}

	/**
	 * Shallow Copy Constructor (with default occurrence)
	 *
	 * @param seq
	 *            sequence to shallow copy
	 */
	public Sequence(final Sequence seq) {
		this.items = seq.items;
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            a list of items that should be added to the new sequence
	 */
	public Sequence(final List<Integer> items) {
		this.items = new ArrayList<>(items);
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            an array of items that should be added to the new sequence
	 */
	public Sequence(final Integer... items) {
		this.items = new ArrayList<>(Arrays.asList(items));
	}

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
	 * Get the items in this sequence
	 *
	 * @return the items
	 */
	public List<Integer> getItems() {
		return this.items;
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
		if (!(obj instanceof Sequence))
			return false;
		final Sequence other = (Sequence) obj;
		return items.equals(other.items);
	}

	@Override
	public Iterator<Integer> iterator() {
		return items.iterator();
	}

}
