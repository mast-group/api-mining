/**
 * 
 */
package codemining.math.distributions;

import com.google.common.math.DoubleMath;

/**
 * A simple calculator of the geometric distribution.
 * 
 * @author "Miltos Allamanis <m.allamanis@ed.ac.uk>"
 * 
 */
public class GeometricDistribution {

	private GeometricDistribution() {
	};

	public static double getLog2Prob(int x, double p) {
		final double logP = DoubleMath.log2(p);
		final double log1_p = DoubleMath.log2(1 - p);
		return (x - 1) * log1_p + logP;
	}

	public static double getProb(int x, double p) {
		return Math.pow(2, getLog2Prob(x, p));
	}
}
