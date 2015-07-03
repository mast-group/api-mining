/**
 * Various useful statistics utilities
 *
 * @author Jaroslav Fowkes
 */
package codemining.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

public final class StatsUtil {

	/**
	 * Return the max idx of the array.
	 *
	 * @param array
	 * @return
	 */
	public static Optional<Integer> argmax(final double[] array) {
		double max = Double.NEGATIVE_INFINITY;
		int maxIdx = -1;
		for (int i = 0; i < array.length; i++) {
			if (max < array[i]) {
				max = array[i];
				maxIdx = i;
			}
		}
		if (maxIdx == -1) {
			return Optional.absent();
		} else {
			return Optional.of(maxIdx);
		}
	}

	/**
	 * Return the element with the maximum value in the map.
	 *
	 * @param valuedObjects
	 * @return
	 */
	public static <T> Optional<T> argmax(final Map<T, Double> valuedObjects) {
		double max = Double.NEGATIVE_INFINITY;
		T maxElement = null;
		for (final Entry<T, Double> entry : valuedObjects.entrySet()) {
			if (max < entry.getValue()) {
				max = entry.getValue();
				maxElement = entry.getKey();
			}
		}
		if (maxElement != null) {
			return Optional.of(maxElement);
		} else {
			return Optional.absent();
		}
	}

	/**
	 * Average an int array elements with divisor.
	 *
	 * @param array
	 * @param divisor
	 * @return
	 */
	public static double[] divideArrayElements(final int[] array,
			final int divisor) {
		final double[] averaged = new double[array.length];
		for (int i = 0; i < averaged.length; i++) {
			averaged[i] = ((double) array[i]) / divisor;
		}
		return averaged;
	}

	/**
	 * Code ported from LingPipe This method returns the log of the sum of the
	 * natural exponentiated values in the specified array. Mathematically, the
	 * result is
	 *
	 * <blockquote>
	 *
	 * <pre>
	 * logSumOfExponentials(xs) = log <big><big>( &Sigma;</big></big><sub>i</sub> exp(xs[i]) <big><big>)</big></big>
	 * </pre>
	 *
	 * </blockquote>
	 *
	 * But the result is not calculated directly. Instead, the calculation
	 * performed is:
	 *
	 * <blockquote>
	 *
	 * <pre>
	 * logSumOfExponentials(xs) = max(xs) + log <big><big>( &Sigma;</big></big><sub>i</sub> exp(xs[i] - max(xs)) <big><big>)</big></big>
	 * </pre>
	 *
	 * </blockquote>
	 *
	 * which produces the same result, but is much more arithmetically stable,
	 * because the largest value for which <code>exp()</code> is calculated is
	 * 0.0.
	 *
	 * <p>
	 * Values of {@code Double.NEGATIVE_INFINITY} are treated as having
	 * exponentials of 0 and logs of negative infinity. That is, they are
	 * ignored for the purposes of this computation.
	 *
	 * @param values
	 *            Array of values.
	 * @return The log of the sum of the exponentiated values in the array.
	 */
	public static double log2SumOfExponentials(final Collection<Double> values) {
		if (values.size() == 1) {
			return values.iterator().next();
		}
		final double max = max(values);
		double sum = 0.0;
		for (final double value : values) {
			if (value != Double.NEGATIVE_INFINITY) {
				sum += Math.pow(2, value - max);
			}
		}
		return max + DoubleMath.log2(sum);
	}

	public static double log2SumOfExponentials(final double... values) {
		if (values.length == 1) {
			return values[0];
		}
		final double max = max(values);
		double sum = 0.0;
		for (final double value : values) {
			if (value != Double.NEGATIVE_INFINITY) {
				sum += Math.pow(2, value - max);
			}
		}
		return max + DoubleMath.log2(sum);
	}

