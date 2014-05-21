package itemsetmining.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ItemsetMiningTests {

	@Test
	public void testDoInference() {

		final LinkedHashMap<Itemset, Double> itemsets = Maps.newLinkedHashMap();

		itemsets.put(new Itemset(1), 0.2);
		itemsets.put(new Itemset(2), 0.2);
		itemsets.put(new Itemset(3), 0.4);
		itemsets.put(new Itemset(4), 0.4);
		final Itemset s12 = new Itemset(new int[] { 1, 2 });
		final Itemset s23 = new Itemset(new int[] { 2, 3 });
		final Itemset s24 = new Itemset(new int[] { 2, 4 });
		final Itemset s34 = new Itemset(new int[] { 3, 4 });
		final double p12 = 0.4;
		final double p23 = 0.2;
		final double p24 = 0.2;
		final double p34 = 0.2;
		itemsets.put(s12, p12);
		itemsets.put(s23, p23);
		itemsets.put(s24, p24);
		itemsets.put(s34, p34);

		double actualCost = 0;
		double expectedCost = 0;
		final Set<Itemset> actual = Sets.newHashSet();
		final Set<Itemset> expected = Sets.newHashSet();
		final Set<Integer> actualItems = Sets.newHashSet();

		// Transactions
		final Transaction transaction1234 = new Transaction(new int[] { 1, 2,
				3, 4 });
		final Transaction transaction234 = new Transaction(
				new int[] { 2, 3, 4 });

		// Expected solution
		expectedCost = -Math.log(p12) - Math.log(p34);
		expected.add(s12);
		expected.add(s34);

		// Test greedy
		actual.clear();
		actualCost = ItemsetMining.inferGreedy(actual, itemsets,
				transaction1234);
		assertEquals(expected, actual);
		assertEquals(expectedCost, actualCost, 1e-15);

		actual.clear();
		ItemsetMining.inferGreedy(actual, itemsets, transaction234);
		assertEquals(expected, actual);
		assertEquals(expectedCost, actualCost, 1e-15);

		// Test primal-dual (only gives rough approximation)
		actual.clear();
		actualCost = ItemsetMining.inferPrimalDual(actual, itemsets,
				new Transaction(new int[] { 1, 2, 3, 4 }));
		actualItems.clear();
		for (final Itemset set : actual)
			actualItems.addAll(set.getItems());
		assertTrue(actualItems.containsAll(transaction1234.getItems()));

		actual.clear();
		ItemsetMining.inferPrimalDual(actual, itemsets, new Transaction(
				new int[] { 2, 3, 4 }));
		actualItems.clear();
		for (final Itemset set : actual)
			actualItems.addAll(set.getItems());
		assertTrue(actualItems.containsAll(transaction234.getItems()));

		// Test ILP
		actual.clear();
		actualCost = ItemsetMining.inferILP(actual, itemsets, transaction1234);
		assertEquals(expected, actual);
		assertEquals(expectedCost, actualCost, 1e-15);

		actual.clear();
		ItemsetMining.inferILP(actual, itemsets, transaction234);
		assertEquals(expected, actual);
		assertEquals(expectedCost, actualCost, 1e-15);

	}
}
