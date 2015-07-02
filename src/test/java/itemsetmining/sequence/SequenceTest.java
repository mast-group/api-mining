package itemsetmining.sequence;

import static org.junit.Assert.assertEquals;

import itemsetmining.sequence.Sequence;
import itemsetmining.transaction.Transaction;

import java.util.BitSet;

import org.junit.Test;

public class SequenceTest {

	// @Test
	// public void testGetSupportOfSequenceWithGaps() throws IOException {
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
	// public void testGetSupportOfSequenceWithoutGaps() throws IOException {
	// final File inputFile = new File(
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/TOY.txt");
	// final TransactionList dBase = ItemsetMining.readTransactions(inputFile);
	//
	// final Sequence seq = new Sequence(1, 3);
	// int expectedSupp = 1;
	// assertEquals(expectedSupp,
	// ItemsetMiningCore.getSupportOfSequence(dBase, seq));
	//
	// final Sequence seq2 = new Sequence(2, 3);
	// expectedSupp = 3;
	// assertEquals(expectedSupp,
	// ItemsetMiningCore.getSupportOfSequence(dBase, seq2));
	//
	// }

	@Test
	public void testSequenceContainsWithGaps() {

		// Example from Agrawal paper
		final Sequence seq1 = new Sequence(3, 4, 5, 8);
		final Sequence seq2 = new Sequence(7, 3, 8, 9, 4, 5, 6, 8);
		final Sequence seq3 = new Sequence(3, 3, 8);

		assertEquals(true, seq2.contains(seq1));
		assertEquals(false, seq1.contains(seq2));
		assertEquals(false, seq1.contains(seq3));

		// Test transaction contains repetitions
		final Transaction trans1 = new Transaction(1, 2, 1, 2);
		final Transaction trans2 = new Transaction(3, 1, 4, 2, 5);
		final Transaction trans3 = new Transaction(3, 1, 4, 2, 5, 1, 6, 2, 7);
		final Sequence seq = new Sequence(1, 2);
		final Sequence seqR = new Sequence(1, 2);
		seqR.incrementOccurence();

		assertEquals(true, trans1.contains(seq));
		assertEquals(true, trans1.contains(seqR));
		assertEquals(true, trans2.contains(seq));
		assertEquals(false, trans2.contains(seqR));
		assertEquals(true, trans3.contains(seq));
		assertEquals(true, trans3.contains(seqR));
	}

	// @Test
	// public void testSequenceContainsWithoutGaps() {
	//
	// final Sequence seq1 = new Sequence(3, 4, 5, 8);
	// final Sequence seq2 = new Sequence(3, 3, 4, 5, 8, 6);
	// final Sequence seq3 = new Sequence(3, 4, 8, 9, 4, 5, 6, 8);
	//
	// assertEquals(true, seq2.contains(seq1));
	// assertEquals(false, seq1.contains(seq2));
	// assertEquals(false, seq3.contains(seq1));
	//
	// final Sequence transI = new Sequence(12763, 12823, 34913);
	// final Sequence seqI = new Sequence(34913);
	//
	// assertEquals(true, transI.contains(seqI));
	//
	// final Sequence transL = new Sequence(1, 2, 3, 4);
	// final Sequence seqL1 = new Sequence(4, 5);
	// final Sequence seqL2 = new Sequence(3, 4);
	//
	// assertEquals(false, transL.contains(seqL1));
	// assertEquals(true, transL.contains(seqL2));
	//
	// // Test transaction contains repetitions
	// final Transaction trans1 = new Transaction(1, 2, 1, 2);
	// final Transaction trans2 = new Transaction(3, 1, 2, 4);
	// final Transaction trans3 = new Transaction(3, 1, 2, 4, 1, 2, 5);
	// final Sequence seq = new Sequence(1, 2);
	// final Sequence seqR = new Sequence(1, 2);
	// seqR.incrementOccurence();
	//
	// assertEquals(true, trans1.contains(seq));
	// assertEquals(true, trans1.contains(seqR));
	// assertEquals(true, trans2.contains(seq));
	// assertEquals(false, trans2.contains(seqR));
	// assertEquals(true, trans3.contains(seq));
	// assertEquals(true, trans3.contains(seqR));
	//
	// }

