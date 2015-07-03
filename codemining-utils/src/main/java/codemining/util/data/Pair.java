/**
 * 
 */
package codemining.util.data;

import com.google.common.base.Objects;

/**
 * A generic unordered pair struct. Since pairs are bad because the
 * "first,second" do not convey semantics, users of that class may choose to
 * wrap this class into a new name and provide accessors to first and second.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class Pair<T1, T2> {
	public static <T1, T2> Pair<T1, T2> create(final T1 first, final T2 second) {
		return new Pair<T1, T2>(first, second);
	}

	public final T1 first;

	public final T2 second;

	protected Pair(final T1 firstValue, final T2 secondValue) {
		first = firstValue;
		second = secondValue;
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
		final Pair<T1, T2> other = (Pair<T1, T2>) obj;
		return Objects.equal(first, other.first)
				&& Objects.equal(second, other.second);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(first, second);
	}

	@Override
	public String toString() {
		return "Pair<" + first.toString() + "," + second.toString() + ">";
	}
}
