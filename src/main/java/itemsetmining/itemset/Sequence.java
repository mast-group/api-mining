package itemsetmining.itemset;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.clearspring.analytics.util.Lists;

public class Sequence extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = -2766830126344921771L;

	/**
	 * Constructor
	 */
	public Sequence() {
		this.items = Lists.newArrayList();
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            a list of items that should be added to the new sequence
	 */
	public Sequence(final List<Integer> items) {
		this.items = Lists.newArrayList(items);
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            an array of items that should be added to the new sequence
	 */
	public Sequence(final Integer... items) {
		this.items = Lists.newArrayList(Arrays.asList(items));
	}

	/**
	 * Join Constructor
	 *
	 * @param lists
	 *            two sequences that should be joined
	 */
	public Sequence(final Sequence seq1, final Sequence seq2) {
		this.items = Lists.newArrayList(seq1.items);
		this.items.addAll(seq2.items);
	}

}