	@Test
	public void testSequenceGetCoveredWithGapsWithoutOverlap() {

		final Sequence trans = new Sequence(7, 3, 8, 9, 4, 5, 6, 8);

		final Sequence seq1 = new Sequence(3, 4, 5, 8);
		final BitSet expected1 = new BitSet(trans.size());
		expected1.set(1);
		expected1.set(4);
		expected1.set(5);
		expected1.set(7);

		final Sequence seq2 = new Sequence(7, 9);
		final BitSet expected2 = new BitSet(trans.size());
		expected2.set(0);
		expected2.set(3);

		final Sequence seq3 = new Sequence(8, 4, 5);
		final BitSet expected3 = new BitSet(trans.size());
		expected3.set(2);
		expected3.set(4);
		expected3.set(5);

		// Seq not contained in trans
		final Sequence seq4 = new Sequence(3, 3, 8);
		final BitSet expected4 = new BitSet(trans.size());

		assertEquals(expected1, trans.getCovered(seq1, new BitSet()));
		assertEquals(expected2, trans.getCovered(seq2, new BitSet()));
		assertEquals(expected2, trans.getCovered(seq2, expected1));
		assertEquals(expected3, trans.getCovered(seq3, new BitSet()));
		assertEquals(expected3, trans.getCovered(seq3, expected2));
		assertEquals(expected4, trans.getCovered(seq4, new BitSet()));

		// Test covering without overlap
		assertEquals(new BitSet(), trans.getCovered(seq3, expected1));

		// Test double covering
		final Sequence transC = new Sequence(1, 2, 1, 2, 1, 2);
		final Sequence seqC = new Sequence(1, 2);
		final BitSet expectedC1 = new BitSet(transC.size());
		expectedC1.set(0);
		expectedC1.set(1);
		final BitSet expectedC2 = new BitSet(transC.size());
		expectedC2.set(2);
		expectedC2.set(3);
		final BitSet expectedC3 = new BitSet(transC.size());
		expectedC3.set(4);
		expectedC3.set(5);

		assertEquals(expectedC1, transC.getCovered(seqC, new BitSet()));
		assertEquals(expectedC2, transC.getCovered(seqC, expectedC1));
		expectedC2.or(expectedC1);
		assertEquals(expectedC3, transC.getCovered(seqC, expectedC2));

		// Test covering with single item sequence
		final Sequence transI = new Sequence(12763, 12823, 34913);
		final Sequence seqI = new Sequence(34913);
		final BitSet expectedI = new BitSet(transI.size());
		expectedI.set(2);

		assertEquals(expectedI, transI.getCovered(seqI, new BitSet()));

	}

	// @Test
	// public void testSequenceGetCoveredWithoutGapsWithoutOverlap() {
	//
	// final Sequence trans = new Sequence(3, 3, 4, 5, 8, 6);
	//
	// final Sequence seq1 = new Sequence(3, 4, 5, 8);
	// final BitSet expected1 = new BitSet(trans.size());
	// expected1.set(1);
	// expected1.set(2);
	// expected1.set(3);
	// expected1.set(4);
	//
	// final Sequence seq2 = new Sequence(8, 6);
	// final BitSet expected2 = new BitSet(trans.size());
	// expected2.set(4);
	// expected2.set(5);
	//
	// final Sequence seq3 = new Sequence(3, 4, 5);
	// final BitSet expected3 = new BitSet(trans.size());
	// expected3.set(1);
	// expected3.set(2);
	// expected3.set(3);
	//
	// // Seq not contained in trans
	// final Sequence seq4 = new Sequence(3, 3, 8);
	// final BitSet expected4 = new BitSet(trans.size());
	//
	// assertEquals(expected1, trans.getCovered(seq1, new BitSet()));
	// assertEquals(expected2, trans.getCovered(seq2, new BitSet()));
	// assertEquals(expected3, trans.getCovered(seq3, new BitSet()));
	// assertEquals(expected3, trans.getCovered(seq3, expected2));
	// assertEquals(expected4, trans.getCovered(seq4, new BitSet()));
	//
	// // Test covering without overlap
	// assertEquals(new BitSet(), trans.getCovered(seq2, expected1));
	// assertEquals(new BitSet(), trans.getCovered(seq3, expected1));
	// assertEquals(new BitSet(), trans.getCovered(seq4, expected1));
	//
	// // Test double covering
	// final Sequence transC = new Sequence(1, 2, 1, 2, 1, 2);
	// final Sequence seqC = new Sequence(1, 2);
	// final BitSet expectedC1 = new BitSet(transC.size());
	// expectedC1.set(0);
	// expectedC1.set(1);
	// final BitSet expectedC2 = new BitSet(transC.size());
	// expectedC2.set(2);
	// expectedC2.set(3);
	// final BitSet expectedC3 = new BitSet(transC.size());
	// expectedC3.set(4);
	// expectedC3.set(5);
	//
	// assertEquals(expectedC1, transC.getCovered(seqC, new BitSet()));
	// assertEquals(expectedC2, transC.getCovered(seqC, expectedC1));
	// expectedC2.or(expectedC1);
	// assertEquals(expectedC3, transC.getCovered(seqC, expectedC2));
	//
	// // Test covering with single item sequence
	// final Sequence transI = new Sequence(12763, 12823, 34913);
	// final Sequence seqI = new Sequence(34913);
	// final BitSet expectedI = new BitSet(transI.size());
	// expectedI.set(2);
	//
	// assertEquals(expectedI, transI.getCovered(seqI, new BitSet()));
	//
	// }

