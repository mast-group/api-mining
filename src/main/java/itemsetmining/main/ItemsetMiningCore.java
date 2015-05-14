package itemsetmining.main;

import itemsetmining.itemset.Sequence;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.TransactionDatabase;
import itemsetmining.transaction.TransactionRDD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import scala.Tuple2;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

public abstract class ItemsetMiningCore {

	/** Main fixed settings */
	private static final int OPTIMIZE_PARAMS_EVERY = 1;
	private static final double OPTIMIZE_TOL = 1e-5;

	protected static final Logger logger = Logger
			.getLogger(ItemsetMiningCore.class.getName());
	public static final File LOG_DIR = new File("/disk/data1/jfowkes/logs/");

	/** Variable settings */
	protected static Level LOG_LEVEL = Level.FINE;
	protected static long MAX_RUNTIME = 6 * 60 * 60 * 1_000; // 6hrs

	/**
	 * Learn itemsets model using structural EM
	 */
	protected static HashMap<Sequence, Double> structuralEM(
			final TransactionDatabase transactions,
			final Multiset<Sequence> singletons,
			final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations) {

		// Start timer
		final long startTime = System.currentTimeMillis();

		// Initialize itemset cache
		// if (transactions instanceof TransactionRDD) {
		// SparkEMStep.initializeCachedItemsets(transactions, singletons);
		// } else {
		EMStep.initializeCachedItemsets(transactions, singletons);
		// }

		// Intialize sequences with singleton seqs and their relative support
		// as well as supports with singletons and their actual supports
		final HashMap<Sequence, Double> sequences = new HashMap<>();
		final HashMap<Sequence, Integer> supports = new HashMap<>();
		for (final Multiset.Entry<Sequence> entry : singletons.entrySet()) {
			final Sequence seq = entry.getElement();
			final int support = entry.getCount();
			sequences.put(seq, support / (double) transactions.size());
			supports.put(seq, support);
		}
		logger.fine(" Initial sequences: " + sequences + "\n");

		// Initialize list of rejected seqs
		final Set<Sequence> rejected_seqs = new HashSet<>();

		// Define decreasing support ordering for sequences
		final Ordering<Sequence> supportOrdering = new Ordering<Sequence>() {
			@Override
			public int compare(final Sequence seq1, final Sequence seq2) {
				return supports.get(seq2) - supports.get(seq1);
			}
		}.compound(Ordering.usingToString());

		// Define decreasing support ordering for candidate sequences
		final HashMap<Sequence, Integer> candidateSupports = new HashMap<>();
		final Ordering<Sequence> candidateSupportOrdering = new Ordering<Sequence>() {
			@Override
			public int compare(final Sequence seq1, final Sequence seq2) {
				return candidateSupports.get(seq2)
						- candidateSupports.get(seq1);
			}
		}.compound(Ordering.usingToString());

		// Initialize average cost per transaction for singletons
		expectationMaximizationStep(sequences, transactions, inferenceAlgorithm);

		// Structural EM
		boolean breakLoop = false;
		for (int iteration = 1; iteration <= maxEMIterations; iteration++) {

			// Learn structure
			logger.finer("\n----- Itemset Combination at Step " + iteration
					+ "\n");
			combineSequencesStep(sequences, transactions, rejected_seqs,
					inferenceAlgorithm, maxStructureSteps, supportOrdering,
					supports, candidateSupportOrdering, candidateSupports);
			if (transactions.getIterationLimitExceeded())
				breakLoop = true;
			logger.finer(String.format(" Average cost: %.2f%n",
					transactions.getAverageCost()));

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0
					|| iteration == maxEMIterations || breakLoop == true) {
				logger.fine("\n***** Parameter Optimization at Step "
						+ iteration + "\n");
				expectationMaximizationStep(sequences, transactions,
						inferenceAlgorithm);
			}

			// Break loop if requested
			if (breakLoop)
				break;

			// Check if time exceeded
			if (System.currentTimeMillis() - startTime > MAX_RUNTIME) {
				logger.warning("\nRuntime limit of " + MAX_RUNTIME
						/ (60. * 1000.) + " minutes exceeded.\n");
				break;
			}

			// Spark: checkpoint every 100 iterations to avoid StackOverflow
			// errors due to long lineage (http://tinyurl.com/ouswhrc)
			if (iteration % 100 == 0 && transactions instanceof TransactionRDD) {
				transactions.getTransactionRDD().cache();
				transactions.getTransactionRDD().checkpoint();
				transactions.getTransactionRDD().count();
			}

			if (iteration == maxEMIterations)
				logger.warning("\nEM iteration limit exceeded.\n");
		}
		logger.info("\nElapsed time: "
				+ (System.currentTimeMillis() - startTime) / (60. * 1000.)
				+ " minutes.\n");

