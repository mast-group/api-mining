package itemsetmining.eval;

import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.sequence.Sequence;
import itemsetmining.main.SequenceMining;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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

	private static final boolean VERBOSE = false;

	public static void main(final String[] args) throws IOException {

		// final int noLabels = 2;
		final int noStates = 4;
		final int noSteps = 10;
		final int noInstances = 1000;
		final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Classification/Markov/";
		final File dbFile = new File(baseFolder + "MARKOV.txt");

		// Set up correlated matrices
		final double[][] F = new double[][] { { 1, 100, 1, 1 },
				{ 1, 1, 100, 1 }, { 1, 1, 1, 100 }, { 100, 1, 1, 1 } };
		final double[][] B = new double[][] { { 1, 1, 1, 100 },
				{ 100, 1, 1, 1 }, { 1, 100, 1, 1 }, { 1, 1, 100, 1 } };
		final List<RealMatrix> matrices = Lists.newArrayList(
				new Array2DRowRealMatrix(F), new Array2DRowRealMatrix(B));

		// Set up starting vectors
		final List<RealVector> starts = new ArrayList<>();
		for (int i = 0; i < 2; i++)
			starts.add(new ArrayRealVector(noStates, 1. / noStates));

		// Generate Sequence database
		final int[] labels = generateMarkovProblem(noSteps, noInstances,
				matrices, starts, dbFile);
		final TransactionList dbTrans = SequenceMining.readTransactions(dbFile);

		// Mine freq seqs
		final double minSup = 0.8;
		final Map<Sequence, Integer> seqsFSM = FrequentSequenceMining
				.mineFrequentSequencesPrefixSpan(dbFile.getPath(), null, minSup);
		System.out.println(seqsFSM);

		// Generate freq seq features
		// seqsFSM = removeSingletons(seqsFSM);
		final File featuresFSM = new File(baseFolder + "FeaturesFSM.txt");
		generateFeatures(dbTrans, seqsFSM.keySet(), featuresFSM, labels);

		// Mine int seqs
		final int maxStructureSteps = 100_000;
		final int maxEMIterations = 1_000;
		final Map<Sequence, Double> seqsISM = SequenceMining.mineSequences(
				dbFile, new InferGreedy(), maxStructureSteps, maxEMIterations,
				null);
		System.out.println(seqsISM);

		// Generate int seq features
		// seqsISM = removeSingletons(seqsISM);
		final File featuresISM = new File(baseFolder + "FeaturesISM.txt");
		generateFeatures(dbTrans, seqsISM.keySet(), featuresISM, labels);

		// Generate simple features
		final Set<Sequence> singletons = new HashSet<>();
		for (int i = 0; i < noStates; i++)
			singletons.add(new Sequence(i));
		final File featuresSimple = new File(baseFolder + "FeaturesSimple.txt");
		generateFeatures(dbTrans, singletons, featuresSimple, labels);

		// Run MALLET Naive Bayes classifier
		classify(baseFolder, featuresFSM);
		classify(baseFolder, featuresISM);
		classify(baseFolder, featuresSimple);
	}

	private static int[] generateMarkovProblem(final int noSteps,
			final int noInstances, final List<RealMatrix> matrices,
			final List<RealVector> starts, final File outFile)
			throws IOException {

		// Set random number seed
		final Random random = new Random(1);

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");
		final int[] labels = new int[noInstances];

		// Generate transaction database
		int count = 0;
		while (count < noInstances) {

			// Choose label
			int i = 0;
			if (random.nextDouble() < 0.6)
				i = 1;
			labels[count] = i;

			// Set up chain
			// SinkhornKnopp(matrices.get(i));
			final MarkovChain chain = new MarkovChain(new MersenneTwister(i),
					matrices.get(i), starts.get(i));

			// Step
			out.print(chain.getState() + " -1 ");
			for (int k = 1; k < noSteps; k++) {
				out.print(chain.step() + " -1 ");
			}
			out.print("-2\n");
			count++;
		}
		out.close();

		if (VERBOSE)
			printFileToScreen(outFile);

		return labels;
	}

	private static void generateFeatures(final TransactionList dbTrans,
			final Set<Sequence> seqs, final File outFile, final int[] labels)
			throws IOException {

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate features
		int count = 0;
		for (final Transaction trans : dbTrans.getTransactionList()) {
			final int label = labels[count];
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

	/** Classify using MALLET Naive Bayes */
	static void classify(final String baseFolder, final File inFile) {

		final String alg = inFile.getName().replace("Features", "")
				.replace(".txt", "");
		final File tmpFile = new File(baseFolder + alg + ".mallet");
		final File outFile = new File(baseFolder + alg + "Classification.txt");

		// Convert to binary MALLET format
		final String cmd[] = new String[4];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mallet-2.0.7/bin/mallet";
		cmd[1] = "import-svmlight";
		cmd[2] = "--input " + inFile;
		cmd[3] = "--output " + tmpFile;
		runScript(cmd, null);

		// Classify
		final String cmd2[] = new String[6];
		cmd2[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mallet-2.0.7/bin/mallet";
		cmd2[1] = "train-classifier";
		cmd2[2] = "--input " + tmpFile;
		cmd2[3] = "--training-portion 0.9";
		cmd2[4] = "--num-trials 25";
		cmd2[5] = "--report train:accuracy test:accuracy";
		runScript(cmd2, outFile);

		// Remove temp file
		final String cmd3[] = new String[2];
		cmd3[0] = "rm";
		cmd3[1] = tmpFile.toString();
		runScript(cmd3, null);

	}

	/** Run shell script with command line arguments */
	public static void runScript(final String cmd[], final File outFile) {

		try {
			final ProcessBuilder pb = new ProcessBuilder(cmd);
			if (outFile != null)
				pb.redirectOutput(outFile);
			else
				pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			final Process process = pb.start();
			process.waitFor();
			process.destroy();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}
