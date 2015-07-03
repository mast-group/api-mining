package codemining.math.probability;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import codemining.math.random.SampleUtils;

import com.google.common.base.Optional;

/**
 * A discrete conditional probability that works on a simple CPD table
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 * @param <A>
 * @param <B>
 */
public class DiscreteConditionalProbability<A, B> implements
		ISamplableConditionalProbability<A, B>,
		IDiscreteConditionalProbability<A, B> {

	/**
	 * The smoothed probability table.
	 */
	protected final Map<B, Map<A, Double>> table;

	public DiscreteConditionalProbability(final Map<B, Map<A, Double>> cpdTable) {
		table = cpdTable;
	}

	@Override
	public Optional<A> getMaximumLikelihoodElement(final B given) {
		if (!table.containsKey(given)) {
			return Optional.absent();
		}

		final Map<A, Double> probs = table.get(given);
		double maxProb = 0;
		A maxElement = null;

		for (final Entry<A, Double> entry : probs.entrySet()) {
			if (maxProb < entry.getValue()) {
				maxProb = entry.getValue();
				maxElement = entry.getKey();
			}
		}

		return Optional.of(checkNotNull(maxElement));
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
		return 0;
	}

	@Override
	public Set<B> getPossibleContexts() {
		return Collections.unmodifiableSet(table.keySet());
	}

	@Override
	public Optional<A> getRandomSample(final B given) {
		if (!table.containsKey(given)) {
			return Optional.absent();
		}
		final A sample = SampleUtils.getRandomKey(table.get(given));
		return Optional.of(sample);
	}

}