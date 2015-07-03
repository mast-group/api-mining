/**
 * 
 */
package codemining.util.data;

import com.google.common.base.Objects;

/**
 * An unordered pair (order of the elements does not matter).
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class UnorderedPair<T1> extends Pair<T1, T1> {

	public static <T1> UnorderedPair<T1> createUnordered(final T1 first,
			final T1 second) {
		return new UnorderedPair<T1>(first, second);
	}

	private UnorderedPair(final T1 first, final T1 second) {
		super(first, second);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final UnorderedPair<T1> other = (UnorderedPair<T1>) obj;
		return (Objects.equal(first, other.first) && Objects.equal(second,
				other.second))
				|| (Objects.equal(second, other.first) && Objects.equal(first,
						other.second));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(first, second)
				+ Objects.hashCode(second, first);
	}

}
