package itemsetmining.main;

import static org.junit.Assert.assertEquals;
import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.Sequence;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Sets;

public class ItemsetMiningTest {

	@Test
	public void testDoInference() {

		// Subsequences
		final Sequence s1 = new Sequence(new Itemset(3), new Itemset(4, 5),
				new Itemset(8));
		final double p1 = 0.4;
		final Sequence s2 = new Sequence(new Itemset(7), new Itemset(9));
		final double p2 = 0.3;
		final Sequence s3 = new Sequence(new Itemset(8), new Itemset(4, 5, 6));
		final double p3 = 0.2;

		// Transaction #1
		final Transaction transaction1 = new Transaction(new Itemset(7),
				new Itemset(3, 8), new Itemset(9), new Itemset(4, 5, 6),
				new Itemset(8));
		transaction1.initializeCachedSequences(HashMultiset.create(), 0);
		transaction1.addSequenceCache(s1, p1);
		transaction1.addSequenceCache(s2, p2);
		transaction1.addSequenceCache(s3, p3);

		// Expected solution #1
		final Set<Sequence> expected1 = Sets.newHashSet(s1, s2, s3);

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		final Set<Sequence> actual = inferGreedy.infer(transaction1);
		assertEquals(expected1, actual);

	}

}
