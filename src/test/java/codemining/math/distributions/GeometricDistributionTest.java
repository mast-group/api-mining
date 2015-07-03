package codemining.math.distributions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.math.DoubleMath;

public class GeometricDistributionTest {

	@Test
	public void testGetProb() {
		assertEquals(GeometricDistribution.getProb(1, .1), .1, 1E-10);
		assertEquals(GeometricDistribution.getLog2Prob(1, .1),
				DoubleMath.log2(.1), 1E-10);

		assertEquals(GeometricDistribution.getProb(2, .1), .09, 1E-10);
		assertEquals(GeometricDistribution.getLog2Prob(2, .1),
				DoubleMath.log2(.09), 1E-10);
	}

}
