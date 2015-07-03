/**
 * 
 */
package codemining.util.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pair Tests
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class PairTest {

	@Test
	public void testPair() {
		final Pair<Integer, String> pair1 = Pair.create(1, "test");
		final Pair<Integer, String> pair2 = Pair.create(1, "diff");
		final Pair<Integer, String> pair3 = Pair.create(2, "test");
		final Pair<Integer, String> pair4 = Pair.create(1, "test");

		assertEquals(pair1, pair4);
		assertEquals(pair1.hashCode(), pair4.hashCode());

		assertFalse(pair1.equals(pair2));
		assertFalse(pair1.equals(pair3));
		assertTrue(pair1.hashCode() != pair2.hashCode());
		assertTrue(pair1.hashCode() != pair3.hashCode());
	}

	@Test
	public void testUnorderedPair() {
		final UnorderedPair<Integer> pair1 = UnorderedPair
				.createUnordered(1, 2);
		final UnorderedPair<Integer> pair2 = UnorderedPair
				.createUnordered(1, 2);
		final UnorderedPair<Integer> pair3 = UnorderedPair
				.createUnordered(2, 1);
		final UnorderedPair<Integer> pair4 = UnorderedPair
				.createUnordered(2, 3);
		final UnorderedPair<Integer> pair5 = UnorderedPair
				.createUnordered(3, 1);

		assertEquals(pair1, pair2);
		assertEquals(pair1, pair3);
		assertEquals(pair2, pair3);

		assertEquals(pair1.hashCode(), pair2.hashCode());
		assertEquals(pair1.hashCode(), pair3.hashCode());
		assertEquals(pair2.hashCode(), pair3.hashCode());

		assertFalse(pair1.equals(pair4));
		assertFalse(pair1.equals(pair5));
		assertFalse(pair2.equals(pair4));
		assertFalse(pair2.equals(pair5));
		assertFalse(pair3.equals(pair4));
		assertFalse(pair3.equals(pair5));
	}
}
