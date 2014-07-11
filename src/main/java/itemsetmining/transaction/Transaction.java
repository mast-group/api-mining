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

/** A transaction is an ordered list of items */
public class Transaction extends AbstractItemset implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/** Cached itemsets for this transaction */
	private HashMap<Itemset, Double> cachedItemsets;

	/** Cached itemsets dropped during candidate eval */
	private static HashMap<Itemset, Double> droppedItemsets;

	public void initializeCache(final Set<Integer> singletons, final double prob) {
		cachedItemsets = Maps.newHashMap();
		for (final int singleton : singletons) {
			if (this.contains(singleton))
				cachedItemsets.put(new Itemset(singleton), prob);
		}
	}

	public HashMap<Itemset, Double> getCachedItemsets() {
		return cachedItemsets;
	}

	public void addItemsetCache(final Itemset candidate, final double prob) {

		// Initialize dropped itemsets
		droppedItemsets = Maps.newHashMap();

		// Adjust probabilities for subsets of candidate
		for (final Iterator<Entry<Itemset, Double>> it = cachedItemsets
				.entrySet().iterator(); it.hasNext();) {

			final Entry<Itemset, Double> entry = it.next();
			if (candidate.contains(entry.getKey())) {
				final double newProb = entry.getValue() - prob;
				if (newProb > 0.0) {
					entry.setValue(newProb);
				} else {
					droppedItemsets.put(entry.getKey(), entry.getValue());
					it.remove();
				}
			}

		}

		// Add candidate if it supports this transaction
		if (this.contains(candidate))
			cachedItemsets.put(candidate, prob);
	}

	public void removeItemsetCache(final Itemset candidate, final double prob) {

		// Remove candidate
		cachedItemsets.remove(candidate);

		// Restore probabilities prior to adding candidate
		for (final Entry<Itemset, Double> entry : cachedItemsets.entrySet()) {
			if (candidate.contains(entry.getKey()))
				cachedItemsets.put(entry.getKey(), entry.getValue() + prob);
		}

		// And restore dropped itemsets
		cachedItemsets.putAll(droppedItemsets);
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