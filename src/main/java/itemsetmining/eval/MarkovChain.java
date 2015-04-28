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

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.DefaultRealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.NonSquareMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.MathArrays;

/**
 * Implementation of Markov chain. The states are indexed from 0 to
 * {@link #getStateCount()}-1.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Markov_chain">Markov chain
 *      (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/MarkovChain.html">Markov Chain
 *      (MathWorld)</a>
 * @version $Id: MarkovChain.java 185 2013-03-10 22:44:08Z wydrych $
 */
public class MarkovChain {

	/**
	 * RNG instance used to generate samples from the distribution.
	 */
	protected final RandomGenerator random;
	/**
	 * Number of states in the chain.
	 */
	private final int stateCount;
	/**
	 * Square stateCount x stateCount transition matrix.
	 */
	private final RealMatrix transitionMatrix;
	/**
	 * List of the transition matrix rows in the form of probability
	 * distributions.
	 */
	private final EnumeratedIntegerDistribution[] transitions;
	/**
	 * Current state.
	 */
	private int state;

	/**
	 * Converts the passed matrix to a proper transition matrix if it is
	 * possible.
	 *
	 * @param transitionMatrix
	 *            square matrix to be normalized.
	 * @return transition square matrix of nonnegative real numbers, with each
	 *         row summing to 1.
	 * @throws NonSquareMatrixException
	 *             if {@code transitionMatrix} is not a square matrix.
	 * @throws NotPositiveException
	 *             if any element of {@code transitionMatrix} is negative.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code transitionMatrix} is infinite.
	 * @throws MathArithmeticException
	 *             if any {@code transitionMatrix} row sums to zero.
	 * @see <a href="http://en.wikipedia.org/wiki/Stochastic_matrix">Stochastic
	 *      matrix (Wikipedia)</a>
	 */
	protected static RealMatrix normalizeTransitionMatrix(
			final RealMatrix transitionMatrix) throws NonSquareMatrixException,
			NotPositiveException, MathIllegalArgumentException,
			MathArithmeticException {
		if (transitionMatrix.getColumnDimension() != transitionMatrix
				.getRowDimension()) {
			throw new NonSquareMatrixException(
					transitionMatrix.getRowDimension(),
					transitionMatrix.getColumnDimension());
		}
		transitionMatrix
				.walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor() {
					@Override
					public void visit(final int row, final int column,
							final double value) {
						if (value < 0) {
							throw new NotPositiveException(value);
						}
					}
				});

