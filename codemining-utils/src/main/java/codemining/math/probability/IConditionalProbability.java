package codemining.math.probability;

import com.google.common.base.Optional;

/**
 * A conditional distribution probability interface for an element of type A
 * given an element of type B ie. P(a|b).
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 * @param <A>
 * @param <B>
 */
public interface IConditionalProbability<A, B> {

	/**
	 * Return the maximum likelihood element for the given element.
	 * 
	 * @param given
	 * @return the maximum likelihood element for the given. If the given does
	 *         not exist an empty optional is returned.
	 */
	Optional<A> getMaximumLikelihoodElement(B given);

	/**
	 * Return the maximum likelihood (unsmoothed) probability for an element
	 * given a specific context. If the given context has not been previously
	 * seen, we return a probability of 1.
	 * 
	 * @param element
	 * @param given
	 * @return
	 */
	double getMLProbability(A element, B given);

}