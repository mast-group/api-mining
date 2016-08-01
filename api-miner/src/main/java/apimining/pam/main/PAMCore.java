package apimining.pam.main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import com.google.common.base.Charsets;
import com.google.common.base.Functions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.io.Files;

import apimining.pam.main.InferenceAlgorithms.InferenceAlgorithm;
import apimining.pam.sequence.Sequence;
import apimining.pam.transaction.Transaction;
import apimining.pam.transaction.TransactionDatabase;
import apimining.pam.transaction.TransactionList;
import apimining.pam.util.Logging;
import apimining.pam.util.Tuple2;

public abstract class PAMCore {

	/** Main fixed settings */
	private static final int OPTIMIZE_PARAMS_EVERY = 1;
	private static final double OPTIMIZE_TOL = 1e-5;

	protected static final Logger logger = Logger.getLogger(PAMCore.class.getName());
	public static final File LOG_DIR = new File("/tmp/");

	/** Variable settings */
	protected static Level LOG_LEVEL = Level.FINE;
	protected static long MAX_RUNTIME = 24 * 60 * 60 * 1_000; // 24hrs

	/** Mine interesting sequences */
	public static Map<Sequence, Double> mineInterestingSequences(final File inputFile,
			final InferenceAlgorithm inferenceAlgorithm, final int maxStructureSteps, final int maxEMIterations,
			final File logFile) throws IOException {

		// Set up logging
		if (logFile != null)
			Logging.setUpFileLogger(logger, LOG_LEVEL, logFile);
		else
			Logging.setUpConsoleLogger(logger, LOG_LEVEL);

		// Echo input parameters
		logger.info("========== INTERESTING SEQUENCE MINING ============");
		logger.info("\n Time: " + new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss").format(new Date()));
		logger.info("\n Inputs: -f " + inputFile + " -s " + maxStructureSteps + " -i " + maxEMIterations + " -r "
				+ MAX_RUNTIME / 60_000);

		// Read in transaction database
		final TransactionList transactions = readTransactions(inputFile);

		// Determine most frequent singletons
		final Multiset<Sequence> singletons = scanDatabaseToDetermineFrequencyOfSingleItems(inputFile);

		// Run inference to find interesting sequences
		logger.fine("\n============= SEQUENCE INFERENCE =============\n");
		final HashMap<Sequence, Double> sequences = structuralEM(transactions, singletons, inferenceAlgorithm,
				maxStructureSteps, maxEMIterations);
		if (LOG_LEVEL.equals(Level.FINEST))
			logger.finest(
					"\n======= Transaction Database =======\n" + Files.toString(inputFile, Charsets.UTF_8) + "\n");

		// Sort sequences by probability
		final HashMap<Sequence, Double> intMap = calculateInterestingness(sequences, transactions);
		final Map<Sequence, Double> sortedSequences = sortSequences(sequences, intMap);

		logger.info("\n============= INTERESTING SEQUENCES =============\n");
		for (final Entry<Sequence, Double> entry : sortedSequences.entrySet()) {
			logger.info(String.format("%s\tprob: %1.5f \tint: %1.5f %n", entry.getKey(), entry.getValue(),
					intMap.get(entry.getKey())));
		}
		logger.info("\n");

		return sortedSequences;
	}

	/**
	 * Learn itemsets model using structural EM
	 */
	protected static HashMap<Sequence, Double> structuralEM(final TransactionDatabase transactions,
			final Multiset<Sequence> singletons, final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations) {

		// Start timer
		final long startTime = System.currentTimeMillis();

		// Initialize itemset cache
		EMStep.initializeCachedItemsets(transactions, singletons);

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
				return candidateSupports.get(seq2) - candidateSupports.get(seq1);
			}
		}.compound(Ordering.usingToString());

		// Initialize average cost per transaction for singletons
		expectationMaximizationStep(sequences, transactions, inferenceAlgorithm);

