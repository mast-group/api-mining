package itemsetmining.main;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ItemsetMiningTests {

	@Test
	public void testDoInferenceGreedy() {

		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();

		itemsets.put(new Itemset(1), 0.2);
		itemsets.put(new Itemset(2), 0.2);
		itemsets.put(new Itemset(3), 0.4);
		itemsets.put(new Itemset(4), 0.4);
		final Itemset s12 = new Itemset(new int[] { 1, 2 });
		final Itemset s34 = new Itemset(new int[] { 3, 4 });
		itemsets.put(s12, 0.4);
		itemsets.put(new Itemset(new int[] { 2, 3 }), 0.2);
		itemsets.put(new Itemset(new int[] { 2, 4 }), 0.2);
		itemsets.put(s34, 0.2);

		final Set<Itemset> actual = Sets.newHashSet();
		ItemsetMining.inferGreedy(actual, itemsets, new Transaction(new int[] {
				1, 2, 3, 4 }));
		final Set<Itemset> expected = Sets.newHashSet();
		expected.add(s12);
		expected.add(s34);
		assertEquals(expected, actual);

		actual.clear();
		ItemsetMining.inferGreedy(actual, itemsets, new Transaction(new int[] {
				2, 3, 4 }));
		assertEquals(expected, actual);

	}
}
