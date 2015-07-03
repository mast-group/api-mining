/**
 * 
 */
package codemining.math.probability;

import java.util.Set;

/**
 * An interface for probabilities that hava a discrete space of the given (i.e.
 * B)
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public interface IDiscreteConditionalProbability<A, B> extends
		IConditionalProbability<A, B> {

	/**
	 * Returns the set of all possible contexts (given's) in P(A|B)
	 * 
	 * @return
	 */
	Set<B> getPossibleContexts();
}
