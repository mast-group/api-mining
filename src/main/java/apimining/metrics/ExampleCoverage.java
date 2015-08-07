package apimining.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Sets;

import apimining.java.APICallVisitor;
import apimining.java.ASTVisitors;

/** Check coverage of API call methods on provided examples */
public class ExampleCoverage {

	// private static final String exampleFolder =
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/library_dataset/java_libraries_examples/";
	// private static final String namespaceFolder =
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/library_dataset/namespaces/";
	// private static final String callsFolder =
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/srclibs/";

	private static final String exampleFolder = "/disk/data2/jfowkes/example_dataset/java_libraries_examples/";
	private static final String namespaceFolder = "/disk/data2/jfowkes/example_dataset/namespaces/";
	private static final String callsFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/";

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Settings
		// final String[] projects = new String[] { "netty", "twitter4j" };
		// final String[] projFQNames = new String[] { "io.netty", "twitter4j"
		// };
		final int topN = 30;
		final String[] projects = new String[] { "netty", "hadoop", "twitter4j", "mahout", "neo4j", "drools",
				"andengine", "spring-data-neo4j", "camel", "weld", "resteasy", "webobjects", "wicket",
				"restlet-framework-java", "cloud9", "hornetq", "spring-data-mongodb" };
		final String[] projFQNames = new String[] { "io.netty", "org.apache.hadoop", "twitter4j", "org.apache.mahout",
				"org.neo4j", "org.drools", "org.andengine", "org.springframework.data.neo4j", "org.apache.camel",
				"org.jboss.weld", "org.jboss.resteasy", "com.webobjects", "org.apache.wicket", "org.restlet",
				"edu.umd.cloud9", "org.hornetq", "org.springframework.data.mongodb" };

