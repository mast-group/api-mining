package apimining.metrics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Show concrete examples of mined API call methods */
public class ExampleUsage {

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Settings
		final String[] projects = new String[] { "netty", "hadoop", "twitter4j", "mahout", "neo4j", "drools",
				"andengine", "spring-data-neo4j", "camel", "weld", "resteasy", "webobjects", "wicket",
				"restlet-framework-java", "cloud9", "hornetq", "spring-data-mongodb" };
		final String[] projFQNames = new String[] { "io.netty", "org.apache.hadoop", "twitter4j", "org.apache.mahout",
				"org.neo4j", "org.drools", "org.andengine", "org.springframework.data.neo4j", "org.apache.camel",
				"org.jboss.weld", "org.jboss.resteasy", "com.webobjects", "org.apache.wicket", "org.restlet",
				"edu.umd.cloud9", "org.hornetq", "org.springframework.data.mongodb" };
		final int topN = 10;

		final String use = "twitter4j";
		for (int i = 0; i < projects.length; i++) {
			if (!projects[i].matches(use))
				continue;

			final File saveFile = new File(ExampleCoverage.baseFolder + projects[i] + "_TopCalls.txt");
			final PrintWriter out = new PrintWriter(saveFile, "UTF-8");
			out.println("\n\n=============== " + projects[i] + " ===============");

			// Examples
			final Set<List<String>> exampleCalls = ExampleCoverage.getExampleAPICalls(projects[i], projFQNames[i]);

			// final String[] call = new String[] {
			// "twitter4j.conf.ConfigurationBuilder.<init>",
			// "twitter4j.conf.ConfigurationBuilder.setOAuthConsumerKey",
			// "twitter4j.conf.ConfigurationBuilder.setOAuthConsumerSecret",
			// "twitter4j.conf.ConfigurationBuilder.build" };
			// final List<String> callSeq = Arrays.asList(call);
			// double count = 0;
			// for (final List<String> seqEx : exampleCalls) {
			// if (ExampleCoverage.containsSeq(seqEx, callSeq))
			// count++;
			// }
			// System.out.println("Call prevalence: " + count);

			// Dataset calls
			final List<List<String>> datasetCalls = ExampleCoverage.getDatasetAPICalls(projects[i]);

			// ISM probability ranking
			final LinkedHashSet<List<String>> ISMCallsProb = ExampleCoverage.getISMCalls(projects[i],
					"ISM_seqs_prob.txt", 500);
			printTopCalls(getTopSequences(ISMCallsProb, topN), "ISM Top ", out);
			printTopCalls(getTopCoveredSequences(exampleCalls, ISMCallsProb, topN), "ISM Covered Top ", out);
			printTopCalls(getTopNotCoveredSequences(exampleCalls, ISMCallsProb, topN), "ISM Not Covered Top ", out);

			// MAPO
			final LinkedHashSet<List<String>> MAPOCalls = ExampleCoverage.getClusteredCalls(projects[i], "mapo", 500,
					datasetCalls);
			printTopCalls(getTopSequences(MAPOCalls, topN), "MAPO Top ", out);
			printTopCalls(getTopCoveredSequences(exampleCalls, MAPOCalls, topN), "MAPO Covered Top ", out);
			printTopCalls(getTopNotCoveredSequences(exampleCalls, MAPOCalls, topN), "MAPO Not Covered Top ", out);

			// UPMiner
			final LinkedHashSet<List<String>> UPMinerCalls = ExampleCoverage.getClusteredCalls(projects[i], "upminer",
					500, datasetCalls);
			printTopCalls(getTopSequences(UPMinerCalls, topN), "UPMiner Top ", out);
			printTopCalls(getTopCoveredSequences(exampleCalls, UPMinerCalls, topN), "UPMiner Covered Top ", out);
			printTopCalls(getTopNotCoveredSequences(exampleCalls, UPMinerCalls, topN), "UPMiner Not Covered Top ", out);

			// Print all example calls
			printTopCalls(exampleCalls, "All Example Calls: ", out);

			out.close();
		}

	}

	private static void printTopCalls(final Set<List<String>> topSequences, final String title, final PrintWriter out) {
		out.println("\n\n=+=+=+=+=+=+=+= " + title + topSequences.size() + " =+=+=+=+=+=+=+=");
		for (final List<String> seq : topSequences) {
			out.println(String.join(", ", seq) + ";");
		}
	}

	private static LinkedHashSet<List<String>> getTopSequences(final LinkedHashSet<List<String>> sequences,
			final int topN) {
		final LinkedHashSet<List<String>> topSequences = new LinkedHashSet<>();
		for (final List<String> seq : sequences) {
			topSequences.add(seq);
			if (topSequences.size() == topN)
				return topSequences;
		}
		System.out.println("WARNING: Not enough sequences:" + topSequences.size());
		return topSequences;
	}

	private static LinkedHashSet<List<String>> getTopCoveredSequences(final Set<List<String>> examples,
			final LinkedHashSet<List<String>> sequences, final int topN) {
		final LinkedHashSet<List<String>> coveredSequences = new LinkedHashSet<>();

		for (final List<String> seq : sequences) {
			for (final List<String> seqEx : examples) {
				if (ExampleCoverage.containsSeq(seqEx, seq)) {
					coveredSequences.add(seq);
					if (coveredSequences.size() == topN)
						return coveredSequences;
				}
			}
		}
		System.out.println("WARNING: Not enough covered sequences:" + coveredSequences.size());
		return coveredSequences;
	}

	private static LinkedHashSet<List<String>> getTopNotCoveredSequences(final Set<List<String>> examples,
			final LinkedHashSet<List<String>> sequences, final int topN) {
		final LinkedHashSet<List<String>> notCoveredSequences = new LinkedHashSet<>();

		for (final List<String> seq : sequences) {
			boolean covered = false;
			for (final List<String> seqEx : examples) {
				if (ExampleCoverage.containsSeq(seqEx, seq)) {
					covered = true;
					break;
				}
			}
			if (!covered)
				notCoveredSequences.add(seq);
			if (notCoveredSequences.size() == topN)
				return notCoveredSequences;
		}
		System.out.println("WARNING: Not enough not covered sequences:" + notCoveredSequences.size());
		return notCoveredSequences;
	}

}