		// Structural EM
		boolean breakLoop = false;
		for (int iteration = 1; iteration <= maxEMIterations; iteration++) {

			// Learn structure
			logger.finer("\n----- Itemset Combination at Step " + iteration + "\n");
			combineSequencesStep(sequences, transactions, rejected_seqs, inferenceAlgorithm, maxStructureSteps,
					supportOrdering, supports, candidateSupportOrdering, candidateSupports);
			if (transactions.getIterationLimitExceeded())
				breakLoop = true;
			logger.finer(String.format(" Average cost: %.2f%n", transactions.getAverageCost()));

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0 || iteration == maxEMIterations || breakLoop == true) {
				logger.fine("\n***** Parameter Optimization at Step " + iteration + "\n");
				expectationMaximizationStep(sequences, transactions, inferenceAlgorithm);
			}

			// Break loop if requested
			if (breakLoop)
				break;

			// Check if time exceeded
			if (System.currentTimeMillis() - startTime > MAX_RUNTIME) {
				logger.warning("\nRuntime limit of " + MAX_RUNTIME / (60. * 1000.) + " minutes exceeded.\n");
				break;
			}

			if (iteration == maxEMIterations)
				logger.warning("\nEM iteration limit exceeded.\n");
		}
		logger.info("\nElapsed time: " + (System.currentTimeMillis() - startTime) / (60. * 1000.) + " minutes.\n");

		return sequences;
	}

	/**
	 * Find optimal parameters for given set of sequences and store in sequences
	 *
	 * @return TransactionDatabase with the average cost per transaction
	 *         <p>
	 *         NB. zero probability sequences are dropped
	 */
	private static void expectationMaximizationStep(final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions, final InferenceAlgorithm inferenceAlgorithm) {

		logger.fine(" Structure Optimal Sequences: " + sequences + "\n");

		Map<Sequence, Double> prevSequences = sequences;

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Set up storage
			final Map<Sequence, Double> newSequences;

			newSequences = EMStep.hardEMStep(transactions.getTransactionList(), inferenceAlgorithm);

			// If set has stabilised calculate norm(p_prev - p_new)
			if (prevSequences.keySet().equals(newSequences.keySet())) {
				norm = 0;
				for (final Sequence seq : prevSequences.keySet()) {
					norm += Math.pow(prevSequences.get(seq) - newSequences.get(seq), 2);
				}
				norm = Math.sqrt(norm);
			}

			prevSequences = newSequences;
		}

		EMStep.calculateAndSetAverageCost(transactions);

		sequences.clear();
		sequences.putAll(prevSequences);
		logger.fine(" Parameter Optimal Sequences: " + sequences + "\n");
		logger.fine(String.format(" Average cost: %.2f%n", transactions.getAverageCost()));
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
	private static void combineSequencesStep(final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions, final Set<Sequence> rejected_seqs,
			final InferenceAlgorithm inferenceAlgorithm, final int maxSteps,
			final Ordering<Sequence> sequenceSupportOrdering, final HashMap<Sequence, Integer> supports,
			final Ordering<Sequence> candidateSupportOrdering, final HashMap<Sequence, Integer> candidateSupports) {

		// Set up support-ordered priority queue
		final PriorityQueue<Sequence> candidateQueue = new PriorityQueue<Sequence>(maxSteps, candidateSupportOrdering);

		// Sort sequences according to given ordering
		final ArrayList<Sequence> sortedSequences = new ArrayList<>(sequences.keySet());
		Collections.sort(sortedSequences, sequenceSupportOrdering);

		// Find maxSteps superseqs for all seqs
		// final long startTime = System.nanoTime();
		int noAdded = 0;
		int istart = 0;
		int jstart = 0;
		int kstart = 0;
		boolean exhausted = true;
		final int len = sortedSequences.size();
		while (noAdded < maxSteps && exhausted) {
			exhausted = false;
			int noUncached = 0;
			final HashSet<Sequence> uncachedCandidates = new HashSet<>();
			outerLoop: for (int k = kstart; k < 2 * len - 2; k++) {
				for (int i = istart; i < len && i < k + 1; i++) {
					for (int j = jstart; j < len && i + j < k + 1; j++) {
						if (k <= i + j && i != j) {

							// Create new candidates by joining seqs
							final Sequence seq1 = sortedSequences.get(i);
							final Sequence seq2 = sortedSequences.get(j);
							final Sequence cand = new Sequence(seq1, seq2);

							// Add candidate to queue
							if (cand != null && !rejected_seqs.contains(cand)) {
								final Integer supp = candidateSupports.get(cand);
								if (supp == null) {
									uncachedCandidates.add(cand);
									noUncached++;
								} else { // add cached candidate to queue
									candidateQueue.add(cand);
									noAdded++;
								}
							}

							// Possibly found enough candidates
							if (noAdded + noUncached >= maxSteps) {
								istart = i;
								jstart = j + 1;
								kstart = k;
								exhausted = true;
								break outerLoop;
							}
						}
					}
					jstart = 0;
				}
				istart = 0;
			}

			// Add uncached candidates to queue
			final Map<Sequence, Long> candidatesWithSupports = EMStep.getSupportsOfSequences(transactions,
					uncachedCandidates);
			for (final Entry<Sequence, Long> entry : candidatesWithSupports.entrySet()) {
				final Sequence cand = entry.getKey();
				final int supp = Math.toIntExact(entry.getValue());
				if (supp > 0) { // ignore unsupported sequences
					candidateSupports.put(cand, supp);
					candidateQueue.add(cand);
					noAdded++;
				}
			}
		}
		logger.info(" Finished bulding priority queue. Size: " + candidateQueue.size() + "\n");
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
			final boolean accepted = evaluateCandidate(sequences, transactions, inferenceAlgorithm, topCandidate);
			if (accepted == true) { // Better itemset found
				// update supports
				supports.put(topCandidate, candidateSupports.get(topCandidate));
				logger.info(" Number of eval calls: " + counter + "\n");
				return;
			}
		}

		if (exhausted) { // Priority queue exhausted
			logger.warning("\n Priority queue exhausted. Exiting. \n");
			transactions.setIterationLimitExceeded();
			// return; // No better itemset found
		}

		// No better itemset found
		logger.info("\n All possible candidates suggested. Exiting. \n");
		transactions.setIterationLimitExceeded();

	}

	/** Evaluate a candidate sequence to see if it should be included */
	private static boolean evaluateCandidate(final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions, final InferenceAlgorithm inferenceAlgorithm,
			final Sequence candidate) {

		logger.finer("\n Candidate: " + candidate);

		// Find cost in parallel
		Tuple2<Double, Double> costAndProb;
		costAndProb = EMStep.structuralEMStep(transactions, inferenceAlgorithm, candidate);
		final double curCost = costAndProb._1;
		final double prob = costAndProb._2;
		logger.finer(String.format(", cost: %.2f", curCost));

		// Return if better collection of seqs found
		if (curCost < transactions.getAverageCost()) {
			logger.finer("\n Candidate Accepted.\n");
			// Update cache with candidate
			final Map<Sequence, Double> newSequences = EMStep.addAcceptedCandidateCache(transactions, candidate, prob);
			// Update sequences with newly inferred sequences
			sequences.clear();
			sequences.putAll(newSequences);
			transactions.setAverageCost(curCost);
			return true;
		} // otherwise keep trying

		// No better candidate found
		return false;
	}

	/** Sort sequences by probability */
	public static Map<Sequence, Double> sortSequences(final HashMap<Sequence, Double> sequences,
			final HashMap<Sequence, Double> intMap) {

		final Ordering<Sequence> comparator = Ordering.natural().reverse().onResultOf(Functions.forMap(sequences))
				.compound(Ordering.natural().reverse().onResultOf(Functions.forMap(intMap)))
				.compound(Ordering.usingToString());
		final Map<Sequence, Double> sortedSequences = ImmutableSortedMap.copyOf(sequences, comparator);

		return sortedSequences;
	}

	/**
	 * Calculate interestingness as defined by i(S) = |z_S = 1|/|T : S in T|
	 * where |z_S = 1| is calculated by pi_S*|T| and |T : S in T| = supp(S)
	 */
	public static HashMap<Sequence, Double> calculateInterestingness(final HashMap<Sequence, Double> sequences,
			final TransactionDatabase transactions) {

		final HashMap<Sequence, Double> interestingnessMap = new HashMap<>();

		// Calculate supports
		final Map<Sequence, Long> supports = EMStep.getSupportsOfSequences(transactions, sequences.keySet());

		// Calculate interestingness
		final long noTransactions = transactions.size();
		for (final Sequence seq : sequences.keySet()) {
			final double interestingness = sequences.get(seq) * noTransactions / (double) supports.get(seq);
			interestingnessMap.put(seq, Math.round(interestingness * 1E10) / 1E10);
		}

		return interestingnessMap;
	}

	public static TransactionList readTransactions(final File inputFile) throws IOException {

		final List<Transaction> transactions = new ArrayList<>();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the transaction into items
			final String[] lineSplited = line.split(" ");
			// convert to Transaction class and add it to the structure
			transactions.add(getTransaction(lineSplited));

		}
		// close the input file
		LineIterator.closeQuietly(it);

		return new TransactionList(transactions);
	}

	/**
	 * Create and add the Transaction in the String array
	 *
	 * @param integers
	 *            one line of integers in the sequence database
	 */
	public static Transaction getTransaction(final String[] integers) {
		final Transaction sequence = new Transaction();

		for (int i = 0; i < integers.length; i++) {
			if (integers[i].equals("-1")) { // end of item

			} else if (integers[i].equals("-2")) { // end of sequence
				return sequence;
			} else { // extract the value for an item
				sequence.add(Integer.parseInt(integers[i]));
			}
		}
		throw new RuntimeException("Corrupt sequence database.");
	}

	/**
	 * This method scans the input database to calculate the support of single
	 * items.
	 *
	 * @param inputFile
	 *            the input file
	 * @return a multiset for storing the support of each singleton
	 */
	public static Multiset<Sequence> scanDatabaseToDetermineFrequencyOfSingleItems(final File inputFile)
			throws IOException {

		final Multiset<Sequence> singletons = HashMultiset.create();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the line into items
			final String[] lineSplit = line.split(" ");
			// for each item
			final HashSet<Sequence> seenItems = new HashSet<>();
			for (final String itemString : lineSplit) {
				final int item = Integer.parseInt(itemString);
				if (item >= 0) { // ignore end of itemset/sequence tags
					final Sequence seq = new Sequence(item);
					PAMCore.recursiveSetOccurrence(seq, seenItems); // set
																	// occurrence
					seenItems.add(seq); // add item to seen
				}
			}
			singletons.addAll(seenItems); // increase the support of the items
		}

		// close the input file
		LineIterator.closeQuietly(it);

		return singletons;
	}

	private static void recursiveSetOccurrence(final Sequence seq, final HashSet<Sequence> seenItems) {
		if (seenItems.contains(seq)) {
			seq.incrementOccurence();
			recursiveSetOccurrence(seq, seenItems);
		}
	}

	/** Read output sequences from file (sorted by probability) */
	public static Map<Sequence, Double> readISMSequences(final File output) throws IOException {
		final HashMap<Sequence, Double> sequences = new HashMap<>();
		final HashMap<Sequence, Double> intMap = new HashMap<>();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		boolean found = false;
		for (final String line : lines) {

			if (found && !line.trim().isEmpty()) {
				final Sequence sequence = new Sequence();
				final String[] splitLine = line.split("\t");
				final String[] seq = splitLine[0].split("\\^");
				if (seq.length > 1) {
					final int occur = Integer.parseInt(seq[1].replaceAll("[()]", ""));
					for (int i = 0; i < occur - 1; i++)
						sequence.incrementOccurence();
				}
				final String[] items = seq[0].split(",");
				items[0] = items[0].replace("[", "");
				items[items.length - 1] = items[items.length - 1].replace("]", "");
				for (final String item : items)
					sequence.add(Integer.parseInt(item.trim()));
				final double prob = Double.parseDouble(splitLine[1].split(":")[1]);
				final double intr = Double.parseDouble(splitLine[2].split(":")[1]);
				sequences.put(sequence, prob);
				intMap.put(sequence, intr);
			}

			if (line.contains("INTERESTING SEQUENCES"))
				found = true;
		}

		// Sort sequences by probability
		final Map<Sequence, Double> sortedSequences = sortSequences(sequences, intMap);

		return sortedSequences;
	}

}