	public static double log2SumOfExponentials(final double log2Prob1,
			final double log2Prob2) {
		final double max;
		final double min;
		if (log2Prob1 > log2Prob2) {
			max = log2Prob1;
			min = log2Prob2;
		} else {
			max = log2Prob2;
			min = log2Prob1;
		}
		final double diff = min - max;
		if (diff < -54) {
			// 1. + Math.pow(2, diff) would return 1 and thus we avoid the
			// computation
			return max;
		} else {
			return max + (Math.log1p(Math.pow(2, diff)) / LN_2);
		}
	}

	/**
	 * Calculates the max of an Array
	 */
	public static double max(final double... array) {
		double max = Double.NEGATIVE_INFINITY;
		for (final double value : array) {
			if (max < value) {
				max = value;
			}
		}
		return max;
	}

	/**
	 * Retrieve the max element
	 *
	 * @param values
	 * @return
	 */
	public static Double max(final Iterable<Double> values) {
		Double max = Double.NEGATIVE_INFINITY;
		for (final Double value : values) {
			if (max < value) {
				max = value;
			}
		}
		return max;
	}

	/**
	 * Calculates the mean of a Collection
	 */
	public static double mean(final Collection<Double> values) {
		return sum(values) / values.size();
	}

	/**
	 * Calculates the median of a List
	 */
	public static double median(final List<Double> values) {

		Collections.sort(values);

		final int middle = values.size() / 2;
		if (values.size() % 2 == 1) {
			return values.get(middle);
		} else {
			return (values.get(middle - 1) + values.get(middle)) / 2.0;
		}

	}

	/**
	 * Retrieve the min element
	 *
	 * @param xs
	 * @return
	 */
	public static double min(final Collection<Double> xs) {
		double min = Double.POSITIVE_INFINITY;
		for (final double value : xs) {
			if (min > value) {
				min = value;
			}
		}
		return min;
	}

	/**
	 * Calculates the min of an Array
	 */
	public static double min(final double... array) {
		double min = Double.POSITIVE_INFINITY;
		for (final double value : array) {
			if (min > value) {
				min = value;
			}
		}
		return min;
	}

	/**
	 * Calculates the mode of a Collection
	 */
	public static double mode(final Collection<Double> values) {

		double maxValue = 0;
		int maxCount = 0;

		for (final Double elementA : values) {
			int count = 0;
			for (final Double elementB : values) {
				if (elementB.equals(elementA)) {
					++count;
				}
			}
			if (count > maxCount) {
				maxCount = count;
				maxValue = elementA;
			}
		}

		return maxValue;
	}

	/**
	 * Calculates the norm of an Array
	 */
	public static double norm(final double... array) {
		double norm = 0;
		for (final double element : array) {
			norm += element * element;
		}
		return Math.sqrt(norm);
	}

	/**
	 * Normalize the given probabilities in place.
	 *
	 * @param memebrshipPcts
	 */
	public static void normalizeLog2Probs(final double[] log2prob) {
		final double sum = log2SumOfExponentials(log2prob);

		for (int i = 0; i < log2prob.length; i++) {
			log2prob[i] = Math.pow(2, log2prob[i] - sum);
		}
	}

	/**
	 * Normalize the given probabilities in place.
	 *
	 * @param memebrshipPcts
	 */
	public static <T> void normalizeLog2Probs(final Map<T, Double> log2prob) {
		final double sum = log2SumOfExponentials(log2prob.values());

		for (final Entry<T, Double> entry : log2prob.entrySet()) {
			entry.setValue(Math.pow(2, entry.getValue() - sum));
		}
	}

	/**
	 * Calculates the sum of an Array
	 */
	public static double sum(final double... array) {
		double sum = 0;
		for (final double element : array) {
			sum += element;
		}
		return sum;
	}

	/**
	 * Calculates the sum of a Collection
	 */
	public static double sum(final Iterable<Double> values) {
		double sum = 0;
		for (final Double element : values) {
			sum += element;
		}
		return sum;
	}

	private static final double LN_2 = Math.log(2);

}
