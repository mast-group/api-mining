package apimining.upminer;

import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import weka.clusterers.HierarchicalClusterer;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.neighboursearch.PerformanceStats;
import weka.gui.hierarchyvisualizer.HierarchyVisualizer;

public class APICallClusterer {

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
	public static Multimap<Integer, String> clusterAPICallSeqs(final String arffFile, final int noClusters)
			throws Exception {

		// Clusterer settings
		final HierarchicalClusterer clusterer = new HierarchicalClusterer();
		clusterer.setOptions(new String[] { "-L", "COMPLETE" }); // link type
		clusterer.setDebug(false);
		clusterer.setNumClusters(noClusters);
		clusterer.setDistanceFunction(SeqSimilarity);
		clusterer.setDistanceIsBranchLength(false);

		// Read in API call seqs
		final DataSource source = new DataSource(arffFile);
		final Instances data = source.getDataSet();

		// Cluster API call seqs
		clusterer.buildClusterer(data);

		// Assign seqs to clusters
		final Multimap<Integer, String> assignments = HashMultimap.create();
		for (int i = 0; i < data.numInstances(); i++) {
			final int id = clusterer.clusterInstance(data.instance(i));
			assignments.put(id, data.instance(i).stringValue(1));
		}

		// showDendrogram(clusterer);

		return assignments;
	}

	public static void showDendrogram(final HierarchicalClusterer clusterer) throws Exception {
		final JFrame mainFrame = new JFrame("Dendrogram");
		mainFrame.setSize(600, 400);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final Container content = mainFrame.getContentPane();
		content.setLayout(new GridLayout(1, 1));

		final HierarchyVisualizer visualizer = new HierarchyVisualizer(clusterer.graph());
		content.add(visualizer);

		mainFrame.setVisible(true);
	}

	private static final DistanceFunction SeqSimilarity = new DistanceFunction() {

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

			final String[] fqCalls1 = first.stringValue(1).split(" ");
			final String[] fqCalls2 = second.stringValue(1).split(" ");
			final double seqDist = getSeqDistance(fqCalls1, fqCalls2);

			if (stats != null)
				stats.incrCoordCount();

			return seqDist;
		}

		private double getSeqDistance(final String[] set1, final String[] set2) {

			final Set<List<String>> nSet1 = getNgramSet(set1);
			final Set<List<String>> nSet2 = getNgramSet(set2);
			final Set<List<String>> intSet = Sets.intersection(nSet1, nSet2);
			final Set<List<String>> uniSet = Sets.union(nSet1, nSet2);

			double sizeIntersection = 0;
			for (final List<String> seq : intSet)
				sizeIntersection += seq.size();
			double sizeUnion = 0;
			for (final List<String> seq : uniSet)
				sizeUnion += seq.size();

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

	private static Set<List<String>> getNgramSet(final String[] seq) {
		final Set<List<String>> ngramSet = new HashSet<>();

		for (int len = 1; len <= seq.length; len++) {
			for (int start = 0; start <= seq.length - len; start++) {
				final List<String> subseq = new ArrayList<>();
				for (int i = start; i < start + len; i++)
					subseq.add(seq[i]);
				ngramSet.add(subseq);

			}
		}

		return ngramSet;
	}

}
