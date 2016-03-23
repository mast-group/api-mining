package apimining.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.math.DoubleMath;

import apimining.java.APICallVisitor;
import apimining.java.ASTVisitors;

/** Check coverage of API call methods on provided examples */
public class ExampleCoverage {

	// static final String baseFolder =
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/";
	// private static final String exampleFolder =
	// "/disk/data2/jfowkes/example_dataset/java_libraries_examples/";
	static final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/train/";
	private static final String exampleFolder = "/disk/data2/jfowkes/example_dataset/test_train_split/test/";
	private static final String namespaceFolder = "/disk/data2/jfowkes/example_dataset/namespaces/";

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		final String[] projects = new String[] { "netty", "hadoop", "twitter4j", "mahout", "neo4j", "drools",
				"andengine", "spring-data-neo4j", "camel", "weld", "resteasy", "webobjects", "wicket",
				"restlet-framework-java", "cloud9", "hornetq", "spring-data-mongodb" };
		final String[] projFQNames = new String[] { "io.netty", "org.apache.hadoop", "twitter4j", "org.apache.mahout",
				"org.neo4j", "org.drools", "org.andengine", "org.springframework.data.neo4j", "org.apache.camel",
				"org.jboss.weld", "org.jboss.resteasy", "com.webobjects", "org.apache.wicket", "org.restlet",
				"edu.umd.cloud9", "org.hornetq", "org.springframework.data.mongodb" };

		// Stats containers
		final Stats statsISM = new Stats("ISM-Int");
		final Stats statsISMProb = new Stats("ISM-Prob");
		final Stats statsMAPO = new Stats("MAPO");
		final Stats statsUPMiner = new Stats("UPMiner");
		final Stats statsDataset = new Stats("Dataset");

		for (int i = 0; i < projects.length; i++) {
			// if (projects[i].matches("hadoop"))
			// continue;

			System.out.println("\n\n=============== " + projects[i] + " ===============");

			// Examples
			final Set<List<String>> exampleCalls = getExampleAPICalls(projects[i], projFQNames[i]);

			// Dataset calls
			final List<List<String>> datasetCalls = getDatasetAPICalls(projects[i]);
			calculateDatasetStats(exampleCalls, datasetCalls, projects[i], Integer.MAX_VALUE, statsDataset);
			// statsDataset.printAverage(Integer.MAX_VALUE);

			for (final int topN : range(10, 501, 10)) {
				// System.out.println("\n\n=+=+=+=+=+=+=+= Top " + topN + "
				// =+=+=+=+=+=+=+=");
				System.out.printf(topN + " ");

				// ISM interestingness ranking
				final Set<List<String>> ISMCalls = getISMCalls(projects[i], "ISM_seqs_int.txt", topN);
				calculatePrecisionRecall(exampleCalls, ISMCalls, projects[i], topN, statsISM);
				// statsISM.printProjectStats(projects[i], topN);

				// ISM probability ranking
				final Set<List<String>> ISMCallsProb = getISMCalls(projects[i], "ISM_seqs_prob.txt", topN);
				calculatePrecisionRecall(exampleCalls, ISMCallsProb, projects[i], topN, statsISMProb);
				// statsISMProb.printProjectStats(projects[i], topN);

				// MAPO
				final Set<List<String>> MAPOCalls = getClusteredCalls(projects[i], "mapo", topN, datasetCalls);
				calculatePrecisionRecall(exampleCalls, MAPOCalls, projects[i], topN, statsMAPO);
				// statsMAPO.printProjectStats(projects[i], topN);

				// UPMiner
				final Set<List<String>> UPMinerCalls = getClusteredCalls(projects[i], "upminer", topN, datasetCalls);
				calculatePrecisionRecall(exampleCalls, UPMinerCalls, projects[i], topN, statsUPMiner);
				// statsUPMiner.printProjectStats(projects[i], topN);

				// Print average
				// statsISM.printAverage(topN);
				// statsISMProb.printAverage(topN);
				// statsMAPO.printAverage(topN);
				// statsUPMiner.printAverage(topN);
			}

		}

