package codemining.math.probability;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import codemining.math.random.SampleUtils;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/**
 * A discrete conditional distribution table. Models P(A|B).
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class DiscreteElementwiseConditionalDistribution<A, B> implements
		ISamplableConditionalProbability<A, B>,
		IDiscreteConditionalProbability<A, B> {

	/**
	 * A map from B-> A names
	 */
	private final TreeMap<B, Multiset<A>> table;

	/**
	 * 
	 */
	public DiscreteElementwiseConditionalDistribution() {
		table = new TreeMap<B, Multiset<A>>();

	}

	/**
	 * Add a single element to the CPD given the context.
	 * 
	 * @param element
	 * @param given
	 */
	public void addElement(final A element, final B given) {
		final Multiset<A> elements;

		if (!table.containsKey(given)) {
			elements = HashMultiset.create();
			table.put(given, elements);
		} else {
			elements = table.get(given);
		}

		elements.add(element);
	}

	/**
	 * Returns the internal table.
	 * 
	 * @return
	 */
	public Map<B, Multiset<A>> getInternalTable() {
		return Maps.unmodifiableNavigableMap(table);
	}

	/**
	 * Return the element with the maximum likelihood. If given is unkown, then
	 * return null.
	 * 
	 * @param given
	 * @return
	 */
	@Override
	public Optional<A> getMaximumLikelihoodElement(final B given) {
		if (!table.containsKey(given)) {
			return Optional.absent();
		}
		int maxCount = 0;
		A maxLi = null;
		for (final Multiset.Entry<A> entry : table.get(given).entrySet()) {
			if (maxCount < entry.getCount()) {
				maxCount = entry.getCount();
				maxLi = entry.getElement();
			}
		}

		return Optional.of(maxLi);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * codemining.math.distributions.IConditionalProbability#getMLProbability(A,
	 * B)
	 */
	@Override
	public double getMLProbability(final A element, final B given) {
		if (table.containsKey(given)) {
			final Multiset<A> elements = table.get(given);
			return ((double) elements.count(element)) / elements.size();
		} else {
			return 1;
		}
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
		final A sample = SampleUtils.getRandomElement(table.get(given));
		return Optional.of(sample);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();

		for (final Entry<B, Multiset<A>> entry : table.entrySet()) {
			sb.append("P(?|" + entry.getKey().toString() + ")="
					+ entry.getValue().toString() + "\n");
		}

		return sb.toString();
	}

}
