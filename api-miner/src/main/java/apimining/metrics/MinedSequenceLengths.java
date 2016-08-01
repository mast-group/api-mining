package apimining.metrics;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

/** Show average length of mined API call sequences */
public class MinedSequenceLengths {

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Settings
		final String[] projects = new String[] { "netty", "hadoop", "twitter4j", "mahout", "neo4j", "drools",
				"andengine", "spring-data-neo4j", "camel", "weld", "resteasy", "webobjects", "wicket",
				"restlet-framework-java", "cloud9", "hornetq", "spring-data-mongodb" };
		final int topN = 100;

		double avgSizePAM = 0;
		double avgSizeMAPO = 0;
		double avgSizeUPMiner = 0;
		int count = 0;
		for (int i = 0; i < projects.length; i++) {
			if (projects[i].matches("hadoop") || projects[i].matches("neo4j"))
				continue;

			// System.out.println("Project " + projects[i]);

			// Dataset calls
			final List<List<String>> datasetCalls = ExampleCoverage.getDatasetAPICalls(projects[i]);

			// PAM
			final LinkedHashSet<List<String>> PAMCalls = ExampleCoverage.getPAMCalls(projects[i], "PAM_seqs.txt", 500);
			avgSizePAM += getTopSequencesAverageSize(PAMCalls, topN);

			// MAPO
			final LinkedHashSet<List<String>> MAPOCalls = ExampleCoverage.getClusteredCalls(projects[i], "mapo", 500,
					datasetCalls);
			avgSizeMAPO += getTopSequencesAverageSize(MAPOCalls, topN);

			// UPMiner
			final LinkedHashSet<List<String>> UPMinerCalls = ExampleCoverage.getClusteredCalls(projects[i], "upminer",
					500, datasetCalls);
			avgSizeUPMiner += getTopSequencesAverageSize(UPMinerCalls, topN);

			// System.out.println("PAM Avg. Size " + avgSizePAM);
			// System.out.println("MAPO Avg. Size " + avgSizeMAPO);
			// System.out.println("UPMiner Avg. Size " + avgSizeUPMiner);

			count++;
		}

		System.out.println("PAM Avg. Size " + avgSizePAM / count);
		System.out.println("MAPO Avg. Size " + avgSizeMAPO / count);
		System.out.println("UPMiner Avg. Size " + avgSizeUPMiner / count);

	}

	private static double getTopSequencesAverageSize(final LinkedHashSet<List<String>> sequences, final int topN) {
		if (sequences.size() == 0)
			return 0;
		double avgSize = 0;
		int count = 0;
		for (final List<String> seq : sequences) {
			avgSize += seq.size();
			count++;
			if (count == topN)
				return avgSize / count;
		}
		System.out.println("WARNING: Not enough sequences:" + count);
		return avgSize / count;
	}

}
