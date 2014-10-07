package itemsetmining.transaction;

import static org.junit.Assert.assertTrue;
import itemsetmining.itemset.Itemset;

import java.util.HashMap;

import org.junit.Test;

public class TransactionTest {

	@Test
	public void testBackgroundItemsets() {

		final HashMap<Itemset, Double> itemsets = TransactionGenerator
				.generateBackgroundItemsets(10, 0.5, 15, 1., 1.);
		System.out.println(itemsets);

		// Check that all items >= 20
		for (final Itemset set : itemsets.keySet()) {
			for (final int item : set) {
				assertTrue(item >= 20);
			}
		}

	}

}
