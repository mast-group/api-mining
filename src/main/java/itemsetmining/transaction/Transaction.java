package itemsetmining.transaction;

import itemsetmining.itemset.AbstractItemset;
import itemsetmining.itemset.Itemset;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/** A transaction is an ordered list of items */
public class Transaction extends AbstractItemset implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/** Cached transaction cost */
	private double cachedCost;
	private double prevCachedCost;

	/** Cached itemsets for this transaction */
	private HashMap<Itemset, Double> cachedItemsets;

	/** Cached itemsets that get negative probs during candidate eval */
	private HashMap<Itemset, Double> negativeItemsets;

	public void initializeCache(final Multiset<Integer> singletons,
			final long noTransactions) {
		cachedItemsets = Maps.newHashMap();
		cachedCost = 0;
		for (final Multiset.Entry<Integer> entry : singletons.entrySet()) {
			if (this.contains(entry.getElement())) {
				final double support = entry.getCount()
						/ (double) noTransactions;
				cachedItemsets.put(new Itemset(entry.getElement()), support);
				cachedCost -= Math.log(support);
			}
		}
	}

	public HashMap<Itemset, Double> getCachedItemsets() {
		return cachedItemsets;
	}

	public boolean addItemsetCache(final Itemset candidate, final double prob,
			final Set<Itemset> subsets) {

		// Initialize negative itemsets
		negativeItemsets = Maps.newHashMap();

		// Adjust probabilities for direct subsets of candidate
		boolean hasChanged = false;
		for (final Itemset subset : subsets) {
			final Double oldProb = cachedItemsets.get(subset);
			if (oldProb != null) { // subset supports this transaction
				hasChanged = true;
				final double newProb = oldProb - prob;
				if (newProb > 0.0) {
					cachedItemsets.put(subset, newProb);
				} else {
					negativeItemsets.put(subset, newProb);
					cachedItemsets.put(subset, 1e-10);
				}
			}
		}

		// Add candidate if it supports this transaction
		if (this.contains(candidate)) {
			hasChanged = true;
			cachedItemsets.put(candidate, prob);
		}

		if (hasChanged)
			prevCachedCost = cachedCost;

		return hasChanged;
	}

	public void removeItemsetCache(final Itemset candidate, final double prob,
			final Set<Itemset> subsets) {

		// Restore cached cost
		cachedCost = prevCachedCost;

		// Remove candidate
		cachedItemsets.remove(candidate);

		// Restore negative itemsets
		cachedItemsets.putAll(negativeItemsets);

		// Restore probabilities prior to adding candidate
		for (final Itemset subset : subsets) {
			final Double oldProb = cachedItemsets.get(subset);
			if (oldProb != null) // subset supports this transaction
				cachedItemsets.put(subset, oldProb + prob);
		}

	}

	public void updateCacheProbabilities(
			final HashMap<Itemset, Double> newItemsets) {

		for (final Iterator<Entry<Itemset, Double>> it = cachedItemsets
				.entrySet().iterator(); it.hasNext();) {

			final Entry<Itemset, Double> entry = it.next();
			final Double newProb = newItemsets.get(entry.getKey());
			if (newProb != null)
				entry.setValue(newProb);
			else
				it.remove();

		}

	}

	public void setCost(final double cost) {
		cachedCost = cost;
	}

	public double getCost() {
		return cachedCost;
	}

	/**
	 * Constructor
	 */
	public Transaction() {
		this.items = new BitSet();
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            a collection of items that should be added to the transaction
	 */
	public Transaction(final int... items) {
		this.items = new BitSet(items.length);
		add(items);
	}

}