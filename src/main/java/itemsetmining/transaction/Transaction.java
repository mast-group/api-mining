package itemsetmining.transaction;

import itemsetmining.itemset.AbstractItemset;
import itemsetmining.itemset.Itemset;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/** A transaction is an ordered list of items */
public class Transaction extends AbstractItemset implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/** Cached itemsets for this transaction */
	private HashMap<Itemset, Double> cachedItemsets;

	/** Cached covering for this transaction */
	private HashSet<Itemset> cachedCovering;
	private HashSet<Itemset> tempCachedCovering;

	public void initializeCachedItemsets(final Multiset<Integer> singletons,
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

	public void addItemsetCache(final Itemset candidate, final double prob) {
		cachedItemsets.put(candidate, prob);
	}

	public void removeItemsetCache(final Itemset candidate) {
		cachedItemsets.remove(candidate);
	}

	public void updateCachedItemsets(final Map<Itemset, Double> newItemsets) {
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

	/** Get cost of cached covering for hard EM-step */
	public double getCachedCost() {
		double totalCost = 0;
		for (final Entry<Itemset, Double> entry : cachedItemsets.entrySet()) {
			if (cachedCovering.contains(entry.getKey()))
				totalCost += -Math.log(entry.getValue());
			else
				totalCost += -Math.log(1 - entry.getValue());
		}
		return totalCost;
	}

	/** Get the cost of cached covering for structural EM-step */
	public double getCachedCost(final Map<Itemset, Double> itemsets) {
		double totalCost = 0;
		for (final Entry<Itemset, Double> entry : cachedItemsets.entrySet()) {
			final Itemset set = entry.getKey();
			final Double prob = itemsets.get(set);
			if (prob != null) {
				if (tempCachedCovering.contains(set))
					totalCost += -Math.log(prob);
				else
					totalCost += -Math.log(1 - prob);
			}
		}
		return totalCost;
	}

	public void setCachedCovering(final HashSet<Itemset> covering) {
		cachedCovering = covering;
	}

	public HashSet<Itemset> getCachedCovering() {
		return cachedCovering;
	}

	public void setTempCachedCovering(final HashSet<Itemset> covering) {
		tempCachedCovering = covering;
	}

	public HashSet<Itemset> getTempCachedCovering() {
		return tempCachedCovering;
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