		// Save to file
		statsISM.saveToFile();
		statsISMProb.saveToFile();
		statsMAPO.saveToFile();
		statsUPMiner.saveToFile();
		statsDataset.saveToFile();
	}

	public static void calculatePrecisionRecall(final Set<List<String>> examples, final Set<List<String>> sequences,
			final String project, final int topN, final Stats stats) {

		final Set<List<String>> coveredSequences = new HashSet<>();
		for (final List<String> seqEx : examples) {
			for (final List<String> seq : sequences) {
				if (containsSeq(seqEx, seq))
					coveredSequences.add(seq);
			}
		}
		final double sequencePrecision = (double) coveredSequences.size() / sequences.size();
		stats.sequencePrecision.put(project, topN, sequencePrecision);

		final Set<List<String>> coveredExamples = new HashSet<>();
		for (final List<String> seqEx : examples) {
			for (final List<String> seq : sequences) {
				if (containsSeq(seqEx, seq))
					coveredExamples.add(seqEx);
			}
		}
		final double sequenceRecall = (double) coveredExamples.size() / examples.size();
		stats.sequenceRecall.put(project, topN, sequenceRecall);

		final Set<String> relevantMethods = examples.stream().flatMap(List::stream).collect(Collectors.toSet());
		final Set<String> retrievedMethods = sequences.stream().flatMap(List::stream).collect(Collectors.toSet());
		final double mprecision = (double) Sets.intersection(relevantMethods, retrievedMethods).size()
				/ retrievedMethods.size();
		final double mrecall = (double) Sets.intersection(relevantMethods, retrievedMethods).size()
				/ relevantMethods.size();
		stats.mPrecision.put(project, topN, mprecision);
		stats.mRecall.put(project, topN, mrecall);

		final double redundancy = calculateRedundancy(sequences);
		final double spuriousness = calculateSpuriousness(sequences);
		stats.redundancy.put(project, topN, redundancy);
		stats.spuriousness.put(project, topN, spuriousness);

	}

	/** Check if first sequence contains second sequence (allowing gaps) */
	static boolean containsSeq(final List<String> seq1, final List<String> seq2) {
		int pos = 0;
		boolean containsItem;
		for (final String item : seq2) {
			containsItem = false;
			for (int i = pos; i < seq1.size(); i++) {
				if (seq1.get(i).equals(item)) {
					pos = i + 1;
					containsItem = true;
					break;
				}
			}
			if (!containsItem)
				return false;
		}
		return true;
	}

	/** Calculate upper bound on precision */
	public static void calculateDatasetStats(final Set<List<String>> examples, final List<List<String>> dataset,
			final String project, final int topN, final Stats stats) {

		final Set<List<String>> coveredSequences = new HashSet<>();
		for (final List<String> seqEx : examples) {
			for (final List<String> seq : dataset) {
				if (containsSeq(seqEx, seq))
					coveredSequences.add(seq);
			}
		}
		final double sequencePrecision = (double) coveredSequences.size() / dataset.size();
		stats.sequencePrecision.put(project, topN, sequencePrecision);

		final Set<List<String>> coveredExamples = new HashSet<>();
		for (final List<String> seqEx : examples) {
			for (final List<String> seq : dataset) {
				if (containsSeq(seqEx, seq))
					coveredExamples.add(seqEx);
			}
		}
		final double sequenceRecall = (double) coveredExamples.size() / examples.size();
		stats.sequenceRecall.put(project, topN, sequenceRecall);

		final Set<String> exampleMethods = examples.stream().flatMap(List::stream).collect(Collectors.toSet());
		final Set<String> datasetMethods = dataset.stream().flatMap(List::stream).collect(Collectors.toSet());
		final double mRecall = (double) Sets.intersection(exampleMethods, datasetMethods).size()
				/ exampleMethods.size();
		stats.mRecall.put(project, topN, mRecall);

		double avgMinDiff = 0;
		for (final List<String> set1 : examples) {

			int minDiff = Integer.MAX_VALUE;
			for (final List<String> set2 : dataset) {
				if (!set1.equals(set2)) {
					final int diff = editDistance(set1, set2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= examples.size();
		stats.redundancy.put(project, topN, avgMinDiff);

	}

	private static double calculateRedundancy(final Set<List<String>> topItemsets) {

		double avgMinDiff = 0;
		for (final List<String> set1 : topItemsets) {

			int minDiff = Integer.MAX_VALUE;
			for (final List<String> set2 : topItemsets) {
				if (!set1.equals(set2)) {
					final int diff = editDistance(set1, set2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= topItemsets.size();

		return avgMinDiff;
	}

	/**
	 * Calculate the Levenshtein distance between two sequences using the
	 * Wagner-Fischer algorithm
	 *
	 * @see http://en.wikipedia.org/wiki/Levenshtein_distance
	 */
	private static int editDistance(final List<String> s, final List<String> t) {
		final int m = s.size();
		final int n = t.size();

		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t;
		final int[][] d = new int[m + 1][n + 1];

		// the distance of any first string to an empty second string
		for (int i = 1; i <= m; i++)
			d[i][0] = i;

		// the distance of any second string to an empty first string
		for (int j = 1; j <= n; j++)
			d[0][j] = j;

		for (int j = 1; j <= n; j++) {
			for (int i = 1; i <= m; i++) {
				if (s.get(i - 1) == t.get(j - 1)) {
					d[i][j] = d[i - 1][j - 1]; // no operation required
				} else {
					d[i][j] = Math.min(d[i - 1][j] + 1, // a deletion
							Math.min(d[i][j - 1] + 1, // an insertion
									d[i - 1][j - 1] + 1)); // a substitution
				}
			}
		}

		return d[m][n];
	}

	private static double calculateSpuriousness(final Set<List<String>> topItemsets) {

		double avgSubseq = 0;
		for (final List<String> set1 : topItemsets) {
			for (final List<String> set2 : topItemsets) {
				if (!set1.equals(set2))
					avgSubseq += isSubseq(set1, set2);
			}
		}
		avgSubseq /= topItemsets.size();

		return avgSubseq;
	}

	private static int isSubseq(final List<String> seq1, final List<String> seq2) {
		if (containsSeq(seq2, seq1))
			return 1;
		return 0;
	}

	static LinkedHashSet<List<String>> getClusteredCalls(final String project, final String miner, final int topN,
			final List<List<String>> datasetCalls) throws IOException {
		final List<File> files = (List<File>) FileUtils.listFiles(new File(baseFolder + project + "/" + miner), null,
				false);
		final HashMap<BufferedReader, Boolean> brs = new LinkedHashMap<>();
		for (final File file : files)
			brs.put(new BufferedReader(new FileReader(file)), true);

		final Set<List<String>> topCalls = new HashSet<>();
		while (true) {
			for (final BufferedReader br : brs.keySet()) {
				while (brs.get(br)) {
					final String line = br.readLine();
					if (line == null) {
						brs.put(br, false);
						break;
					}
					if (!line.trim().isEmpty() && !line.matches("^supp:.*")) {
						final List<String> call = Arrays.asList(line.split(" "));
						if (call.size() > 1) {
							topCalls.add(call);
							break;
						}
					}
				}
			}
			if ((topCalls.size() > topN) || !brs.values().contains(true))
				break;
		}

		for (final BufferedReader br : brs.keySet())
			br.close();

		if (topCalls.size() < topN)
			System.out.println("WARNING: Not enough " + miner + " calls:" + topCalls.size());
		return orderBySupport(topCalls, datasetCalls, topN);
	}

	static LinkedHashSet<List<String>> getISMCalls(final String project, final String itemsetsFile, final int topN)
			throws IOException {
		final LinkedHashSet<List<String>> topCalls = new LinkedHashSet<>();
		final BufferedReader br = new BufferedReader(
				new FileReader(new File(baseFolder + project + "/" + itemsetsFile)));
		for (String line; (line = br.readLine()) != null;) {
			if (line.contains("[")) {
				final List<String> call = Arrays.asList(line.replaceAll("\\[|\\]|\'", "").split(", "));
				if (call.size() > 1) {
					topCalls.add(call);
					if (topCalls.size() == topN)
						break;
				}
			}
		}
		br.close();
		if (topCalls.size() < topN)
			System.out.println("WARNING: Not enough interesting sequences:" + topCalls.size());
		return topCalls;
	}

	@SuppressWarnings("unchecked")
	public static Set<List<String>> getExampleAPICalls(final String project, final String projFQName)
			throws IOException, ClassNotFoundException {
		Set<List<String>> allCalls;

		// Read serialized example calls if they exist
		final File exampleCallsFile = new File(baseFolder + project + "/" + project + "_examplecalls.ser");
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
				for (final String fqCaller : fqAPICalls.keySet()) {
					final List<String> call = new ArrayList<>(fqAPICalls.get(fqCaller));
					if (call.size() > 1)
						allCalls.add(call);
				}
			}

			// Serialize calls
			final ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(exampleCallsFile));
			writer.writeObject(allCalls);
			writer.close();

			return allCalls;
		}
	}

	private static LinkedHashSet<List<String>> orderBySupport(final Set<List<String>> calls,
			final List<List<String>> datasetCalls, final int topN) {
		final Ordering<List<String>> comp = Ordering.natural().reverse()
				.onResultOf(new Function<List<String>, Double>() {
					@Override
					public Double apply(final List<String> call) {
						double supp = 0.;
						for (final List<String> datasetCall : datasetCalls) {
							if (containsSeq(datasetCall, call))
								supp++;
						}
						return supp;
					}
				}).compound(Ordering.usingToString());
		final ImmutableSortedSet<List<String>> orderedSet = ImmutableSortedSet.copyOf(comp, calls);

		final LinkedHashSet<List<String>> topCalls = new LinkedHashSet<>();
		for (final List<String> call : orderedSet) {
			topCalls.add(call);
			if (topCalls.size() == topN)
				break;
		}

		return topCalls;
	}

	static List<List<String>> getDatasetAPICalls(final String project) throws IOException {
		final List<List<String>> calls = new ArrayList<>();
		final BufferedReader br = new BufferedReader(
				new FileReader(new File(baseFolder + "calls/" + project + ".arff")));
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

	private static class Stats {
		String algName;

		Table<String, Integer, Double> sequencePrecision = TreeBasedTable.create();
		Table<String, Integer, Double> sequenceRecall = TreeBasedTable.create();
		Table<String, Integer, Double> mPrecision = TreeBasedTable.create();
		Table<String, Integer, Double> mRecall = TreeBasedTable.create();
		Table<String, Integer, Double> redundancy = TreeBasedTable.create();
		Table<String, Integer, Double> spuriousness = TreeBasedTable.create();

		final NumberFormat formatter = new DecimalFormat("#.####");

		Stats(final String algName) {
			this.algName = algName;
		}

		void printProjectStats(final String proj, final int topN) {
			System.out.println("\n---------- " + algName + " ----------");
			System.out.println(
					"Sequence precision @" + topN + ": " + formatter.format(sequencePrecision.get(proj, topN)));
			System.out.println("Sequence recall @" + topN + ": " + formatter.format(sequenceRecall.get(proj, topN)));
			System.out.println("Method precision @" + topN + ": " + formatter.format(mPrecision.get(proj, topN)));
			System.out.println("Method recall @" + topN + ": " + formatter.format(mRecall.get(proj, topN)));
			System.out.println("Redundancy @" + topN + ": " + formatter.format(redundancy.get(proj, topN)));
			System.out.println("Spuriousness @" + topN + ": " + formatter.format(spuriousness.get(proj, topN)));
		}

		void printAverage(final int topN) {
			System.out.println("\n========== Average " + algName + " ==========");
			System.out.println("Sequence precision @" + topN + ": "
					+ formatter.format(nanmean(sequencePrecision.column(topN).values())));
			System.out.println("Sequence recall @" + topN + ": "
					+ formatter.format(nanmean(sequenceRecall.column(topN).values())));
			System.out.println(
					"Method precision @" + topN + ": " + formatter.format(nanmean(mPrecision.column(topN).values())));
			System.out.println(
					"Method recall @" + topN + ": " + formatter.format(nanmean(mRecall.column(topN).values())));
			System.out.println(
					"Redundancy @" + topN + ": " + formatter.format(nanmean(redundancy.column(topN).values())));
			System.out.println(
					"Spuriousness @" + topN + ": " + formatter.format(nanmean(spuriousness.column(topN).values())));
		}

		private static double nanmean(final Collection<Double> values) {
			final List<Double> finiteValues = values.stream().filter(Double::isFinite).collect(Collectors.toList());
			if (finiteValues.isEmpty())
				return Double.NaN;
			return DoubleMath.mean(finiteValues);
		}

		public void saveToFile() throws IOException {
			final File saveFile = new File(baseFolder + algName + ".pr");
			final PrintWriter out = new PrintWriter(saveFile, "UTF-8");
			for (final Integer topN : mRecall.columnKeySet()) {
				final double avgSeqPrecision = nanmean(sequencePrecision.column(topN).values());
				final double avgSeqRecall = nanmean(sequenceRecall.column(topN).values());
				final double avgMprecision = nanmean(mPrecision.column(topN).values());
				final double avgMrecall = nanmean(mRecall.column(topN).values());
				final double avgRedundnacy = nanmean(redundancy.column(topN).values());
				final double avgSpuriousness = nanmean(spuriousness.column(topN).values());
				out.println(avgSeqPrecision + "," + avgSeqRecall + "," + avgMprecision + "," + avgMrecall + ","
						+ avgRedundnacy + "," + avgSpuriousness);
			}
			out.close();
		}

	}

	/**
	 * An implementation of Python's range function
	 *
	 * @author Jaroslav Fowkes
	 */
	public static ArrayList<Integer> range(final int start, final int stop, final int increment) {
		final ArrayList<Integer> result = new ArrayList<>();
		for (int i = 0; i < stop - start; i += increment)
			result.add(start + i);
		return result;
	}

}
