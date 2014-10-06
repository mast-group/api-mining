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

	/** Cached itemsets for this transaction */
	private HashMap<Itemset, Double> cachedItemsets;

	/** Cached itemsets dropped during candidate eval */
	private HashMap<Itemset, Double> droppedItemsets;

	public void initializeCache(final Multiset<Integer> singletons,
			final long noTransactions) {
		cachedItemsets = Maps.newHashMap();
		for (final Multiset.Entry<Integer> entry : singletons.entrySet()) {
			if (this.contains(entry.getElement()))
				cachedItemsets.put(new Itemset(entry.getElement()),
						entry.getCount() / (double) noTransactions);
		}
	}

	public HashMap<Itemset, Double> getCachedItemsets() {
		return cachedItemsets;
	}

	public void addItemsetCache(final Itemset candidate, final double prob,
			final Set<Itemset> subsets) {

		// Initialize dropped itemsets
		droppedItemsets = Maps.newHashMap();

		// Adjust probabilities for direct subsets of candidate
		for (final Itemset subset : subsets) {
			final Double oldProb = cachedItemsets.get(subset);
			if (oldProb != null) { // subset supports this transaction
				final double newProb = oldProb - prob;
				if (newProb > 0.0) {
					cachedItemsets.put(subset, newProb);
				} else {
					droppedItemsets.put(subset, oldProb);
					cachedItemsets.remove(subset);
				}
			}
		}

		// Add candidate if it supports this transaction
		if (this.contains(candidate))
			cachedItemsets.put(candidate, prob);
	}

	public void removeItemsetCache(final Itemset candidate, final double prob,
			final Set<Itemset> subsets) {

		// Remove candidate
		cachedItemsets.remove(candidate);

		// Restore probabilities prior to adding candidate
		for (final Itemset subset : subsets) {
			final Double oldProb = cachedItemsets.get(subset);
			if (oldProb != null) // subset supports this transaction
				cachedItemsets.put(subset, oldProb + prob);
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