	// @Test
	// public void testSequenceGetCoveredWithGapsWithOverlap() {
	//
	// final Sequence trans = new Sequence(7, 3, 8, 9, 4, 5, 6, 8);
	//
	// final Sequence seq1 = new Sequence(3, 4, 5, 8);
	// final BitSet expected1 = new BitSet(trans.size());
	// expected1.set(1);
	// expected1.set(4);
	// expected1.set(5);
	// expected1.set(7);
	//
	// final Sequence seq2 = new Sequence(7, 9);
	// final BitSet expected2 = new BitSet(trans.size());
	// expected2.set(0);
	// expected2.set(3);
	//
	// final Sequence seq3 = new Sequence(8, 4, 5);
	// final BitSet expected3 = new BitSet(trans.size());
	// expected3.set(2);
	// expected3.set(4);
	// expected3.set(5);
	//
	// // Seq not contained in trans
	// final Sequence seq4 = new Sequence(3, 3, 8);
	// final BitSet expected4 = new BitSet(trans.size());
	//
	// assertEquals(expected1, trans.getCovered(seq1, new BitSet()));
	// assertEquals(expected2, trans.getCovered(seq2, new BitSet()));
	// assertEquals(expected2, trans.getCovered(seq2, expected1));
	// assertEquals(expected3, trans.getCovered(seq3, new BitSet()));
	// assertEquals(expected3, trans.getCovered(seq3, expected2));
	// assertEquals(expected4, trans.getCovered(seq4, new BitSet()));
	//
	// // Test covering with overlap
	// assertEquals(expected3, trans.getCovered(seq3, expected1));
	//
	// // Test double covering
	// final Sequence transC = new Sequence(1, 2, 1, 2, 1, 2);
	// final Sequence seqC = new Sequence(1, 2);
	// final BitSet expectedC1 = new BitSet(transC.size());
	// expectedC1.set(0);
	// expectedC1.set(1);
	// final BitSet expectedC2 = new BitSet(transC.size());
	// expectedC2.set(2);
	// expectedC2.set(3);
	// final BitSet expectedC3 = new BitSet(transC.size());
	// expectedC3.set(4);
	// expectedC3.set(5);
	//
	// assertEquals(expectedC1, transC.getCovered(seqC, new BitSet()));
	// assertEquals(expectedC2, transC.getCovered(seqC, expectedC1));
	// expectedC2.or(expectedC1);
	// assertEquals(expectedC3, transC.getCovered(seqC, expectedC2));
	//
	// // Test covering with single item sequence
	// final Sequence transI = new Sequence(12763, 12823, 34913);
	// final Sequence seqI = new Sequence(34913);
	// final BitSet expectedI = new BitSet(transI.size());
	// expectedI.set(2);
	//
	// assertEquals(expectedI, transI.getCovered(seqI, new BitSet()));
	//
	// }

	// @Test
	// public void testSequenceGetCoveredWithoutGapsWithOverlap() {
	//
	// final Sequence trans = new Sequence(3, 3, 4, 5, 8, 6);
	//
	// final Sequence seq1 = new Sequence(3, 4, 5, 8);
	// final BitSet expected1 = new BitSet(trans.size());
	// expected1.set(1);
	// expected1.set(2);
	// expected1.set(3);
	// expected1.set(4);
	//
	// final Sequence seq2 = new Sequence(8, 6);
	// final BitSet expected2 = new BitSet(trans.size());
	// expected2.set(4);
	// expected2.set(5);
	//
	// final Sequence seq3 = new Sequence(3, 4, 5);
	// final BitSet expected3 = new BitSet(trans.size());
	// expected3.set(1);
	// expected3.set(2);
	// expected3.set(3);
	//
	// // Seq not contained in trans
	// final Sequence seq4 = new Sequence(3, 3, 8);
	// final BitSet expected4 = new BitSet(trans.size());
	//
	// assertEquals(expected1, trans.getCovered(seq1, new BitSet()));
	// assertEquals(expected2, trans.getCovered(seq2, new BitSet()));
	// assertEquals(expected2, trans.getCovered(seq2, expected1));
	// assertEquals(expected3, trans.getCovered(seq3, new BitSet()));
	// assertEquals(expected3, trans.getCovered(seq3, expected2));
	// assertEquals(expected4, trans.getCovered(seq4, new BitSet()));
	//
	// // Test double covering
	// final Sequence transC = new Sequence(1, 2, 1, 2, 1, 2);
	// final Sequence seqC = new Sequence(1, 2);
	// final BitSet expectedC1 = new BitSet(transC.size());
	// expectedC1.set(0);
	// expectedC1.set(1);
	// final BitSet expectedC2 = new BitSet(transC.size());
	// expectedC2.set(2);
	// expectedC2.set(3);
	// final BitSet expectedC3 = new BitSet(transC.size());
	// expectedC3.set(4);
	// expectedC3.set(5);
	//
	// assertEquals(expectedC1, transC.getCovered(seqC, new BitSet()));
	// assertEquals(expectedC2, transC.getCovered(seqC, expectedC1));
	// expectedC2.or(expectedC1);
	// assertEquals(expectedC3, transC.getCovered(seqC, expectedC2));
	//
	// // Test covering with single item sequence
	// final Sequence transI = new Sequence(12763, 12823, 34913);
	// final Sequence seqI = new Sequence(34913);
	// final BitSet expectedI = new BitSet(transI.size());
	// expectedI.set(2);
	//
	// assertEquals(expectedI, transI.getCovered(seqI, new BitSet()));
	//
	// }

}
