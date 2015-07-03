package codemining.math.distributions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import codemining.math.probability.DiscreteElementwiseConditionalDistribution;

public class DiscreteConditionalDistributionTest {

	@Test
	public void test() {
		final DiscreteElementwiseConditionalDistribution<String, Integer> c = new DiscreteElementwiseConditionalDistribution<String, Integer>();

		c.addElement("hello", 1);
		c.addElement("hello2", 1);
		c.addElement("hello", 2);

		assertEquals(c.getMLProbability("hello", 1), .5, 1E-10);
		assertEquals(c.getMLProbability("hello2", 1), .5, 1E-10);

		assertEquals(c.getMLProbability("hello", 2), 1, 1E-10);

		assertEquals(c.getMLProbability("hello", 3), 1, 1E-10);
		assertEquals(c.getMLProbability("other", 1), 0, 1E-10);

	}

}
