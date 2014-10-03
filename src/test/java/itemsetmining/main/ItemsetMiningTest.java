package itemsetmining.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferPrimalDual;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;

import java.util.HashMap;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ItemsetMiningTest {

	@Test
	public void testDoInference() {

		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();

		final Itemset s1 = new Itemset(1);
		itemsets.put(s1, 0.2);
		itemsets.put(new Itemset(2), 0.2);
		itemsets.put(new Itemset(3), 0.4);
		final Itemset s4 = new Itemset(4);
		final double p4 = 0.4;
		itemsets.put(s4, p4);

		final Itemset s12 = new Itemset(1, 2);
		final Itemset s23 = new Itemset(2, 3);
		final Itemset s24 = new Itemset(2, 4);
		final Itemset s34 = new Itemset(3, 4);
		final double p12 = 0.4;
		final double p23 = 0.3;
		final double p24 = 0.2;
		final double p34 = 0.4;
		itemsets.put(s12, p12);
		itemsets.put(s23, p23);
		itemsets.put(s24, p24);
		itemsets.put(s34, p34);

		double actualCost = 0;
		final Set<Itemset> actual = Sets.newHashSet();
		final Set<Integer> actualItems = Sets.newHashSet();

		// Transaction #1
		final Transaction transaction1234 = new Transaction(1, 2, 3, 4);

		// Expected solution #1
		double expectedCost1234 = -Math.log(p12) - Math.log(p34);
		final Set<Itemset> expected1234 = Sets.newHashSet(s12, s34);
		for (final Itemset set : Sets.difference(itemsets.keySet(),
				expected1234)) {
			expectedCost1234 += -Math.log(1 - itemsets.get(set));
		}

		// Transaction #2
		final Transaction transaction234 = new Transaction(2, 3, 4);

		// Expected solution #2
		double expectedCost234 = -1 * Math.log(p23) - Math.log(p34);
		final Set<Itemset> expected234 = Sets.newHashSet(s23, s34);
		for (final Itemset set : Sets
				.difference(itemsets.keySet(), expected234)) {
			if (!(set.equals(s1) || set.equals(s12)))
				expectedCost234 += -Math.log(1 - itemsets.get(set));
		}

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		actual.clear();
		actualCost = inferGreedy.infer(actual, itemsets, transaction1234);
		assertEquals(expected1234, actual);
		assertEquals(expectedCost1234, actualCost, 1e-15);

		actual.clear();
		actualCost = inferGreedy.infer(actual, itemsets, transaction234);
		assertEquals(expected234, actual);
		assertEquals(expectedCost234, actualCost, 1e-15);

		// Test primal-dual (only gives rough approximation)
		final InferenceAlgorithm inferPrimalDual = new InferPrimalDual();
		actual.clear();
		actualCost = inferPrimalDual.infer(actual, itemsets, transaction1234);
		actualItems.clear();
		for (final Itemset set : actual)
			actualItems.addAll(set);
		assertTrue(actualItems.containsAll(transaction1234));

		actual.clear();
		actualCost = inferPrimalDual.infer(actual, itemsets, transaction234);
		actualItems.clear();
		for (final Itemset set : actual)
			actualItems.addAll(set);
		assertTrue(actualItems.containsAll(transaction234));

		// Test ILP // TODO mavenize so libcplex is found
		// final InferenceAlgorithm inferILP = new inferILP();
		// actual.clear();
		// actualCost = inferILP.infer(actual, itemsets, transaction1234);
		// assertEquals(expected1234, actual);
		// assertEquals(expectedCost1234, actualCost, 1e-15);
		//
		// actual.clear();
		// actualCost = inferILP.infer(actual, itemsets, transaction234);
		// assertEquals(expected234, actual);
		// assertEquals(expectedCost234, actualCost, 1e-15);

	}

	@Test
	public void testDirectSubsets() {

		Set<Itemset> itemsets = Sets.newHashSet();
		itemsets.add(new Itemset(1, 2));
		itemsets.add(new Itemset(17));
		itemsets.add(new Itemset(18));
		itemsets.add(new Itemset(33));
		itemsets.add(new Itemset(39));
		itemsets.add(new Itemset(33, 39));
		itemsets.add(new Itemset(18, 33, 39));

		Set<Itemset> expectedDirectSubsets = Sets.newHashSet();
		expectedDirectSubsets.add(new Itemset(17));
		expectedDirectSubsets.add(new Itemset(18, 33, 39));

		Set<Itemset> actualDirectSubsets = ItemsetMiningCore.getDirectSubsets(
				itemsets, new Itemset(17, 18, 33, 39));
		assertEquals(expectedDirectSubsets, actualDirectSubsets);
	}

}
