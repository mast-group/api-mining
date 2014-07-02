package itemsetmining.itemset;

import static org.junit.Assert.assertEquals;
import itemsetmining.transaction.Transaction;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class BitSetIteratorTest {

	@Test
	public void testIterator() {

		final Transaction transaction = new Transaction(1, 2, 3, 4);

		final List<Integer> expected = Lists.newArrayList(1, 2, 3, 4);
		final List<Integer> actual = Lists.newArrayList();
		for (final Integer item : transaction) {
			actual.add(item);
		}
		assertEquals(expected, actual);

	}

}
