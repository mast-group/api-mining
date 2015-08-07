package apimining.upminer;

import java.util.ArrayList;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Parse Newick tree string into tree structure
 *
 * @see https://community.oracle.com/thread/1662917
 */
public class NewickTreeParser {

	public static Node parse(final String s) {
		final int x = s.lastIndexOf(':');
		return build(s, new Node(Double.parseDouble(s.substring(x + 1))), 0, x);
	}

	// this is the parsing code
	public static Node build(final String s, final Node parent, final int from, final int to) {
		if (s.charAt(from) != '(') {
			parent.setName(s.substring(from, to));
			return parent;
		}

		int b = 0; // bracket counter
		int colon = 0; // colon marker
		int x = from; // position marker

		for (int i = from; i < to; i++) {
			final char c = s.charAt(i);

			if (c == '(')
				b++;
			else if (c == ')')
				b--;
			else if (c == ':')
				colon = i;

			if (b == 0 || b == 1 && c == ',') {
				parent.addChild(build(s, new Node(Double.parseDouble(s.substring(colon + 1, i))), x + 1, colon));
				x = i;
			}
		}

		return parent;
	}
	// -------------------- end of parsing code ------------------

	private static int clusterCount;

	public static Multimap<Integer, String> getClusters(final String newick, final double threshold) {
		clusterCount = 0;
		final Multimap<Integer, String> clusters = HashMultimap.create();
		final Node root = NewickTreeParser.parse(newick);
		root.traverse(0, 0., clusters, threshold);
		return clusters;
	}

	public static class Node {
		private String name = null;
		private double value = 0;

		private final ArrayList<Node> children = new ArrayList<>();

		public Node(final double d) {
			value = d;
		}

		public void setName(final String s) {
			name = s;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(this, sb);
			return sb.toString();
		}

		public int numChildren() {
			return children.size();
		}

		public void addChild(final Node n) {
			children.add(n);
		}

		public Node getChild(final int i) {
			return children.get(i);
		}

		public void traverse(int clusterLabel, final double prevHeight, final Multimap<Integer, String> clusters,
				final double threshold) {
			final double height = prevHeight + value;
			if (height > threshold) {
				if (clusterLabel == 0) {
					clusterLabel = clusterCount;
					clusterCount++;
				} else if (this.children.isEmpty())
					clusters.put(clusterLabel, this.name);
			}
			for (final Node node : children)
				node.traverse(clusterLabel, height, clusters, threshold);
			return;
		}

		// toString method provided for testing purposes
		public static void toString(final Node n, final StringBuilder sb) {
			if (n.numChildren() == 0) {
				sb.append(n.name);
				sb.append(":");
				sb.append(n.value);
			} else {
				sb.append("(");
				toString(n.getChild(0), sb);
				for (int i = 1; i < n.numChildren(); i++) {
					sb.append(",");
					toString(n.getChild(i), sb);
				}
				sb.append("):");
				sb.append(n.value);
			}
		}
	}

	public static void main(final String[] args) {
		System.out.println(parse("a:1"));
		System.out.println(parse("(a:1):2"));
		System.out.println(parse("((a:1,b:2):3,c:4):5"));
		System.out.println(parse("(((One:0.2,Two:0.3):0.3,(Three:0.5,Four:0.3):0.2):0.3,Five:0.7):0.0"));
	}
}