		for (int i = 0; i < projects.length; i++) {
			System.out.println("\n\n=============== " + projects[i] + " ===============");

			// Examples
			final Set<List<String>> exampleCalls = getExampleAPICalls(projects[i], projFQNames[i]);

			// ISM interestingness ranking
			final Set<List<String>> ISMCalls = getISMCalls(projects[i], "interesting_sequences.txt", topN);
			printPrecisionRecall(exampleCalls, ISMCalls, "" + topN, "ISM Int");

			// ISM probability ranking
			final Set<List<String>> ISMCallsProb = getISMCalls(projects[i], "interesting_sequences_prob.txt", topN);
			printPrecisionRecall(exampleCalls, ISMCallsProb, "" + topN, "ISM Prob");

			// MAPO
			final Set<List<String>> MAPOCalls = getClusteredCalls(projects[i], "mapo", topN);
			printPrecisionRecall(exampleCalls, MAPOCalls, "" + topN, "MAPO");

			// UPMiner
			final Set<List<String>> UPMinerCalls = getClusteredCalls(projects[i], "upminer", topN);
			printPrecisionRecall(exampleCalls, UPMinerCalls, "" + topN, "UP-Miner");

			// Dataset calls
			final Set<List<String>> datasetCalls = getDatasetAPICalls(projects[i]);
			printPrecisionRecall(exampleCalls, datasetCalls, "all", "Dataset");

		}
	}

	public static void printPrecisionRecall(final Set<List<String>> examples, final Set<List<String>> sequences,
			final String topN, final String algName) {

		final Set<List<String>> coveredExamples = new HashSet<>();
		for (final List<String> seqEx : examples) {
			for (final List<String> seq : sequences) {
				if (seqEx.containsAll(seq))
					coveredExamples.add(seqEx);
			}
		}
		final double exampleCoverage = (double) coveredExamples.size() / examples.size();

		final Set<List<String>> coveredSequences = new HashSet<>();
		for (final List<String> seqEx : examples) {
			for (final List<String> seq : sequences) {
				if (seqEx.containsAll(seq))
					coveredSequences.add(seq);
			}
		}
		final double sequenceCoverage = (double) coveredSequences.size() / sequences.size();

		final NumberFormat formatter = new DecimalFormat("#.####");
		System.out.println("\n---------- " + algName + " ----------");
		System.out.println("Example coverage @" + topN + ": " + formatter.format(exampleCoverage));
		System.out.println("Sequence coverage @" + topN + ": " + formatter.format(sequenceCoverage));

		final Set<String> relevantMethods = examples.stream().flatMap(List::stream).collect(Collectors.toSet());
		final Set<String> retrievedMethods = sequences.stream().flatMap(List::stream).collect(Collectors.toSet());
		final double mprecision = (double) Sets.intersection(relevantMethods, retrievedMethods).size()
				/ relevantMethods.size();
		final double mrecall = (double) Sets.intersection(relevantMethods, retrievedMethods).size()
				/ retrievedMethods.size();

		System.out.println("Method precision @" + topN + ": " + formatter.format(mprecision));
		System.out.println("Method recall @" + topN + ": " + formatter.format(mrecall));
	}

	private static Set<List<String>> getClusteredCalls(final String project, final String miner, final int topN)
			throws IOException {
		final Set<List<String>> topCalls = new HashSet<>();

		final List<File> files = (List<File>) FileUtils.listFiles(new File(callsFolder + project + "/" + miner), null,
				false);
		final int topNC = topN / files.size();
		if (topNC == 0)
			throw new RuntimeException(
					"ERROR: Less calls than clusters requested. Please increase to at least " + files.size());

		for (final File file : files) {
			final BufferedReader br = new BufferedReader(new FileReader(file));
			int count = 0;
			for (String line; (line = br.readLine()) != null;) {
				if (!line.trim().isEmpty() && !line.matches("^supp:.*")) {
					final List<String> call = Arrays.asList(line.split(" "));
					if (call.size() > 1) {
						topCalls.add(call);
						count++;
					}
					if (count == topNC)
						break;
				}
			}
			br.close();
			if (count < topNC)
				System.out.println("WARNING: Not enough calls in cluster " + file.getName() + ":" + count);
		}
		return topCalls;
	}

	private static Set<List<String>> getISMCalls(final String project, final String itemsetsFile, final int topN)
			throws IOException {
		final Set<List<String>> topCalls = new HashSet<>();
		final BufferedReader br = new BufferedReader(
				new FileReader(new File(callsFolder + project + "/" + itemsetsFile)));
		int count = 0;
		for (String line; (line = br.readLine()) != null;) {
			if (line.contains("[")) {
				final List<String> call = Arrays.asList(line.replaceAll("\\[|\\]|\'", "").split(", "));
				if (call.size() > 1) {
					topCalls.add(call);
					count++;
				}
				if (count == topN)
					break;
			}
		}
		br.close();
		if (count < topN)
			System.out.println("WARNING: Not enough interesting sequences:" + count);
		return topCalls;
	}

	@SuppressWarnings("unchecked")
	public static Set<List<String>> getExampleAPICalls(final String project, final String projFQName)
			throws IOException, ClassNotFoundException {
		Set<List<String>> allCalls;

		// Read serialized example calls if they exist
		final File exampleCallsFile = new File(callsFolder + project + "/" + project + "_examplecalls.ser");
		if (exampleCallsFile.exists()) {
			final ObjectInputStream reader = new ObjectInputStream(new FileInputStream(exampleCallsFile));
			allCalls = (Set<List<String>>) reader.readObject();
			reader.close();
			return allCalls;
		} else { // otherwise create

			// Get all java files in source folder
			allCalls = new HashSet<>();
			final List<File> files = (List<File>) FileUtils.listFiles(new File(exampleFolder + project),
					new String[] { "java" }, true);
			Collections.sort(files);

			int count = 0;
			for (final File file : files) {
				System.out.println("\nFile: " + file);

				// Ignore empty files
				if (file.length() == 0)
					continue;

				if (count % 50 == 0)
					System.out.println("At file " + count + " of " + files.size());
				count++;

				final APICallVisitor acv = new APICallVisitor(ASTVisitors.getAST(file), namespaceFolder);
				acv.process();
				final LinkedListMultimap<String, String> fqAPICalls = acv.getAPINames(projFQName);
				for (final String fqCaller : fqAPICalls.keySet())
					allCalls.add(new ArrayList<>(fqAPICalls.get(fqCaller)));
			}

			// Filter singletons
			allCalls = allCalls.stream().filter(l -> l.size() > 1).collect(Collectors.toSet());

			// Serialize calls
			final ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(exampleCallsFile));
			writer.writeObject(allCalls);
			writer.close();

			return allCalls;
		}
	}

	private static Set<List<String>> getDatasetAPICalls(final String project) throws IOException {
		final Set<List<String>> calls = new HashSet<>();
		final BufferedReader br = new BufferedReader(
				new FileReader(new File(callsFolder + "calls/" + project + ".arff")));
		boolean found = false;
		for (String line; (line = br.readLine()) != null;) {
			if (line.startsWith("@data"))
				found = true;
			else if (found) {
				final List<String> call = Arrays.asList(line.split(",")[1].replaceAll("\'", "").split(" "));
				if (call.size() > 1)
					calls.add(call);
			}
		}
		br.close();
		return calls;
	}

}
