/**
 * 
 */
package codemining.math.probability;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/**
 * A discrete conditional distribution that is smoothed using a Katz-like
 * smoothing.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class SmoothedDiscreteConditionalDistribution<A extends Comparable<A>, B extends Comparable<B>>
		extends DiscreteConditionalProbability<A, B> {

	/**
	 * The UNK probabilities for given B.
	 */
	private final Map<B, Double> unkTable;

	private static final int KATZ_MAX_COUNT = 8;

	public SmoothedDiscreteConditionalDistribution(
			final DiscreteElementwiseConditionalDistribution<A, B> dct) {
		super(Maps.<B, Map<A, Double>> newTreeMap());
		unkTable = Maps.newTreeMap();
		for (final Entry<B, Multiset<A>> entry : dct.getInternalTable()
				.entrySet()) {
			smoothEntry(entry.getValue(), entry.getKey());
		}
	}

	@Override
	public double getMLProbability(final A element, final B given) {
		if (!table.containsKey(given)) {
			return 1;
		}
		final Map<A, Double> probTable = table.get(given);
		if (probTable.containsKey(element)) {
			return probTable.get(element);
		}
		return unkTable.get(element);
	}

	/**
	 * Smooth the entries using a Katz-like smoothing.
	 * 
	 * @param values
	 * @param key
	 */
	private void smoothEntry(final Multiset<A> values, final B key) {
		// Create count of counts
		final int[] countOfCounts = new int[KATZ_MAX_COUNT + 1];
		Arrays.fill(countOfCounts, 0);

		for (final Multiset.Entry<A> entry : values.entrySet()) {
			final int count = entry.getCount();
			if (count <= countOfCounts.length) {
				countOfCounts[count - 1]++;
			}
		}

		// Calculated discount parameters
		final double[] coefficient = new double[KATZ_MAX_COUNT];
		for (int r = 1; r <= KATZ_MAX_COUNT; r++) {
			final double smoothedR = (r + 1.) * ((double) countOfCounts[r])
					/ ((double) countOfCounts[r - 1]);
			final double discount = (KATZ_MAX_COUNT + 1.)
					* ((double) countOfCounts[KATZ_MAX_COUNT])
					/ countOfCounts[0];
			final double nominator = smoothedR / r - discount;
			final double denominator = 1. - discount;
			coefficient[r - 1] = nominator / denominator;
		}

		// Calculate smoothed probabilities and unk-residual
		double sum = 0;
		final Map<A, Double> smoothedProbs = Maps.newTreeMap();
		table.put(key, smoothedProbs);

		for (final Multiset.Entry<A> entry : values.entrySet()) {
			final int count = entry.getCount();
			final double mlProbability = ((double) count) / values.size();
			if (count > KATZ_MAX_COUNT) {
				smoothedProbs.put(entry.getElement(), mlProbability);
				sum += mlProbability;
			} else {
				final double smoothedProbability = coefficient[count - 1]
						* mlProbability;
				smoothedProbs.put(entry.getElement(), smoothedProbability);
				sum += smoothedProbability;
			}
		}
		unkTable.put(key, 1. - sum);
	}
}
