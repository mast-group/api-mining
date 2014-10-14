package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import com.google.common.collect.Maps;

public class MTVItemsetMining {

	public static void main(final String[] args) throws IOException {

		// MTV Parameters
		final String dataset = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/abstracts.dat";
		final double minSupp = 0.01164144353; // relative support
		final int noItemsets = 10;

		mineItemsets(new File(dataset), minSupp, noItemsets);

	}

	public static HashMap<Itemset, Double> mineItemsets(final File dbfile,
			final double minsup, final int noItemsets) throws IOException {

		final HashMap<Itemset, Double> minedItemsets = Maps.newHashMap();

		// Set MTV settings
		final String cmd[] = new String[6];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mtv/mtv.sh";
		cmd[1] = "-f " + dbfile;
		cmd[2] = "-s " + minsup;
		cmd[3] = "-k " + noItemsets;
		cmd[4] = "-o /tmp/mtv-log.txt";
		cmd[5] = "-q";
		runScript(cmd);

		System.out.println("SUMMARY:");
		final LineIterator it = FileUtils.lineIterator(new File(
				"/tmp/mtv-log.txt"), "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// Skip comments
			if (line.charAt(0) == '#')
				continue;
			System.out.println(line);

			// Read prob
			final String[] splitLine = line.split(" ", 2);
			final double prob = Double.parseDouble(splitLine[0]);

			// Read itemset
			final Itemset set = new Itemset();
			final String[] items = splitLine[1].split(" ");
			for (int i = 0; i < items.length; i++)
				set.add(Integer.parseInt(items[i]));
			minedItemsets.put(set, prob);

		}
		LineIterator.closeQuietly(it);
		System.out.println();

		return minedItemsets;
	}

	/** Run shell script with command line arguments */
	public static void runScript(final String cmd[]) {

		try {
			final ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			final Process process = pb.start();
			process.waitFor();
			process.destroy();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}
