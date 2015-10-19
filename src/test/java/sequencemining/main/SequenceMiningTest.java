package sequencemining.main;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import com.google.common.collect.HashMultiset;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.InferenceAlgorithms.InferenceAlgorithm;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.Transaction;

public class SequenceMiningTest {

	@Test
	public void testDoInference() {

		// Subsequences
		final Sequence s1 = new Sequence(3, 4, 5, 8);
		final double p1 = 0.4;
		final Sequence s2 = new Sequence(7, 9);
		final double p2 = 0.3;
		// final Sequence s3 = new Sequence(8, 4, 5, 6); // with overlap
		// final double p3 = 0.2;
		final Sequence s3 = new Sequence(8, 6);
		final double p3 = 0.2;

		// Transaction #1
		final Transaction transaction1 = new Transaction(7, 3, 8, 9, 4, 5, 6, 8);
		transaction1.initializeCachedSequences(HashMultiset.create(), 0);
		transaction1.addSequenceCache(s1, p1);
		transaction1.addSequenceCache(s2, p2);
		transaction1.addSequenceCache(s3, p3);

		// Expected solution #1
		final HashSet<Sequence> expected1 = new HashSet<>();
		expected1.add(s1);
		expected1.add(s2);
		expected1.add(s3);
		final HashSet<Integer> order1 = new HashSet<>();
		order1.add(0);
		order1.add(1);
		order1.add(2);

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		final HashSet<Sequence> actual = inferGreedy.infer(transaction1);
		assertEquals(expected1, actual);
		// assertTrue(order1.containsAll(actual.values()));

		// Subsequences
		final Sequence s4 = new Sequence(1, 2);
		final double p4 = 0.5;
		final Sequence s42 = new Sequence(1, 2);
		s42.incrementOccurence();
		final double p42 = 0.4;
		final Sequence s43 = new Sequence(1, 2);
		s43.incrementOccurence();
		s43.incrementOccurence();
		final double p43 = 0.3;

		// Transaction #2
		final Transaction transaction2 = new Transaction(1, 2, 1, 2, 1, 2);
		transaction2.initializeCachedSequences(HashMultiset.create(), 0);
		transaction2.addSequenceCache(s4, p4);
		transaction2.addSequenceCache(s42, p42);
		transaction2.addSequenceCache(s43, p43);

		// Expected solution #2
		final HashSet<Sequence> expected2 = new HashSet<>();
		expected2.add(s4);
		expected2.add(s42);
		expected2.add(s43);
		final HashSet<Integer> order2 = new HashSet<>();
		order2.add(0);
		order2.add(2);
		order2.add(4);

		// final double expectedCost2 = -Math.log(p4);

		// Test greedy
		final HashSet<Sequence> actual2 = inferGreedy.infer(transaction2);
		assertEquals(expected2, actual2);
		// assertTrue(order2.containsAll(actual2.values()));
		// transaction2.setCachedCovering(actual2);
		// assertEquals(expectedCost2, transaction2.getCachedCost(), 1e-15);

	}

	// @Test
	// public void testCombLoop() {
	//
	// final ArrayList<Sequence> sequences = new ArrayList<>();
	// for (int i = 1; i < 10; i++)
	// sequences.add(new Sequence(i));
	//
	// final int len = sequences.size();
	// for (int k = 0; k < 2 * len - 2; k++) {
	// for (int i = 0; i < len && i < k + 1; i++) {
	// for (int j = 0; j < len && i + j < k + 1; j++) {
	// if (k <= i + j && i != j) {
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
