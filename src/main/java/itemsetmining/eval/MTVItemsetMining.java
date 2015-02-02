package itemsetmining.eval;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;

import ca.pfv.spmf.tools.other_dataset_tools.FixTransactionDatabaseTool;

import com.google.common.collect.Maps;

public class MTVItemsetMining {

	private static final String TMPDB = "/tmp/fixed-dataset.dat";
	private static final FixTransactionDatabaseTool dbTool = new FixTransactionDatabaseTool();

	public static void main(final String[] args) throws IOException {

		// MTV Parameters
		final String[] datasets = new String[] { "plants", "mammals",
				"abstracts", "uganda" };
		final double[] minSupps = new double[] { 0.05750265949, 0.07490636704,
				0.01164144353, 0.001 }; // relative support
		final int noItemsets = 1000;

		for (int i = 0; i < datasets.length; i++) {

			final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/"
					+ datasets[i] + ".dat";
			final String saveFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/MTV/"
					+ datasets[i] + ".txt";

			mineItemsets(new File(dbPath), minSupps[i], noItemsets, new File(
					saveFile));

		}

	}

	public static LinkedHashMap<Itemset, Double> mineItemsets(
			final File dbFile, final double minSup, final int noItemsets,
			final File saveFile) throws IOException {

		// Remove transaction duplicates and sort items ascending
		dbTool.convert(dbFile.getAbsolutePath(), TMPDB);

		// Set MTV settings
		final String cmd[] = new String[6];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mtv/mtv.sh";
		cmd[1] = "-f " + TMPDB;
		cmd[2] = "-s " + minSup;
		cmd[3] = "-k " + noItemsets;
		cmd[4] = "-o " + saveFile;
		cmd[5] = "-g 10"; // Max items per group (for efficiency)
		// cmd[6] = "-q" // Quiet mode
		runScript(cmd);

		return readMTVItemsets(saveFile);
	}

	/** Read in MTV itemsets */
	public static LinkedHashMap<Itemset, Double> readMTVItemsets(
			final File output) throws IOException {
		final LinkedHashMap<Itemset, Double> itemsets = Maps.newLinkedHashMap();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		for (final String line : lines) {
			if (!line.trim().isEmpty() && line.charAt(0) != '#') {
				final String[] splitLine = line.split(" ");
				final Itemset itemset = new Itemset();
				for (int i = 1; i < splitLine.length; i++)
					itemset.add(Integer.parseInt(splitLine[i].trim()));
				final double prob = Double.parseDouble(splitLine[0].trim());
				itemsets.put(itemset, prob);
			}
		}

		return itemsets;
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
