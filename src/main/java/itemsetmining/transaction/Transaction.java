package itemsetmining.transaction;

import itemsetmining.itemset.AbstractSequence;
import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.Sequence;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.clearspring.analytics.util.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/** A transaction is an ordered list of items */
public class Transaction extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/** Cached itemsets for this transaction */
	private HashMap<Sequence, Double> cachedSequences;

	/** Cached covering for this transaction */
	private HashSet<Sequence> cachedCovering;
	private HashSet<Sequence> tempCachedCovering;

	public void initializeCachedSequences(final Multiset<Integer> singletons,
			final long noTransactions) {
		cachedSequences = Maps.newHashMap();
		for (final Multiset.Entry<Integer> entry : singletons.entrySet()) {
			if (this.contains(entry.getElement()))
				cachedSequences.put(new Sequence(
						new Itemset(entry.getElement())), entry.getCount()
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
			else
				it.remove();
		}
	}

	/** Get cost of cached covering for hard EM-step */
	public double getCachedCost() {
		double totalCost = 0;
		for (final Entry<Sequence, Double> entry : cachedSequences.entrySet()) {
			if (cachedCovering.contains(entry.getKey()))
				totalCost += -Math.log(entry.getValue());
			else
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
			final HashSet<Sequence> covering) {
		double totalCost = 0;
		for (final Entry<Sequence, Double> entry : cachedSequences.entrySet()) {
			final Sequence seq = entry.getKey();
			final Double prob = sequences.get(seq);
			if (prob != null) {
				if (covering.contains(seq))
					totalCost += -Math.log(prob);
				else
					totalCost += -Math.log(1 - prob);
			}
		}
		return totalCost;
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

	/**
	 * Constructor
	 */
	public Transaction() {
		this.itemsets = Lists.newArrayList();
	}

	/**
	 * Constructor
	 *
	 * @param itemsets
	 *            an array of itemsets that should be added to the new sequence
	 */
	public Transaction(final Itemset... itemsets) {
		this.itemsets = Lists.newArrayList(Arrays.asList(itemsets));
	}

}