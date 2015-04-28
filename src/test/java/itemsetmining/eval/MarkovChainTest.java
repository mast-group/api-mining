/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package itemsetmining.eval;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.NonSquareMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.util.MathArrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link MarkovChain}.
 *
 * @version $Id: MarkovChainTest.java 186 2013-03-11 14:39:35Z wydrych $
 */
public class MarkovChainTest {

	/**
	 * Transition matrix used for testing.
	 */
	private final RealMatrix transitionMatrix;
	/**
	 * Default chain used for testing;
	 */
	private final MarkovChain chain;
	/**
	 * Stationary vector used for testing.
	 */
	private final RealVector stationaryVector;

	/**
	 * Creates the default chain.
	 */
	public MarkovChainTest() {
		transitionMatrix = new Array2DRowRealMatrix(new double[][] {
				{ 0.5, 0.5 }, { 0.25, 0.75 } });
		chain = new MarkovChain(transitionMatrix);
		stationaryVector = new ArrayRealVector(new double[] { 1.0 / 3.0,
				2.0 / 3.0 });
	}

	/**
	 * Tests if correct exceptions are thrown by the constructor.
	 */
	@Test
	public void testExceptions() {
		MarkovChain invalid = null;
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5 }, { 0.25 } }), new ArrayRealVector(new double[] {
					0.5, 0.5 }));
			Assert.fail("Expected NonSquareMatrixException");
		} catch (final NonSquareMatrixException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5, 0.5 }, { 0.25, 0.75 } }), new ArrayRealVector(
					new double[] { 0.5 }));
			Assert.fail("Expected DimensionMismatchException");
		} catch (final DimensionMismatchException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5, -1 }, { 0.25, 0.75 } }), new ArrayRealVector(
					new double[] { 0.5, 0.5 }));
			Assert.fail("Expected NotPositiveException");
		} catch (final NotPositiveException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5, 0.5 }, { 0.25, 0.75 } }), new ArrayRealVector(
					new double[] { 0.5, -1 }));
			Assert.fail("Expected NotPositiveException");
		} catch (final NotPositiveException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5, Double.POSITIVE_INFINITY }, { 0.25, 0.75 } }),
					new ArrayRealVector(new double[] { 0.5, 0.5 }));
			Assert.fail("Expected MathIllegalArgumentException");
		} catch (final MathIllegalArgumentException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5, 0.5 }, { 0.25, 0.75 } }), new ArrayRealVector(
					new double[] { 0.5, Double.POSITIVE_INFINITY }));
			Assert.fail("Expected MathIllegalArgumentException");
		} catch (final MathIllegalArgumentException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.0, 0.0 }, { 0.25, 0.75 } }), new ArrayRealVector(
					new double[] { 0.5, 0.5 }));
			Assert.fail("Expected MathArithmeticException");
		} catch (final MathArithmeticException e) {
		}
		try {
			invalid = new MarkovChain(new Array2DRowRealMatrix(new double[][] {
					{ 0.5, 0.5 }, { 0.25, 0.75 } }), new ArrayRealVector(
					new double[] { 0.0, 0.0 }));
			Assert.fail("Expected MathArithmeticException");
		} catch (final MathArithmeticException e) {
		}
		Assert.assertNull(invalid);
	}

	/**
	 * Tests if the static getStationaryVector method returns proper vector.
	 */
	@Test
	public void testGetStationaryVector_RealMatrix() {
		final RealVector pi = MarkovChain.getStationaryVector(transitionMatrix);
		Assert.assertEquals(2, pi.getDimension());
		Assert.assertArrayEquals(stationaryVector.toArray(), pi.toArray(),
				1e-10);
		RealVector invalid = null;
		try {
			invalid = MarkovChain.getStationaryVector(new Array2DRowRealMatrix(
					new double[][] { { 0.5 }, { 0.25 } }));
			Assert.fail("Expected NonSquareMatrixException");
		} catch (final NonSquareMatrixException e) {
		}
		try {
			invalid = MarkovChain.getStationaryVector(new Array2DRowRealMatrix(
					new double[][] { { 0.5, -1 }, { 0.25, 0.75 } }));
			Assert.fail("Expected NotPositiveException");
		} catch (final NotPositiveException e) {
		}
		try {
			invalid = MarkovChain.getStationaryVector(new Array2DRowRealMatrix(
					new double[][] { { 0.5, Double.POSITIVE_INFINITY },
							{ 0.25, 0.75 } }));
			Assert.fail("Expected MathIllegalArgumentException");
		} catch (final MathIllegalArgumentException e) {
		}
		try {
			invalid = MarkovChain.getStationaryVector(new Array2DRowRealMatrix(
					new double[][] { { 0.0, 0.0 }, { 0.25, 0.75 } }));
			Assert.fail("Expected MathArithmeticException");
		} catch (final MathArithmeticException e) {
		}
		try {
			invalid = MarkovChain.getStationaryVector(new Array2DRowRealMatrix(
					new double[][] { { 1.0, 0.0 }, { 0.0, 1.0 } }));
			Assert.fail("Expected SingularMatrixException");
		} catch (final SingularMatrixException e) {
		}
		Assert.assertNull(invalid);
	}

	/**
	 * Tests if proper number of states is returned.
	 */
	@Test
	public void testGetStateCount() {
		Assert.assertEquals(2, chain.getStateCount());
	}

	/**
	 * Tests if proper transition matrix is returned.
	 */
	@Test
	public void testGetTransitionMatrix() {
		final RealMatrix tm = chain.getTransitionMatrix();
		Assert.assertEquals(2, tm.getRowDimension());
		Assert.assertEquals(2, tm.getColumnDimension());
		for (int row = 0; row < tm.getColumnDimension(); row++) {
			Assert.assertArrayEquals(transitionMatrix.getData()[row],
					tm.getRow(row), 0);
		}
	}

	/**
	 * Tests if proper stationary vector is returned.
	 */
	@Test
	public void testGetStationaryVector_0args() {
		Assert.assertArrayEquals(stationaryVector.toArray(), chain
				.getStationaryVector().toArray(), 1e-10);
	}

	/**
	 * Tests if setting a new state randomly works properly.
	 */
	@Test
	public void testSetState_doubleArr() {
		chain.setState(new double[] { 1, 0 });
		Assert.assertEquals(0, chain.getState());
		chain.setState(new double[] { 0, 1 });
		Assert.assertEquals(1, chain.getState());

		try {
			chain.setState(new double[] { 0 });
			Assert.fail("Expected DimensionMismatchException");
		} catch (final DimensionMismatchException e) {
		}
		try {
			chain.setState(new double[] { 0, -1 });
			Assert.fail("Expected NotPositiveException");
		} catch (final NotPositiveException e) {
		}
		try {
			chain.setState(new double[] { 0, 0 });
			Assert.fail("Expected MathArithmeticException");
		} catch (final MathArithmeticException e) {
		}
		try {
			chain.setState(new double[] { 0, Double.POSITIVE_INFINITY });
			Assert.fail("Expected MathIllegalArgumentException");
		} catch (final MathIllegalArgumentException e) {
		}
	}

	/**
	 * Tests if setting a new state randomly works properly.
	 */
	@Test
	public void testSetState_RealVector() {
		chain.setState(new ArrayRealVector(new double[] { 1, 0 }));
		Assert.assertEquals(0, chain.getState());
		chain.setState(new ArrayRealVector(new double[] { 0, 1 }));
		Assert.assertEquals(1, chain.getState());

		try {
			chain.setState(new ArrayRealVector(new double[] { 0 }));
			Assert.fail("Expected DimensionMismatchException");
		} catch (final DimensionMismatchException e) {
		}
		try {
			chain.setState(new ArrayRealVector(new double[] { 0, -1 }));
			Assert.fail("Expected NotPositiveException");
		} catch (final NotPositiveException e) {
		}
		try {
			chain.setState(new ArrayRealVector(new double[] { 0, 0 }));
			Assert.fail("Expected MathArithmeticException");
		} catch (final MathArithmeticException e) {
		}
		try {
			chain.setState(new ArrayRealVector(new double[] { 0,
					Double.POSITIVE_INFINITY }));
			Assert.fail("Expected MathIllegalArgumentException");
		} catch (final MathIllegalArgumentException e) {
		}
	}

	/**
	 * Tests if setting a new state arbitrarily works properly.
	 */
	@Test
	public void testSetState_int() {
		chain.setState(0);
		Assert.assertEquals(0, chain.getState());
		chain.setState(1);
		Assert.assertEquals(1, chain.getState());
		try {
			chain.setState(-1);
			Assert.fail("Expected OutOfRangeException");
		} catch (final OutOfRangeException e) {
		}
		try {
			chain.setState(2);
			Assert.fail("Expected OutOfRangeException");
		} catch (final OutOfRangeException e) {
		}
	}

	/**
	 * Tests if state from proper range is returned and if checking state does
	 * not change it.
	 */
	@Test
	public void testGetState() {
		Assert.assertTrue(chain.getState() >= 0);
		Assert.assertTrue(chain.getState() < 2);
		// check if getState does not change state
		Assert.assertEquals(chain.getState(), chain.getState());
	}

	/**
	 * Tests random sampling.
	 */
	@Test
	public void testStep_0args() {
		final int n = 1000000;
		chain.reseedRandomGenerator(-334759360); // fixed seed
		final int[][] c = new int[2][2];
		for (int i = 0; i < n; i++) {
			++c[chain.getState()][chain.step()];
		}
		Assert.assertEquals(n, c[0][0] + c[0][1] + c[1][0] + c[1][1]);
		Assert.assertArrayEquals(
				transitionMatrix.getRow(0),
				MathArrays.normalizeArray(new double[] { c[0][0], c[0][1] }, 1),
				1e-2);
		Assert.assertArrayEquals(
				transitionMatrix.getRow(1),
				MathArrays.normalizeArray(new double[] { c[1][0], c[1][1] }, 1),
				1e-2);

		final int[] sc = new int[2];
		for (int i = 0; i < n; i++) {
			++sc[chain.step()];
		}
		Assert.assertEquals(n, sc[0] + sc[1]);
		Assert.assertArrayEquals(stationaryVector.toArray(),
				MathArrays.normalizeArray(new double[] { sc[0], sc[1] }, 1),
				1e-2);
	}

	/**
	 * Tests deterministic sampling.
	 */
	@Test
	public void testStep_int() {
		final MarkovChain walker = new MarkovChain(new Array2DRowRealMatrix(
				new double[][] { { 0, 1, 0 }, { 0, 0, 1 }, { 1, 0, 0 } }),
				new ArrayRealVector(new double[] { 1, 0, 0 }));
		// start in 0
		Assert.assertEquals(0, walker.getState());
		// wrong n value does not change value
		try {
			walker.step(-1);
			Assert.fail("Expected NotStrictlyPositiveException");
		} catch (final NotStrictlyPositiveException e) {
		}
		// (0 + 3) % 3 = 0
		Assert.assertEquals(0, walker.step(3));
		// (0 + 1) % 3 = 1
		Assert.assertEquals(1, walker.step(1));
		// (1 + 4) % 3 = 2
		Assert.assertEquals(2, walker.step(4));
		// (2 + 46271264) % 3 = (2 + 2) % 3 = 1
		Assert.assertEquals(1, walker.step(46271264));
	}
}
