package itemsetmining.itemset;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;

import com.clearspring.analytics.util.Lists;

public class Sequence extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = -2766830126344921771L;

	/**
	 * Constructor
	 */
	public Sequence() {
		this.itemsets = Lists.newArrayList();
	}

	/**
	 * Constructor
	 *
	 * @param itemsets
	 *            a list of itemsets that should be added to the new sequence
	 */
	public Sequence(final AbstractList<Itemset> itemsets) {
		this.itemsets = Lists.newArrayList(itemsets);
	}

	/**
	 * Constructor
	 *
	 * @param itemsets
	 *            an array of itemsets that should be added to the new sequence
	 */
	public Sequence(final Itemset... itemsets) {
		this.itemsets = Lists.newArrayList(Arrays.asList(itemsets));
	}

	/**
	 * Join two Sequence Codes if they overlap, returning null if not
	 *
	 * @see <a href="http://tinyurl.com/lfuuj3o">http://tinyurl.com/lfuuj3o</a>
	 *
	 * @return overlapping Sequence Code or null if no overlap
	 */
	public static String join(final String s1, final String s2) {
		final int len1 = s1.length() - 1;
		final char last1 = s1.charAt(len1);
		final char first2 = s2.charAt(0);

		// Find the first potential match, bounded by the length of s1
		int indexOfLast2 = s2.lastIndexOf(last1,
				Math.min(len1, s2.length() - 1));
		while (indexOfLast2 != -1) {
			if (s1.charAt(len1 - indexOfLast2) == first2) {
				// After the quick check, do a full check
				int ix = indexOfLast2;
				while ((ix != -1)
						&& (s1.charAt(len1 - indexOfLast2 + ix) == s2
								.charAt(ix)))
					ix--;
				if (ix == -1)
					return s1 + s2.substring(indexOfLast2 + 1);
			}
			// Search for the next possible match
			indexOfLast2 = s2.lastIndexOf(last1, indexOfLast2 - 1);
		}

		// No match found
		return null;
	}

	/**
	 * Convert this sequence to a String code
	 */
	public String toCode() {
		final StringBuilder sb = new StringBuilder(this.size() * 2);
		String prefixSet = "";
		for (final Itemset set : this.itemsets) {
			sb.append(prefixSet);
			String prefix = "";
			for (final int item : set) {
				sb.append(prefix);
				prefix = ",";
				sb.append(item);
			}
			prefixSet = ";";
		}
		return sb.toString();
	}

	/**
	 * Convert given String code to a Sequence
	 */
	public static Sequence fromCode(final String code) {
		final Sequence seq = new Sequence();
		for (final String itemset : code.split(";")) {
			final Itemset set = new Itemset();
			for (final String item : itemset.split(",")) {
				set.add(Integer.parseInt(item));
			}
			seq.add(set);
		}
		return seq;
	}

}
