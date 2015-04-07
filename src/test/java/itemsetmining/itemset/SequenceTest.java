package itemsetmining.itemset;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import org.junit.Test;

public class SequenceTest {

	@Test
	public void testSequenceContains() {

		// Example from Agrawal paper
		final Sequence seq1 = new Sequence(new Itemset(3), new Itemset(4, 5),
				new Itemset(8));
		final Sequence seq2 = new Sequence(new Itemset(7), new Itemset(3, 8),
				new Itemset(9), new Itemset(4, 5, 6), new Itemset(8));
		final Sequence seq3 = new Sequence(new Itemset(3), new Itemset(3, 8));

		assertEquals(true, seq2.contains(seq1));
		assertEquals(false, seq1.contains(seq2));
		assertEquals(false, seq1.contains(seq3));

	}

	@Test
	public void testSequenceGetCovered() {

		final Sequence trans = new Sequence(new Itemset(7), new Itemset(3, 8),
				new Itemset(9), new Itemset(4, 5, 6), new Itemset(8));

		final Sequence seq1 = new Sequence(new Itemset(3), new Itemset(4, 5),
				new Itemset(8));
		final BitSet expected1 = new BitSet(trans.size());
		expected1.set(1);
		expected1.set(4);
		expected1.set(5);
		expected1.set(7);

		final Sequence seq2 = new Sequence(new Itemset(7), new Itemset(9));
		final BitSet expected2 = new BitSet(trans.size());
		expected2.set(0);
		expected2.set(3);

		final Sequence seq3 = new Sequence(new Itemset(8), new Itemset(4, 5));
		final BitSet expected3 = new BitSet(trans.size());
		expected3.set(2);
		expected3.set(4);
		expected3.set(5);

		// Finally, seq not contained in trans
		final Sequence seq4 = new Sequence(new Itemset(3), new Itemset(3, 8));
		final BitSet expected4 = new BitSet(trans.size());

		assertEquals(expected1, trans.getCovered(seq1));
		assertEquals(expected2, trans.getCovered(seq2));
		assertEquals(expected3, trans.getCovered(seq3));
		assertEquals(expected4, trans.getCovered(seq4));

	}

	@Test
	public void testSequenceCode() {

		final Sequence seq = new Sequence(new Itemset(7), new Itemset(3, 8),
				new Itemset(9), new Itemset(4, 5, 6), new Itemset(8));
		final String expectedCode = "7;3,8;9;4,5,6;8";
		final String actualCode = seq.toCode();
		assertEquals(expectedCode, actualCode);
		final Sequence seqFromCode = Sequence.fromCode(actualCode);
		assertEquals(seq, seqFromCode);

	}

	@Test
	public void testSequenceJoin() {

		final Sequence seq1 = new Sequence(new Itemset(0), new Itemset(1, 2),
				new Itemset(3));
		final String code1 = seq1.toCode();
		final Sequence seq2 = new Sequence(new Itemset(2), new Itemset(3, 4),
				new Itemset(5));
		final String code2 = seq2.toCode();

		final String expectedCode = "0;1,2;3,4;5";
		assertEquals(expectedCode, Sequence.join(code1, code2));
		assertEquals(null, Sequence.join(code2, code1));

	}
}
