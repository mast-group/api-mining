package itemsetmining.transaction;

import itemsetmining.itemset.AbstractSequence;
import itemsetmining.itemset.Sequence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Multiset;

/** A transaction is an ordered list of items */
public class Transaction extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/** Cached itemsets for this transaction */
	private HashMap<Sequence, Double> cachedSequences;

	/** Cached covering for this transaction */
	private HashSet<Sequence> cachedCovering;
	private HashSet<Sequence> tempCachedCovering;

	public void initializeCachedSequences(final Multiset<Sequence> singletons,
			final long noTransactions) {
		cachedSequences = new HashMap<>();
		for (final com.google.common.collect.Multiset.Entry<Sequence> entry : singletons
				.entrySet()) {
			if (this.contains(entry.getElement()))
				cachedSequences.put(entry.getElement(), entry.getCount()
						/ (double) noTransactions);
		}
	}

	public HashMap<Sequence, Double> getCachedSequences() {
		return cachedSequences;
	}

	public void addSequenceCache(final Sequence candidate, final double prob) {
		cachedSequences.put(candidate, prob);
	}

	public void removeSequenceCache(final Sequence candidate) {
		cachedSequences.remove(candidate);
	}

	public void updateCachedSequences(final Map<Sequence, Double> newSequences) {
		for (final Iterator<Entry<Sequence, Double>> it = cachedSequences
				.entrySet().iterator(); it.hasNext();) {
			final Entry<Sequence, Double> entry = it.next();
			final Double newProb = newSequences.get(entry.getKey());
			if (newProb != null)
				entry.setValue(newProb);
			else if (entry.getKey().size() == 1)
				entry.setValue(0.); // so we can fill incomplete coverings
			else
				it.remove();
		}
	}

	/** Get cost of cached covering for hard EM-step */
	public double getCachedCost() {
		double totalCost = 0;
		int lenCovering = 0;
		for (final Entry<Sequence, Double> entry : cachedSequences.entrySet()) {
			final Sequence seq = entry.getKey();
			if (cachedCovering.contains(seq) && !entry.getValue().equals(0.)) {
				totalCost += -Math.log(entry.getValue())
						+ sumLogRange(lenCovering + 1, lenCovering + seq.size())
						- sumLogRange(1, seq.size());
				lenCovering += seq.size();
			} else
				totalCost += -Math.log(1 - entry.getValue());
		}
		return totalCost;
	}

	/** Get cost of cached covering for structural EM-step */
	public double getCachedCost(final Map<Sequence, Double> sequences) {
		return calculateCachedCost(sequences, cachedCovering);
	}

	/** Get cost of temp. cached covering for structural EM-step */
	public double getTempCachedCost(final Map<Sequence, Double> sequences) {
		return calculateCachedCost(sequences, tempCachedCovering);
	}

	/** Calculate cached cost for structural EM-step */
	private double calculateCachedCost(final Map<Sequence, Double> sequences,
			final Set<Sequence> covering) {
		double totalCost = 0;
		int lenCovering = 0;
		for (final Entry<Sequence, Double> entry : cachedSequences.entrySet()) {
			final Sequence seq = entry.getKey();
			final Double prob = sequences.get(seq);
			if (prob != null) {
				if (covering.contains(seq) && !entry.getValue().equals(0.)) {
					totalCost += -Math.log(prob)
							+ sumLogRange(lenCovering + 1,
									lenCovering + seq.size())
							- sumLogRange(1, seq.size());
					lenCovering += seq.size();
				} else
					totalCost += -Math.log(1 - prob);
			}
		}
		return totalCost;
	}

	private double sumLogRange(final int a, final int b) {
		double sum = 0;
		for (int i = a; i <= b; i++)
			sum += Math.log(i);
		return sum;
	}

	public void setCachedCovering(final HashSet<Sequence> covering) {
		cachedCovering = covering;
	}

	public HashSet<Sequence> getCachedCovering() {
		return cachedCovering;
	}

	public void setTempCachedCovering(final HashSet<Sequence> covering) {
		tempCachedCovering = covering;
	}

	public HashSet<Sequence> getTempCachedCovering() {
		return tempCachedCovering;
	}

	// /** Get the sequence transitions for this transaction */
	// public HashMap<Sequence, Sequence> getTransitions() {
	// final HashMap<Sequence, Sequence> transitions = new HashMap<>();
	// for (final Entry<Sequence, Integer> entry1 : cachedCovering.entrySet()) {
	// final int position1 = entry1.getValue();
	// Sequence nextSeq = null;
	// int nextPosition = Integer.MAX_VALUE;
	// for (final Entry<Sequence, Integer> entry2 : cachedCovering
	// .entrySet()) {
	// final int position2 = entry2.getValue();
	// if (!entry1.equals(entry2) && position1 <= position2
	// && position2 < nextPosition) {
	// nextSeq = entry2.getKey();
	// nextPosition = position2;
	// }
	// }
	// transitions.put(entry1.getKey(), nextSeq);
	// }
	// return transitions;
	// }

	/**
	 * Constructor
	 */
	public Transaction() {
		this.items = new ArrayList<>();
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            an array of items that should be added to the new sequence
	 */
	public Transaction(final Integer... items) {
		this.items = new ArrayList<>(Arrays.asList(items));
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            a List of items that should be added to the new sequence
	 */
	public Transaction(final List<Integer> items) {
		this.items = new ArrayList<>(items);
	}

}