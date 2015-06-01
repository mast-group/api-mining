package itemsetmining.itemset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sequence extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = -2766830126344921771L;

	/** Label denoting the seq occurrence in the transaction */
	private int occurrence = 1; // (default is first occurrence)

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
	 * Increment which occurrence this sequence is
	 */
	public void incrementOccurence() {
		this.occurrence++;
	}

	public int getOccurence() {
		return occurrence;
	}

	/**
	 * Join Constructor (naturally uses minimum occurrence)
	 *
	 * @param seqs
	 *            two sequences that should be joined
	 */
	public Sequence(final Sequence seq1, final Sequence seq2) {
		this.items = new ArrayList<>(seq1.items);
		this.items.addAll(seq2.items);
		this.occurrence = Math.min(seq1.occurrence, seq2.occurrence);
	}

	@Override
	public String toString() {
		String suffix = "";
		if (occurrence > 1)
			suffix = "^(" + occurrence + ")";
		return items.toString() + suffix;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		result = prime * result + occurrence;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Sequence))
			return false;
		final Sequence other = (Sequence) obj;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		if (occurrence != other.occurrence)
			return false;
		return true;
	}

	@Override
	public boolean contains(final Sequence seq) {
		if (this.occurrence == 1 && seq.occurrence == 1)
			return super.contains(seq);
		throw new UnsupportedOperationException();
	}

}
