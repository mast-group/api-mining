package apimining.pam.main;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Multiset;

import apimining.pam.main.InferenceAlgorithms.InferenceAlgorithm;
import apimining.pam.sequence.Sequence;
import apimining.pam.transaction.Transaction;
import apimining.pam.transaction.TransactionDatabase;
import apimining.pam.util.Tuple2;

/** Class to hold the various transaction EM Steps */
public class EMStep {

	/** Initialize cached itemsets */
	static void initializeCachedItemsets(final TransactionDatabase transactions, final Multiset<Sequence> singletons) {
		final long noTransactions = transactions.size();
		transactions.getTransactionList().parallelStream()
				.forEach(t -> t.initializeCachedSequences(singletons, noTransactions));
	}

	/** EM-step for hard EM */
	static Map<Sequence, Double> hardEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm) {
		final double noTransactions = transactions.size();

		// E-step
		final Map<Sequence, Long> coveringWithCounts = transactions.parallelStream().map(t -> {
			final HashSet<Sequence> covering = inferenceAlgorithm.infer(t);
			t.setCachedCovering(covering);
			return covering;
		}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Sequence, Double> newSequences = coveringWithCounts.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() / noTransactions));

		// Update cached itemsets
		transactions.parallelStream().forEach(t -> t.updateCachedSequences(newSequences));

		return newSequences;
	}

	/** Get average cost of last EM-step */
	static void calculateAndSetAverageCost(final TransactionDatabase transactions) {
		final double noTransactions = transactions.size();
		final double averageCost = transactions.getTransactionList().parallelStream()
				.mapToDouble(Transaction::getCachedCost).sum() / noTransactions;
		transactions.setAverageCost(averageCost);
	}

	/** EM-step for structural EM */
	static Tuple2<Double, Double> structuralEMStep(final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Sequence candidate) {
		final double noTransactions = transactions.size();

		// E-step (adding candidate to transactions that support it)
		final Map<Sequence, Long> coveringWithCounts = transactions.getTransactionList().parallelStream().map(t -> {
			if (t.contains(candidate)) {
				t.addSequenceCache(candidate, 1.0);
				final HashSet<Sequence> covering = inferenceAlgorithm.infer(t);
				t.setTempCachedCovering(covering);
				return covering;
			}
			return t.getCachedCovering();
		}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Sequence, Double> newSequences = coveringWithCounts.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() / noTransactions));

		// Get average cost (removing candidate from supported transactions)
		final double averageCost = transactions.getTransactionList().parallelStream().mapToDouble(t -> {
			double cost;
			if (t.contains(candidate))
				cost = t.getTempCachedCost(newSequences);
			else
				cost = t.getCachedCost(newSequences);
			t.removeSequenceCache(candidate);
			return cost;
		}).sum() / noTransactions;

		// Get candidate prob
		Double prob = newSequences.get(candidate);
		if (prob == null)
			prob = 0.;

		return new Tuple2<Double, Double>(averageCost, prob);
	}

	/** Add accepted candidate itemset to cache */
	static Map<Sequence, Double> addAcceptedCandidateCache(final TransactionDatabase transactions,
			final Sequence candidate, final double prob) {
		final double noTransactions = transactions.size();

		// Cached E-step (adding candidate to transactions that support it)
		final Map<Sequence, Long> coveringWithCounts = transactions.getTransactionList().parallelStream().map(t -> {
			if (t.contains(candidate)) {
				t.addSequenceCache(candidate, prob);
				final HashSet<Sequence> covering = t.getTempCachedCovering();
				t.setCachedCovering(covering);
				return covering;
			}
			return t.getCachedCovering();
		}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Sequence, Double> newSequences = coveringWithCounts.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() / noTransactions));

		// Update cached itemsets
		transactions.getTransactionList().parallelStream().forEach(t -> t.updateCachedSequences(newSequences));

		return newSequences;
	}

	/** Get the support of given sequences */
	static Map<Sequence, Long> getSupportsOfSequences(final TransactionDatabase transactions,
			final Set<Sequence> sequences) {
		return transactions.getTransactionList().parallelStream().map(t -> {
			final HashSet<Sequence> supportedSeqs = new HashSet<>();
			for (final Sequence seq : sequences) {
				if (t.contains(seq))
					supportedSeqs.add(seq);
			}
			return supportedSeqs;
		}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));
	}

	private EMStep() {
	}

}
