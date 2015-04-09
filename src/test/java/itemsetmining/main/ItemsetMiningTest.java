package itemsetmining.main;

import static org.junit.Assert.assertEquals;
import itemsetmining.itemset.Sequence;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;

import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class ItemsetMiningTest {

	@Test
	public void testDoInference() {

		// Subsequences
		final Sequence s1 = new Sequence(3, 4, 5, 8);
		final double p1 = 0.4;
		final Sequence s2 = new Sequence(7, 9);
		final double p2 = 0.3;
		final Sequence s3 = new Sequence(8, 4, 5, 6);
		final double p3 = 0.2;

		// Transaction #1
		final Transaction transaction1 = new Transaction(7, 3, 8, 9, 4, 5, 6, 8);
		transaction1.initializeCachedSequences(HashMultiset.create(), 0);
		transaction1.addSequenceCache(s1, p1);
		transaction1.addSequenceCache(s2, p2);
		transaction1.addSequenceCache(s3, p3);

		// Expected solution #1
		final Multiset<Sequence> expected1 = HashMultiset.create();
		expected1.add(s1);
		expected1.add(s2);
		expected1.add(s3);

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		final Multiset<Sequence> actual = inferGreedy.infer(transaction1);
		assertEquals(expected1, actual);

		// Subsequences
		final Sequence s4 = new Sequence(1, 2);
		final double p4 = 0.5;

		// Transaction #1
		final Transaction transaction2 = new Transaction(1, 2, 1, 2, 1, 2);
		transaction2.initializeCachedSequences(HashMultiset.create(), 0);
		transaction2.addSequenceCache(s4, p4);

		// Expected solution #1
		final Multiset<Sequence> expected2 = HashMultiset.create();
		expected2.add(s4, 3);
		final double expectedCost2 = -Math.log(p4) - Math.log(p4)
				- Math.log(p4);

		// Test greedy
		final Multiset<Sequence> actual2 = inferGreedy.infer(transaction2);
		assertEquals(expected2, actual2);
		transaction2.setCachedCovering(actual2);
		assertEquals(expectedCost2, transaction2.getCachedCost(), 1e-15);

	}

	// @Test
	// public void testGetSupportOfSequence() {
	// final File inputFile = new File(
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/TOY.txt");
	// final Sequence seq = new Sequence(1, 3);
	// final int expectedSupp = 5;
	// assertEquals(expectedSupp,
	// ItemsetMiningCore.getSupportOfSequence(inputFile, seq));
	// }

	// @Test
	// public void testCombLoop() {
	//
	// final ArrayList<Sequence> sequences = Lists.newArrayList();
	// for (int i = 1; i < 10; i++)
	// sequences.add(new Sequence(i));
	//
	// final int len = sequences.size();
	// for (int k = 0; k < 2 * len - 1; k++) {
	// for (int i = 0; i < len && i < k + 1; i++) {
	// for (int j = 0; j < len && i + j < k + 1; j++) {
	// if (k <= i + j) {
	// final Sequence s1 = sequences.get(i);
	// final Sequence s2 = sequences.get(j);
	// System.out.println(s1.toString() + s2.toString());
	// }
	// }
	// }
	// }
	//
	// }

}
