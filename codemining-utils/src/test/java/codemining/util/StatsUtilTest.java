/**
 * 
 */
package codemining.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class StatsUtilTest {

	@Test
	public void testLogSumExpSet() {
		final List<Double> listOfNumbers = Lists.newArrayList();

		listOfNumbers.add(-10.);
		listOfNumbers.add(-10.);
		assertEquals(StatsUtil.log2SumOfExponentials(listOfNumbers), -9,
				10E-100);

		listOfNumbers.add(-1000.);
		assertEquals(StatsUtil.log2SumOfExponentials(listOfNumbers), -9,
				10E-100);
	}

	@Test
	public void testLogSumExpSimple() {
		final double log2prob = -1000;
		assertEquals(StatsUtil.log2SumOfExponentials(log2prob, log2prob), -999,
				10E-100);

		final double log2prob2 = -10000;
		assertEquals(StatsUtil.log2SumOfExponentials(log2prob2, log2prob2),
				-9999, 10E-100);

		assertEquals(StatsUtil.log2SumOfExponentials(log2prob, log2prob2),
				-1000, 10E-10);
	}

	@Test
	public void testNanBehaviour() {
		final List<Double> listOfNumbers = Lists.newArrayList();

		listOfNumbers.add(1.);
		listOfNumbers.add(2.);
		listOfNumbers.add(5.);
		listOfNumbers.add(10.);
		listOfNumbers.add(10.);
		listOfNumbers.add(Double.NaN);

		assertEquals(StatsUtil.max(listOfNumbers), 10, 10E-100);
		assertEquals(StatsUtil.min(listOfNumbers), 1, 10E-100);
		assertEquals(StatsUtil.mode(listOfNumbers), 10, 10E-100);
		assertEquals(StatsUtil.median(listOfNumbers), 7.5, 10E-100);
		assertEquals(StatsUtil.sum(listOfNumbers), Double.NaN, 10E-100);
		assertEquals(StatsUtil.mean(listOfNumbers), Double.NaN, 10E-10);
	}

	@Test
	public void testNormalizeLog2probs() {
		final Map<Integer, Double> testNumbers = Maps.newTreeMap();
		testNumbers.put(1, -100.);
		testNumbers.put(2, -100.);
		testNumbers.put(3, -100.);

		StatsUtil.normalizeLog2Probs(testNumbers);
		assertEquals(testNumbers.get(1), 1. / 3, 10E-10);
		assertEquals(testNumbers.get(2), 1. / 3, 10E-10);
		assertEquals(testNumbers.get(3), 1. / 3, 10E-10);

		testNumbers.clear();
		testNumbers.put(1, -100.);
		testNumbers.put(2, -1000.);
		testNumbers.put(3, -5000.);

		StatsUtil.normalizeLog2Probs(testNumbers);
		assertEquals(testNumbers.get(1), 1., 10E-10);
		assertEquals(testNumbers.get(2), 0, 10E-10);
		assertEquals(testNumbers.get(3), 0, 10E-10);

	}

	@Test
	public void testSimpleFunctions() {
		final List<Double> listOfNumbers = Lists.newArrayList();

		listOfNumbers.add(-1.);
		listOfNumbers.add(1.);
		listOfNumbers.add(2.);
		listOfNumbers.add(5.);
		listOfNumbers.add(10.);
		listOfNumbers.add(10.);
		listOfNumbers.add(100.);

		assertEquals(StatsUtil.max(listOfNumbers), 100, 10E-100);
		assertEquals(StatsUtil.min(listOfNumbers), -1, 10E-100);
		assertEquals(StatsUtil.mode(listOfNumbers), 10, 10E-100);
		assertEquals(StatsUtil.median(listOfNumbers), 5, 10E-100);
		assertEquals(StatsUtil.sum(listOfNumbers), 127, 10E-100);
		assertEquals(StatsUtil.mean(listOfNumbers), 127. / 7, 10E-10);
	}

}