		final RealMatrix out = transitionMatrix.copy(); // preserve type
		for (int row = 0; row < transitionMatrix.getRowDimension(); row++) {
			out.setRow(row, MathArrays.normalizeArray(
					transitionMatrix.getRow(row), 1.0));
		}
		return out;
	}

	/**
	 * Calculates the stationary probability vector. Applying the transition
	 * matrix to the stationary vector (often denoted &pi;) does not change its
	 * value: {@code pi * P = pi}.
	 *
	 * @param transitionMatrix
	 *            square matrix for which the stationary vector is calculated.
	 * @return stationary vector.
	 * @throws NonSquareMatrixException
	 *             if {@code transitionMatrix} is not a square matrix.
	 * @throws NotPositiveException
	 *             if any element of {@code transitionMatrix} is negative.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code transitionMatrix} is infinite.
	 * @throws MathArithmeticException
	 *             if any {@code transitionMatrix} row sums to zero.
	 * @throws SingularMatrixException
	 *             if it is not possible to calculate the stationary vector.
	 */
	public static RealVector getStationaryVector(
			final RealMatrix transitionMatrix) throws NonSquareMatrixException,
			NotPositiveException, MathIllegalArgumentException,
			MathArithmeticException, SingularMatrixException {
		final int d = transitionMatrix.getColumnDimension();

		RealMatrix m = normalizeTransitionMatrix(transitionMatrix);
		m = m.transpose();
		m = m.subtract(MatrixUtils.createRealIdentityMatrix(d));
		for (int i = 0; i < d; i++) {
			m.setEntry(d - 1, i, 1.0);
		}

		final RealVector v = new ArrayRealVector(d);
		v.setEntry(d - 1, 1.0);

		final DecompositionSolver solver = new LUDecomposition(m).getSolver();

		return solver.solve(v);
	}

	/**
	 * Create a Markov chain using the given transition matrix. The probability
	 * of starting in each state is determined by the
	 * {@link #getStationaryVector() stationary probability vector}.
	 *
	 * @param transitionMatrix
	 *            square matrix defining the Markov chain transitions.
	 * @throws NonSquareMatrixException
	 *             if {@code transitionMatrix} is not a square matrix.
	 * @throws NotPositiveException
	 *             if any element of {@code transitionMatrix} is negative.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code transitionMatrix} is infinite.
	 * @throws MathArithmeticException
	 *             if any {@code transitionMatrix} row sums to zero.
	 * @throws SingularMatrixException
	 *             if it is not possible to calculate the stationary vector.
	 */
	public MarkovChain(final RealMatrix transitionMatrix)
			throws NonSquareMatrixException, NotPositiveException,
			MathIllegalArgumentException, MathArithmeticException,
			SingularMatrixException {
		this(transitionMatrix, getStationaryVector(transitionMatrix));
	}

	/**
	 * Create a Markov chain using the given random number generator and
	 * transition matrix. The probability of starting in each state is
	 * determined by the {@link #getStationaryVector() stationary probability
	 * vector}.
	 *
	 * @param rng
	 *            random number generator.
	 * @param transitionMatrix
	 *            square matrix defining the Markov chain transitions.
	 * @throws NonSquareMatrixException
	 *             if {@code transitionMatrix} is not a square matrix.
	 * @throws NotPositiveException
	 *             if any element of {@code transitionMatrix} is negative.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code transitionMatrix} is infinite.
	 * @throws MathArithmeticException
	 *             if any {@code transitionMatrix} row sums to zero.
	 * @throws SingularMatrixException
	 *             if it is not possible to calculate the stationary vector.
	 */
	public MarkovChain(final RandomGenerator rng,
			final RealMatrix transitionMatrix) throws NonSquareMatrixException,
			NotPositiveException, MathIllegalArgumentException,
			MathArithmeticException, SingularMatrixException {
		this(rng, transitionMatrix, getStationaryVector(transitionMatrix));
	}

	/**
	 * Create a Markov chain using the given transition matrix and start point
	 * vector. The probability of starting in each state is determined by the
	 * {@code start} vector.
	 *
	 * @param transitionMatrix
	 *            square matrix defining the Markov chain transitions.
	 * @param start
	 *            vector of probabilities of starting in respective states.
	 * @throws NonSquareMatrixException
	 *             if {@code transitionMatrix} is not a square matrix.
	 * @throws DimensionMismatchException
	 *             if dimension of {@code start} vector does not match the
	 *             dimensions of {@code transitionMatrix}.
	 * @throws NotPositiveException
	 *             if any element of {@code transitionMatrix} or {@code start}
	 *             vector is negative.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code transitionMatrix} or {@code start}
	 *             vector is infinite.
	 * @throws MathArithmeticException
	 *             if any {@code transitionMatrix} row or {@code start} vector
	 *             sums to zero.
	 */
	public MarkovChain(final RealMatrix transitionMatrix, final RealVector start)
			throws NonSquareMatrixException, DimensionMismatchException,
			NotPositiveException, MathIllegalArgumentException,
			MathArithmeticException {
		this(new Well19937c(), transitionMatrix, start);
	}

	/**
	 * Create a Markov chain using the given random number generator, transition
	 * matrix, and start point vector. The probability of starting in each state
	 * is determined by the {@code start} vector.
	 *
	 * @param rng
	 *            random number generator.
	 * @param transitionMatrix
	 *            square matrix defining the Markov chain transitions.
	 * @param start
	 *            vector of probabilities of starting in respective states.
	 * @throws NonSquareMatrixException
	 *             if {@code transitionMatrix} is not a square matrix.
	 * @throws DimensionMismatchException
	 *             if dimension of {@code start} vector does not match the
	 *             dimensions of {@code transitionMatrix}.
	 * @throws NotPositiveException
	 *             if any element of {@code transitionMatrix} or {@code start}
	 *             vector is negative.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code transitionMatrix} or {@code start}
	 *             vector is infinite.
	 * @throws MathArithmeticException
	 *             if any {@code transitionMatrix} row or {@code start} vector
	 *             sums to zero.
	 */
	public MarkovChain(final RandomGenerator rng,
			final RealMatrix transitionMatrix, final RealVector start)
			throws NonSquareMatrixException, DimensionMismatchException,
			NotPositiveException, MathIllegalArgumentException,
			MathArithmeticException {
		random = rng;
		this.transitionMatrix = normalizeTransitionMatrix(transitionMatrix);
		stateCount = transitionMatrix.getColumnDimension();
		transitions = new EnumeratedIntegerDistribution[stateCount];
		final int[] stateIndexes = new int[stateCount];

		for (int s = 0; s < stateCount; s++) {
			stateIndexes[s] = s;
		}
		for (int s = 0; s < stateCount; s++) {
			transitions[s] = new EnumeratedIntegerDistribution(rng,
					stateIndexes, transitionMatrix.getRow(s));
		}

		internalSetState(start.toArray());
	}

	/**
	 * Reseed the random generator used to generate samples.
	 *
	 * @param seed
	 *            the new seed
	 */
	public void reseedRandomGenerator(final long seed) {
		random.setSeed(seed);
	}

	/**
	 * Returns the number of states in the chain.
	 *
	 * @return number of states in the chain.
	 */
	public int getStateCount() {
		return stateCount;
	}

	/**
	 * Returns a copy of chain's transition matrix. It is a square
	 * {@link #getStateCount() stateCount}&nbsp;&times;&nbsp;
	 * {@link #getStateCount() stateCount} matrix of nonnegative real numbers,
	 * with each row summing to 1.
	 *
	 * @return chain's transition matrix.
	 */
	public RealMatrix getTransitionMatrix() {
		return transitionMatrix.copy(); // preserve type
	}

	/**
	 * Calculates the stationary probability vector of the chain. Applying the
	 * {@link #getTransitionMatrix() transition matrix} to the stationary vector
	 * (often denoted &pi;) does not change its value: {@code pi * P = pi}.
	 *
	 * @return stationary vector of the chain.
	 * @throws SingularMatrixException
	 *             if it is not possible to calculate the stationary vector.
	 */
	public RealVector getStationaryVector() throws SingularMatrixException {
		return getStationaryVector(transitionMatrix);
	}

	/**
	 * Changes randomly the state of the chain. Non-overridable function used
	 * within the constructor.
	 *
	 * @param probabilities
	 *            vector of probabilities of moving the chain into the
	 *            respective states.
	 * @throws DimensionMismatchException
	 *             if dimension of {@code probabilities} vector does not match
	 *             the {@link #getStateCount() state count}.
	 * @throws NotPositiveException
	 *             if any element of or {@code probabilities} vector is
	 *             negative.
	 * @throws MathArithmeticException
	 *             if {@code probabilities} vector sums to zero.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code start} vector is infinite.
	 */
	private void internalSetState(final double[] probabilities)
			throws DimensionMismatchException, NotPositiveException,
			MathArithmeticException, MathIllegalArgumentException {
		final int[] stateIndexes = new int[stateCount];

		for (int s = 0; s < stateCount; s++) {
			stateIndexes[s] = s;
		}

		internalSetState(new EnumeratedIntegerDistribution(random,
				stateIndexes, probabilities).sample());
	}

	/**
	 * Changes arbitrarily the state of the chain. Non-overridable function used
	 * within the constructor.
	 *
	 * @param newState
	 *            index of the state into which chain's current state will be
	 *            changed.
	 * @throws OutOfRangeException
	 *             if the index is out of range (<tt>newState
	 * &lt; 0 || newState &gt;= getStateCount()</tt>)
	 */
	private void internalSetState(final int newState)
			throws OutOfRangeException {
		if (newState < 0 || newState >= stateCount) {
			throw new OutOfRangeException(newState, 0, stateCount - 1);
		}
		this.state = newState;
	}

	/**
	 * Changes randomly the state of the chain.
	 *
	 * @param probabilities
	 *            array of probabilities of moving the chain into the respective
	 *            states.
	 * @throws DimensionMismatchException
	 *             if dimension of {@code probabilities} array does not match
	 *             the {@link #getStateCount() state count}.
	 * @throws NotPositiveException
	 *             if any element of or {@code probabilities} array is negative.
	 * @throws MathArithmeticException
	 *             if {@code probabilities} array sums to zero.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code probabilities} array is infinite.
	 */
	public void setState(final double[] probabilities)
			throws DimensionMismatchException, NotPositiveException,
			MathArithmeticException, MathIllegalArgumentException {
		internalSetState(probabilities);
	}

	/**
	 * Changes randomly the state of the chain.
	 *
	 * @param probabilities
	 *            vector of probabilities of moving the chain into the
	 *            respective states.
	 * @throws DimensionMismatchException
	 *             if dimension of {@code probabilities} vector does not match
	 *             the {@link #getStateCount() state count}.
	 * @throws NotPositiveException
	 *             if any element of or {@code probabilities} vector is
	 *             negative.
	 * @throws MathArithmeticException
	 *             if {@code probabilities} vector sums to zero.
	 * @throws MathIllegalArgumentException
	 *             if any element of {@code start} vector is infinite.
	 */
	public void setState(final RealVector probabilities)
			throws DimensionMismatchException, NotPositiveException,
			MathArithmeticException, MathIllegalArgumentException {
		internalSetState(probabilities.toArray());
	}

	/**
	 * Changes arbitrarily the state of the chain.
	 *
	 * @param newState
	 *            index of the state into which chain's current state will be
	 *            changed.
	 * @throws OutOfRangeException
	 *             if the index is out of range (
	 *             <tt>newStateIndex &lt; 0 || newStateIndex &gt;= getStateCount()</tt>
	 *             )
	 */
	public void setState(final int newState) throws OutOfRangeException {
		internalSetState(newState);
	}

	/**
	 * Returns current chain's state.
	 *
	 * @return current state.
	 */
	public int getState() {
		return state;
	}

	/**
	 * Changes chain's state to a randomized one and returns it.
	 *
	 * @return new current state.
	 */
	public int step() {
		return step(1);
	}

	/**
	 * Changes chain's state to a randomized one {@code n} times and returns the
	 * last one.
	 *
	 * @param n
	 *            number of random state changes to perform.
	 * @return new current state.
	 * @throws NotStrictlyPositiveException
	 *             if {@code n} is not positive.
	 */
	public int step(final int n) throws NotStrictlyPositiveException {
		if (n <= 0) {
			throw new NotStrictlyPositiveException(
					LocalizedFormats.NUMBER_OF_SAMPLES, n);
		}
		int nextState = state;
		for (int i = 0; i < n; i++) {
			nextState = transitions[nextState].sample();
		}
		setState(nextState);
		return state;
	}
}
