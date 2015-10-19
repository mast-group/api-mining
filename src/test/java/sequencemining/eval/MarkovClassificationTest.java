package sequencemining.eval;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

public class MarkovClassificationTest {

	@Test
	public void testStochasticGenerator() {

		final RandomGenerator rand = new MersenneTwister(1);
		final int n = 5;

		final RealMatrix A = MarkovClassificationTask.generateRandomPositiveSquareMatrix(n, rand);
		final RealMatrix S = MarkovClassificationTask.SinkhornKnopp(A);

		for (int i = 0; i < n; i++)
			assertEquals(1.0, S.getRowVector(i).getL1Norm(), 1e-15);
		for (int i = 0; i < n; i++)
			assertEquals(1.0, S.getColumnVector(i).getL1Norm(), 1e-15);

	}

}
