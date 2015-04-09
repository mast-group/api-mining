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
	 *            two overlapping sequences that should be joined
	 * @param index
	 *            index of join point in second sequence
	 */
	public Sequence(final Sequence seq1, final Sequence seq2, final int index) {
		this.items = Lists.newArrayList(seq1.items);
		this.items.addAll(seq2.items.subList(index, seq2.size()));
	}

	/**
	 * Returns the index of the last occurrence of the specified item, searching
	 * backward starting at the specified index or -1 if not present.
	 *
	 * @param item
	 *            item to search for
	 * @param fromIndex
	 *            the index to start the search from.
	 */
	public int lastIndexOf(final int item, final int fromIndex) {
		return this.items.subList(0, fromIndex).lastIndexOf(item);
	}

	/**
	 * Join two Sequences if they overlap, returning null if not
	 *
	 * @see <a href="http://tinyurl.com/lfuuj3o">http://tinyurl.com/lfuuj3o</a>
	 *
	 * @return overlapping Sequence or null if no overlap
	 */
	public static Sequence join(final Sequence s1, final Sequence s2) {
		final int len1 = s1.size() - 1;
		final int len2 = s2.size() - 1;

		// Handle singletons by just joining them
		if (len1 == 0 && len2 == 0)
			return new Sequence(s1, s2, 0);

		// Otherwise detect overlap
		final int last1 = s1.get(len1);
		final int first2 = s2.get(0);

		// Find the first potential match, bounded by the length of s1
		int indexOfLast2 = s2.lastIndexOf(last1, Math.min(len1, s2.size() - 1));
		while (indexOfLast2 != -1) {
			if (s1.get(len1 - indexOfLast2) == first2) {
				// After the quick check, do a full check
				int ix = indexOfLast2;
				while ((ix != -1)
						&& (s1.get(len1 - indexOfLast2 + ix) == s2.get(ix)))
					ix--;
				if (ix == -1) {
					return new Sequence(s1, s2, indexOfLast2 + 1);
				}
			}
			// Search for the next possible match
			indexOfLast2 = s2.lastIndexOf(last1, indexOfLast2 - 1);
		}

		// No match found
		return null;
	}

}
