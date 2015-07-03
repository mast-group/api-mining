package codemining.math.random;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import codemining.util.StatsUtil;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class SampleUtilsTest {

	@Test
	public void testRandomPartition() {
		// Uniform
		final Map<String, Double> partitionWeights = Maps.newHashMap();
		partitionWeights.put("A", 1.);
		partitionWeights.put("B", 1.);
		partitionWeights.put("C", 1.);

		final Map<Integer, Double> elementWeights = Maps.newHashMap();
		for (int i = 0; i < 300; i++) {
			elementWeights.put(i, 1.);
		}
		assertEquals(elementWeights.keySet().size(), 300);

		Multimap<String, Integer> partition = SampleUtils.randomPartition(
				elementWeights, partitionWeights);

		assertEquals(partition.get("A").size(), 100);
		assertEquals(partition.get("B").size(), 100);
		assertEquals(partition.get("C").size(), 100);

		// Weighted
		partitionWeights.put("A", 1.);
		partitionWeights.put("B", 5.);
		partitionWeights.put("C", 4.);

		partition = SampleUtils.randomPartition(elementWeights,
				partitionWeights);

		assertEquals(partition.get("A").size(), 30);
		assertEquals(partition.get("B").size(), 150);
		assertEquals(partition.get("C").size(), 120);

		// One is empty
		partitionWeights.put("A", 100.);
		partitionWeights.put("B", 0.);
		partitionWeights.put("C", 200.);

		partition = SampleUtils.randomPartition(elementWeights,
				partitionWeights);
		assertEquals(partition.get("A").size(), 100);
		assertEquals(partition.get("B").size(), 0);
		assertEquals(partition.get("C").size(), 200);
	}

	/**
	 * This test may be flakey, due to its probabilistic nature.
	 */
	@Test
	public void testRandomPartitionAvgBehavior() {
		// Check a random sample of 100 randomly created partitions and elements
		// that are statistically "ok".
		for (int i = 0; i < 100; i++) {
			final Map<Integer, Double> partitionWeights = Maps.newHashMap();
			final Map<Integer, Double> elementWeights = Maps.newHashMap();

			final int nPartitions = RandomUtils.nextInt(99) + 1;
			final int nElements = RandomUtils.nextInt(1000) + 5000;

			for (int partition = 0; partition < nPartitions; partition++) {
				partitionWeights.put(partition, RandomUtils.nextDouble());
			}
			for (int element = 0; element < nElements; element++) {
				elementWeights.put(element, RandomUtils.nextDouble());
			}

			final Multimap<Integer, Integer> partitions = SampleUtils
					.randomPartition(elementWeights, partitionWeights);
			final double partitionsTotalSum = StatsUtil.sum(partitionWeights
					.values());

			double sumDifference = 0;
			for (final int partitionId : partitions.keySet()) {
				final int partitionSize = partitions.get(partitionId).size();
				final double expectedPartitionPct = partitionWeights
						.get(partitionId) / partitionsTotalSum;
				final double actualPartitionPct = ((double) partitionSize)
						/ nElements;
				sumDifference += Math.abs(expectedPartitionPct
						- actualPartitionPct);
			}

			// On average we are not off by 1%
			assertEquals(sumDifference / partitions.keySet().size(), 0, 1E-2);

		}
	}
}