		return sequences;
	}

	/**
	 * Find optimal parameters for given set of sequences and store in sequences
	 *
	 * @return TransactionDatabase with the average cost per transaction
	 *         <p>
	 *         NB. zero probability sequences are dropped
	 */
	private static void expectationMaximizationStep(
			final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm) {

		logger.fine(" Structure Optimal Sequences: " + sequences + "\n");

		Map<Sequence, Double> prevSequences = sequences;

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Set up storage
			final Map<Sequence, Double> newSequences;

			// Parallel E-step and M-step combined
			// if (transactions instanceof TransactionRDD)
			// newItemsets = SparkEMStep.hardEMStep(transactions,
			// inferenceAlgorithm);
			// else
			newSequences = EMStep.hardEMStep(transactions.getTransactionList(),
					inferenceAlgorithm);

			// If set has stabilised calculate norm(p_prev - p_new)
			if (prevSequences.keySet().equals(newSequences.keySet())) {
				norm = 0;
				for (final Sequence seq : prevSequences.keySet()) {
					norm += Math.pow(
							prevSequences.get(seq) - newSequences.get(seq), 2);
				}
				norm = Math.sqrt(norm);
			}

			prevSequences = newSequences;
		}

		// Calculate average cost of last covering
		// if (transactions instanceof TransactionRDD)
		// SparkEMStep.calculateAndSetAverageCost(transactions);
		// else
		EMStep.calculateAndSetAverageCost(transactions);

		sequences.clear();
		sequences.putAll(prevSequences);
		logger.fine(" Parameter Optimal Sequences: " + sequences + "\n");
		logger.fine(String.format(" Average cost: %.2f%n",
				transactions.getAverageCost()));
	}

	/**
	 * Generate candidate sequences by combining existing seqs with highest
	 * order. Evaluate candidates with highest order first.
	 *
	 * @param sequenceSupportOrdering
	 *            ordering that determines which sequences to combine first
	 * @param supports
	 *            cached sequence supports for the above ordering
	 * @param candidateSupportOrdering
	 *            ordering that determines which candidates to evaluate first
	 * @param candidateSupports
	 *            cached candididate supports for the above ordering
	 */
	private static void combineSequencesStep(
			final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions,
			final Set<Sequence> rejected_seqs,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps,
			final Ordering<Sequence> sequenceSupportOrdering,
			final HashMap<Sequence, Integer> supports,
			final Ordering<Sequence> candidateSupportOrdering,
			final HashMap<Sequence, Integer> candidateSupports) {

		// Set up support-ordered priority queue
		final PriorityQueue<Sequence> candidateQueue = new PriorityQueue<Sequence>(
				maxSteps, candidateSupportOrdering);

		// Sort sequences according to given ordering
		final ArrayList<Sequence> sortedSequences = new ArrayList<>(
				sequences.keySet());
		Collections.sort(sortedSequences, sequenceSupportOrdering);

		// Find maxSteps superseqs for all seqs
		// final long startTime = System.nanoTime();
		int iteration = 0;
		final int len = sortedSequences.size();
		outerLoop: for (int k = 0; k < 2 * len - 2; k++) {
			for (int i = 0; i < len && i < k + 1; i++) {
				for (int j = 0; j < len && i + j < k + 1; j++) {
					if (k <= i + j && i != j) {

						// Create new candidates by joining seqs
						final Sequence seq1 = sortedSequences.get(i);
						final Sequence seq2 = sortedSequences.get(j);
						final Sequence cand = new Sequence(seq1, seq2);

						// Add candidate to queue
						if (cand != null && !rejected_seqs.contains(cand)) {
							if (!candidateSupports.containsKey(cand))
								candidateSupports
										.put(cand,
												getSupportOfSequence(
														transactions, cand));
							candidateQueue.add(cand);
							iteration++;
						}

						if (iteration >= maxSteps) // Queue limit exceeded
							break outerLoop; // finished building queue

					}
				}
			}
		}
		logger.info(" Finished bulding priority queue. Size: "
				+ candidateQueue.size() + "\n");
		// logger.info(" Time taken: " + (System.nanoTime() - startTime) / 1e6);
		// logger.finest(" Structural candidate itemsets: ");

		// Evaluate candidates with highest support first
		int counter = 0;
		for (Sequence topCandidate; (topCandidate = candidateQueue.poll()) != null;) {
			// logger.finest("\n Candidate: " + topCandidate + ", supp: "
			// + candidateSupports.get(topCandidate)
			// / (double) transactions.size());
			counter++;
			rejected_seqs.add(topCandidate); // candidate seen
			final boolean accepted = evaluateCandidate(sequences, transactions,
					inferenceAlgorithm, topCandidate);
			if (accepted == true) { // Better itemset found
				// update supports
				supports.put(topCandidate, candidateSupports.get(topCandidate));
				logger.info(" Number of eval calls: " + counter + "\n");
				return;
			}
		}

		if (iteration >= maxSteps) { // Priority queue exhausted
			logger.warning("\n Priority queue exhausted. Exiting. \n");
			transactions.setIterationLimitExceeded();
			// return; // No better itemset found
		}

		// No better itemset found
		logger.info("\n All possible candidates suggested. Exiting. \n");
		transactions.setIterationLimitExceeded();

	}

	/** Evaluate a candidate sequence to see if it should be included */
	private static boolean evaluateCandidate(
			final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm,
			final Sequence candidate) {

		logger.finer("\n Candidate: " + candidate);

		// Find cost in parallel
		Tuple2<Double, Double> costAndProb;
		// if (transactions instanceof TransactionRDD) {
		// costAndProb = SparkEMStep.structuralEMStep(transactions,
		// inferenceAlgorithm, candidate);
		// } else {
		costAndProb = EMStep.structuralEMStep(transactions, inferenceAlgorithm,
				candidate);
		// }
		final double curCost = costAndProb._1;
		final double prob = costAndProb._2;
		logger.finer(String.format(", cost: %.2f", curCost));

		// Return if better collection of seqs found
		if (curCost < transactions.getAverageCost()) {
			logger.finer("\n Candidate Accepted.\n");
			// Update cache with candidate
			Map<Sequence, Double> newSequences;
			// if (transactions instanceof TransactionRDD) {
			// newItemsets = SparkEMStep.addAcceptedCandidateCache(
			// transactions, candidate, prob);
			// } else {
			newSequences = EMStep.addAcceptedCandidateCache(transactions,
					candidate, prob);
			// }
			// Update sequences with newly inferred sequences
			sequences.clear();
			sequences.putAll(newSequences);
			transactions.setAverageCost(curCost);
			return true;
		} // otherwise keep trying

		// No better candidate found
		return false;
	}

	/** Sort sequences by interestingness */
	public static Map<Sequence, Double> sortSequences(
			final HashMap<Sequence, Double> sequences,
			final HashMap<Sequence, Double> intMap) {

		final Ordering<Sequence> comparator = Ordering
				.natural()
				.reverse()
				.onResultOf(Functions.forMap(intMap))
				.compound(
						Ordering.natural().reverse()
								.onResultOf(Functions.forMap(sequences)))
				.compound(Ordering.usingToString());
		final Map<Sequence, Double> sortedSequences = ImmutableSortedMap
				.copyOf(sequences, comparator);

		return sortedSequences;
	}

	/**
	 * Calculate interestingness as defined by i(S) = |z_S = 1|/|T : S in T|
	 * where |z_S = 1| is calculated by pi_S*|T| and |T : S in T| = supp(S)
	 */
	public static HashMap<Sequence, Double> calculateInterestingness(
			final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions) {

		final HashMap<Sequence, Double> interestingnessMap = new HashMap<>();

		// Calculate interestingness
		final long noTransactions = transactions.size();
		for (final Sequence seq : sequences.keySet()) {
			final double interestingness = sequences.get(seq) * noTransactions
					/ (double) getSupportOfSequence(transactions, seq);
			interestingnessMap.put(seq, interestingness);
		}

		return interestingnessMap;
	}

	/** Read output sequences from file (sorted by interestingness) */
	public static Map<Sequence, Double> readISMSequences(final File output)
			throws IOException {
		final HashMap<Sequence, Double> sequences = new HashMap<>();
		final HashMap<Sequence, Double> intMap = new HashMap<>();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		boolean found = false;
		for (final String line : lines) {

			if (found && !line.trim().isEmpty()) {
				final Sequence sequence = new Sequence();
				final String[] splitLine = line.split("\t");
				final String[] items = splitLine[0].split(",");
				items[0] = items[0].replace("[", "");
				items[items.length - 1] = items[items.length - 1].replace("]",
						"");
				for (final String item : items)
					sequence.add(Integer.parseInt(item.trim()));
				final double prob = Double
						.parseDouble(splitLine[1].split(":")[1]);
				final double intr = Double
						.parseDouble(splitLine[2].split(":")[1]);
				sequences.put(sequence, prob);
				intMap.put(sequence, intr);
			}

			if (line.contains("INTERESTING SEQUENCES"))
				found = true;
		}

		// Sort itemsets by interestingness
		final Map<Sequence, Double> sortedSequences = sortSequences(sequences,
				intMap);

		return sortedSequences;
	}

	/**
	 * This method scans the cached input database to calculate the support of a
	 * sequence.
	 *
	 * @return the support of the requested sequence
	 */
	public static int getSupportOfSequence(
			final TransactionDatabase transactions, final Sequence seq) {

		// Get sequence occurence
		final int occurence = seq.getOccurence();

		// Convert sequence to regex
		final StringBuilder sb = new StringBuilder(seq.size() * 2 + 1);
		String prefix = "(^| )";
		for (final int item : seq) {
			sb.append(prefix);
			sb.append(item);
			prefix = " .*? ";
		}
		sb.append(" ");
		final Pattern pattern = Pattern.compile(sb.toString());

		// for each line (transaction) until end of database
		int support = 0;
		for (final String line : transactions.getCachedDB()) {
			final Matcher matcher = pattern.matcher(line);
			int maxOccurence = 0;
			while (matcher.find())
				maxOccurence++;
			if (occurence <= maxOccurence)
				support++;
		}

		return support;
	}

}
