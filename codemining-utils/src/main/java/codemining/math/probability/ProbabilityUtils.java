/**
 * 
 */
package codemining.math.probability;

import java.util.Set;

import com.google.common.base.Optional;

/**
 * Utilities related to probabilities.
 * 
 * @author Miltos Allamanis
 * 
 */
public class ProbabilityUtils {

	/**
	 * Compare if two conditional probabilities are equal in the
	 * maximum-likelihood view.
	 * 
	 * @param cpd1
	 * @param cpd2
	 * @return
	 */
	public static <A, B> boolean conditionalProbabiltiesEquivalentInML(
			final IDiscreteConditionalProbability<A, B> cpd1,
			final IDiscreteConditionalProbability<A, B> cpd2) {
		final Set<B> support1 = cpd1.getPossibleContexts();
		final Set<B> support2 = cpd2.getPossibleContexts();

		if (!support1.equals(support2)) {
			return false;
		}

		for (final B context : support1) {
			final Optional<A> ml1 = cpd1.getMaximumLikelihoodElement(context);
			final Optional<A> ml2 = cpd2.getMaximumLikelihoodElement(context);
			if (!ml1.equals(ml2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Print the differences among two CPDs.
	 * 
	 * @param cpd1
	 * @param cpd2
	 */
	public static <A, B> void printClusterDifferences(
			final IDiscreteConditionalProbability<A, B> cpd1,
			final IDiscreteConditionalProbability<A, B> cpd2) {
		final Set<B> support1 = cpd1.getPossibleContexts();

		for (final B context : support1) {
			final Optional<A> ml1 = cpd1.getMaximumLikelihoodElement(context);
			final Optional<A> ml2 = cpd2.getMaximumLikelihoodElement(context);
			if (!ml1.equals(ml2)) {
				System.out.println("Context " + context + ": " + ml1.orNull()
						+ " vs " + ml2.orNull());
			}
		}
	}

	private ProbabilityUtils() {
		// utility class
	}
}
