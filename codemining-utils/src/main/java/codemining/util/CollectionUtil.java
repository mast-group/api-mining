/**
 *
 */
package codemining.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * A utility class containing collection-related utilities.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public final class CollectionUtil {

	/**
	 * Return the elements that have been seen at least nSeen times.
	 *
	 * @param nSeen
	 * @param baseMultiset
	 * @return
	 */
	public static <T> Set<T> getElementsUpToCount(final int nSeen,
			final Multiset<T> baseMultiset) {
		checkArgument(nSeen > 0);
		final Set<T> toKeep = Sets.newHashSet();
		for (final Entry<T> entry : Multisets.copyHighestCountFirst(
				checkNotNull(baseMultiset)).entrySet()) {
			if (entry.getCount() < nSeen) {
				break;
			}
			toKeep.add(entry.getElement());
		}
		return toKeep;
	}

	/**
	 * Return the elements that have been seen at most nSeen times.
	 *
	 * @param nSeen
	 * @param baseMultiset
	 * @return
	 */
	public static <T> Set<T> getElementsWithLessThanCount(final int nSeen,
			final Multiset<T> baseMultiset) {
		checkArgument(nSeen > 0);
		final Set<T> toKeep = Sets.newHashSet();
		for (final Entry<T> entry : checkNotNull(baseMultiset).entrySet()) {
			if (entry.getCount() < nSeen) {
				toKeep.add(entry.getElement());
			}
		}
		return toKeep;
	}

	/**
	 * Return the top elements of a sorted set.
	 *
	 * @param originalSet
	 * @param nTopElements
	 * @return
	 */
	public static <T extends Comparable<T>> SortedSet<T> getTopElements(
			final SortedSet<T> originalSet, final int nTopElements) {
		final SortedSet<T> filteredElements = Sets.newTreeSet();
		int i = 0;
		for (final T element : originalSet) {
			if (i >= nTopElements) {
				break;
			}
			filteredElements.add(element);
			i++;
		}
		return filteredElements;
	}

	/** Sort map by value (lowest first) */
	public static <K extends Comparable<K>, V extends Comparable<V>> Map<K, V> sortMapByValueAscending(
			final Map<K, V> map) {
		final Ordering<K> valueThenKeyComparator = Ordering.natural()
				.onResultOf(Functions.forMap(map))
				.compound(Ordering.<K> natural());
		return ImmutableSortedMap.copyOf(map, valueThenKeyComparator);
	}

	/** Sort map by value (highest first) */
	public static <K extends Comparable<K>, V extends Comparable<V>> Map<K, V> sortMapByValueDescending(
			final Map<K, V> map) {
		final Ordering<K> valueThenKeyComparator = Ordering.natural().reverse()
				.onResultOf(Functions.forMap(map))
				.compound(Ordering.<K> natural().reverse());
		return ImmutableSortedMap.copyOf(map, valueThenKeyComparator);
	}

}
