package apimining.mapo;

import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import apimining.upminer.NewickTreeParser;
import weka.clusterers.HierarchicalClusterer;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.neighboursearch.PerformanceStats;
import weka.gui.hierarchyvisualizer.HierarchyVisualizer;

public class APICallClustererMAPO {

	public static void main(final String[] args) throws Exception {

		final String dataset = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/srclibs/calls/netty.arff";
		final Multimap<Integer, String> assignments = clusterAPICallSeqs(dataset, 10);

		// Print assignments
		for (final int cluster : assignments.keySet()) {
			System.out.println("===== Cluster #" + cluster);
			for (final String seq : assignments.get(cluster))
				System.out.println("      " + seq);
		}
	}

	/**
	 * Cluster API call sequences as described in MAPO
	 *
	 * @return Multimap of cluster IDs to API call sequences
	 */
	public static Multimap<Integer, String> clusterAPICallSeqs(final String arffFile, final double threshold)
			throws Exception {

		// Clusterer settings
		final HierarchicalClusterer clusterer = new HierarchicalClusterer();
		clusterer.setOptions(new String[] { "-L", "COMPLETE" }); // link type
		clusterer.setDebug(true);
		clusterer.setNumClusters(1);
		clusterer.setDistanceFunction(MAPOSimilarity);
		clusterer.setDistanceIsBranchLength(false);

		// Read in API call seqs
		final DataSource source = new DataSource(arffFile);
		final Instances data = source.getDataSet();

		// Cluster API call seqs
		clusterer.buildClusterer(data);

		// Assign seqs to clusters based on dendrogram
		final String newick = clusterer.graph().replace("Newick:", "") + ":0";
		if (newick.equals("(no,clusters):0")) // Handle no clusters
			return HashMultimap.create();
		final Multimap<Integer, String> clusters = NewickTreeParser.getClusters(newick, threshold);
		System.out.println("No. clusters: " + clusters.keySet().size());
		final Multimap<Integer, String> assignments = HashMultimap.create();
		for (int i = 0; i < data.numInstances(); i++) {
			for (final int id : clusters.keySet()) {
				if (clusters.get(id).contains(data.instance(i).stringValue(0)))
					assignments.put(id, data.instance(i).stringValue(1));
			}
		}

		// showDendrogram(clusterer);

		return assignments;
	}

	public static void showDendrogram(final HierarchicalClusterer clusterer) throws Exception {
		final JFrame mainFrame = new JFrame("Dendrogram");
		mainFrame.setSize(1024, 768);
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final Container content = mainFrame.getContentPane();
		content.setLayout(new GridLayout(1, 1));

		final HierarchyVisualizer visualizer = new HierarchyVisualizer(clusterer.graph());
		content.add(visualizer);

		mainFrame.setVisible(true);
	}

	private static final DistanceFunction MAPOSimilarity = new DistanceFunction() {

		@SuppressWarnings("rawtypes")
		@Override
		public Enumeration listOptions() {
			// we have no distance options
			return null;
		}

		@Override
		public void setOptions(final String[] options) throws Exception {
			// we have no distance options
		}

		@Override
		public String[] getOptions() {
			// we have no distance options
			return null;
		}

		@Override
		public void setInstances(final Instances insts) {
			// we don't use internal instances
		}

		@Override
		public Instances getInstances() {
			// we don't use internal instances
			return null;
		}

		@Override
		public void setAttributeIndices(final String value) {
			// we use all attributes
		}

		@Override
		public String getAttributeIndices() {
			// we use all attributes
			return null;
		}

		@Override
		public void setInvertSelection(final boolean value) {
			// we use all attributes
		}

		@Override
		public boolean getInvertSelection() {
			// we use all attributes
			return false;
		}

		@Override
		public double distance(final Instance first, final Instance second) {
			return distance(first, second, null);
		}

		@Override
		public double distance(final Instance first, final Instance second, final PerformanceStats stats) {
			return distance(first, second, Double.POSITIVE_INFINITY, stats);
		}

		@Override
		public double distance(final Instance first, final Instance second, final double cutOffValue) {
			return distance(first, second, cutOffValue, null);
		}

		@Override
		public double distance(final Instance first, final Instance second, final double cutOffValue,
				final PerformanceStats stats) {

			final String[] fqCaller1 = first.stringValue(0).split("\\.");
			final String[] fqCaller2 = second.stringValue(0).split("\\.");
			final double methodDist = getNameDistance(fqCaller1[fqCaller1.length - 1], fqCaller2[fqCaller2.length - 1]);
			final double classDist = getNameDistance(fqCaller1[fqCaller1.length - 2], fqCaller2[fqCaller2.length - 2]);

			final String[] fqCalls1 = first.stringValue(1).split(" ");
			final String[] fqCalls2 = second.stringValue(1).split(" ");
			final double seqDist = getSeqDistance(new HashSet<>(Arrays.asList(fqCalls1)),
					new HashSet<>(Arrays.asList(fqCalls2)));

			if (stats != null)
				stats.incrCoordCount();

			return (methodDist + classDist + seqDist) / 3.;
		}

		private double getNameDistance(final String string1, final String string2) {
			final List<String> words1 = new ArrayList<>();
			final List<String> words2 = new ArrayList<>();
			putTokenParts(words1, string1);
			putTokenParts(words2, string2);
			double distance = 0;
			for (final String word1 : words1) {
				for (final String word2 : words2) {
					distance += editDistance(word1, word2);
				}
			}
			return distance / (words1.size() * words2.size());
		}

		private double getSeqDistance(final Set<String> set1, final Set<String> set2) {
			final double sizeIntersection = Sets.intersection(set1, set2).size();
			final double sizeUnion = Sets.union(set1, set2).size();
			return sizeIntersection / sizeUnion;
		}

		@Override
		public void postProcessDistances(final double[] distances) {
			// no need to post process distances
		}

		@Override
		public void update(final Instance ins) {
			// we use all attributes
		}

	};

	/**
	 * Calculate the Levenshtein distance between two strings using the
	 * Wagner-Fischer algorithm
	 *
	 * @see http://en.wikipedia.org/wiki/Levenshtein_distance
	 */
	private static int editDistance(final String s, final String t) {
		final int m = s.length();
		final int n = t.length();

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
				if (s.charAt(i - 1) == t.charAt(j - 1)) {
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

	/**
	 * Split given token and convert to lowercase (splits CamelCase and
	 * _under_score)
	 *
	 * @see {@link codemining.java.codeutils.IdentifierTokenRetriever#putTokenParts}
	 * @author Jaroslav Fowkes
	 */
	public static void putTokenParts(final List<String> identifierList, final String identifier) {
		for (final String token : identifier.split("((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|_")) {
			if (!token.equals(""))
				identifierList.add(token.toLowerCase());
		}
	}

}
