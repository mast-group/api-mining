package itemsetmining.eval;

import itemsetmining.itemset.Sequence;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.NonSquareMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.Lists;

public class MarkovClassificationTask {

	private static final boolean VERBOSE = true;

	public static void main(final String[] args) throws IOException {

		final int noLabels = 2;
		final int noStates = 4;
		final int noSteps = 10;
		final int noInstances = 10;
		final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Classification/";
		final File dbFile = new File(baseFolder + "MARKOV.txt");

		// Set up correlated matrices
		final double[][] F = new double[][] { { 1, 100, 1, 1 },
				{ 1, 1, 100, 100 }, { 100, 1, 1, 1 }, { 100, 1, 1, 1 } };
		final double[][] B = new double[][] { { 1, 1, 100, 100 },
				{ 100, 1, 1, 1 }, { 1, 100, 1, 1 }, { 1, 100, 1, 1 } };
		final List<RealMatrix> matrices = Lists.newArrayList(
				new Array2DRowRealMatrix(F), new Array2DRowRealMatrix(B));

		// Set up starting vectors
		final List<RealVector> starts = new ArrayList<>();
		for (int i = 0; i < noLabels; i++)
			starts.add(new ArrayRealVector(noStates, 1. / noStates));

		// Generate Sequence database
		generateMarkovProblem(noLabels, noSteps, noInstances, matrices, starts,
				dbFile);
		final TransactionList dbTrans = ItemsetMining.readTransactions(dbFile);

		// Mine freq seqs
		final double minSup = 0.6;
		final Map<Sequence, Integer> seqsFSM = FrequentItemsetMining
				.mineFrequentSequencesPrefixSpan(dbFile.getPath(), null, minSup);
		System.out.println(seqsFSM);

		// Generate freq seq features
		// seqsFSM = removeSingletons(seqsFSM);
		generateFeatures(dbTrans, seqsFSM.keySet(), new File(baseFolder
				+ "FeaturesFSM.txt"), noLabels);

		// Mine int seqs
		final int maxStructureSteps = 100_000;
		final int maxEMIterations = 1_000;
		final Map<Sequence, Double> seqsISM = ItemsetMining.mineSequences(
				dbFile, new InferGreedy(), maxStructureSteps, maxEMIterations,
				null);
		System.out.println(seqsISM);

		// Generate int seq features
		// seqsISM = removeSingletons(seqsISM);
		generateFeatures(dbTrans, seqsISM.keySet(), new File(baseFolder
				+ "FeaturesISM.txt"), noLabels);

		// Generate simple features
		final Set<Sequence> singletons = new HashSet<>();
		for (int i = 0; i < noStates; i++)
			singletons.add(new Sequence(i));
		generateFeatures(dbTrans, singletons, new File(baseFolder
				+ "FeaturesSimple.txt"), noLabels);
	}

	private static void generateMarkovProblem(final int noLabels,
			final int noSteps, final int noInstances,
			final List<RealMatrix> matrices, final List<RealVector> starts,
			final File outFile) throws IOException {

		// Set up Markov Chains
		final List<MarkovChain> chains = new ArrayList<>();
		for (int i = 0; i < noLabels; i++) {
			final RandomGenerator rng = new MersenneTwister(i);
			final RealMatrix transitionMatrix = matrices.get(i);
			// SinkhornKnopp(matrices.get(i));
			// System.out.println(transitionMatrix);
			final MarkovChain chain = new MarkovChain(rng, transitionMatrix,
					starts.get(i));
			System.out.println(chain.getTransitionMatrix());
			chains.add(chain);
		}

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		int count = 0;
		while (count < noInstances) {
			for (int i = 0; i < noLabels; i++) {
				final MarkovChain chain = chains.get(i);
				for (int k = 0; k < noSteps; k++) {
					out.print(chain.step() + " -1 ");
				}
				out.print("-2\n");
				count++;
			}
		}
		out.close();

		if (VERBOSE)
			printFileToScreen(outFile);
	}

	private static void generateFeatures(final TransactionList dbTrans,
			final Set<Sequence> seqs, final File outFile, final int noLabels)
			throws IOException {

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate features
		int count = 0;
		for (final Transaction trans : dbTrans.getTransactionList()) {
			final int label = count % noLabels;
			out.print(label + " ");
			int fNum = 0;
			for (final Sequence seq : seqs) {
				if (trans.contains(seq))
					out.print("f" + fNum + ":1 ");
				else
					out.print("f" + fNum + ":0 ");
				fNum++;
			}
			out.println();
			count++;
		}
		out.close();

		if (VERBOSE)
			printFileToScreen(outFile);
	}

	public static RealMatrix generateRandomPositiveSquareMatrix(final int n,
			final RandomGenerator rand) {
		final double[][] d = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				d[i][j] = rand.nextDouble();
			}
		}
		return new Array2DRowRealMatrix(d);
	}

	/**
	 * Convert an irreducible matrix A to a doubly stochastic matrix DAE
	 *
	 * @param A
	 *            an irreducible matrix (i.e. [A^m]_ij > 0 for some m > 0)
	 * @return a doubly stochastic matrix DAE where D,E are diagonal matrices
	 */
	public static RealMatrix SinkhornKnopp(final RealMatrix A) {
		final int n = A.getRowDimension();
		final int m = A.getColumnDimension();
		if (n != m)
			throw new NonSquareMatrixException(n, m);

		ArrayRealVector c = null;
		final ArrayRealVector e = new ArrayRealVector(n, 1.0);
		ArrayRealVector r = e;
		ArrayRealVector rold = new ArrayRealVector(n);

		while (r.subtract(rold).getNorm() > 1e-15) {
			rold = r;
			c = e.ebeDivide(A.preMultiply(r));
			r = e.ebeDivide(A.operate(c));
		}
		final RealMatrix Dr = MatrixUtils.createRealDiagonalMatrix(r
				.getDataRef());
		final RealMatrix Dc = MatrixUtils.createRealDiagonalMatrix(c
				.getDataRef());

		return Dr.multiply(A).multiply(Dc);
	}

	public static void printFileToScreen(final File file)
			throws FileNotFoundException {
		final FileReader reader = new FileReader(file);
		final LineIterator it = new LineIterator(reader);
		while (it.hasNext()) {
			System.out.println(it.nextLine());
		}
		LineIterator.closeQuietly(it);
	}

	static <V> Map<Sequence, V> removeSingletons(final Map<Sequence, V> oldSeqs) {
		final Map<Sequence, V> newSeqs = new HashMap<>();
		for (final Entry<Sequence, V> entry : oldSeqs.entrySet()) {
			if (entry.getKey().size() > 1)
				newSeqs.put(entry.getKey(), entry.getValue());
		}
		return newSeqs;
	}

}
