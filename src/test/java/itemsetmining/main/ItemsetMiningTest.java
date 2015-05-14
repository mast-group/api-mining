package itemsetmining.main;

import static org.junit.Assert.assertEquals;
import itemsetmining.itemset.Sequence;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;

import java.util.HashSet;

import org.junit.Test;

import com.google.common.collect.HashMultiset;

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
		final HashSet<Sequence> expected1 = new HashSet<>();
		expected1.add(s1);
		expected1.add(s2);
		expected1.add(s3);

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		final HashSet<Sequence> actual = inferGreedy.infer(transaction1);
		assertEquals(expected1, actual);

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

		// final double expectedCost2 = -Math.log(p4);

		// Test greedy
		final HashSet<Sequence> actual2 = inferGreedy.infer(transaction2);
		assertEquals(expected2, actual2);
		transaction2.setCachedCovering(actual2);
		// assertEquals(expectedCost2, transaction2.getCachedCost(), 1e-15);

	}

	// @Test
	// public void testGetSupportOfSequence() throws IOException {
	// final File inputFile = new File(
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/TOY.txt");
	// final TransactionList dBase = ItemsetMining.readTransactions(inputFile);
	//
	// final Sequence seq = new Sequence(1, 3);
	// int expectedSupp = 4;
	// assertEquals(expectedSupp,
	// ItemsetMiningCore.getSupportOfSequence(dBase, seq));
	//
	// final Sequence seq2 = new Sequence(1, 3);
	// seq2.incrementOccurence();
	// expectedSupp = 1;
	// assertEquals(expectedSupp,
	// ItemsetMiningCore.getSupportOfSequence(dBase, seq2));
	//
	// final Sequence seq3 = new Sequence(1, 3);
	// seq3.incrementOccurence();
	// seq3.incrementOccurence();
	// expectedSupp = 0;
	// assertEquals(expectedSupp,
	// ItemsetMiningCore.getSupportOfSequence(dBase, seq3));
	//
	// }

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
