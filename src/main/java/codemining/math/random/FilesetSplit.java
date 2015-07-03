/**
 * 
 */
package codemining.math.random;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Perform split on some set of files and output them into a different directory
 * (e.g. train/validation/test split)
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public final class FilesetSplit {

	/**
	 * A file weighting function that assigns equal weight to each file
	 * irrespectively from their contents.
	 */
	public static final Function<File, Double> UNIFORM_FILE_WEIGHT = new Function<File, Double>() {

		@Override
		public Double apply(final File f) {
			return 1.;
		}
	};

	private static final Logger LOGGER = Logger.getLogger(FilesetSplit.class
			.getName());

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length < 7) {
			System.err
					.println("Usage fromDirectory toDirectory fileSuffix <<segmentName_i> <weight_i> ...>");
			System.exit(-1);
		}

		final File fromDirectory = new File(args[0]);
		final File toDirectory = new File(args[1]);

		final IOFileFilter fileFilter = FileFilterUtils
				.suffixFileFilter(args[2]);

		final Map<String, Double> segments = Maps.newHashMap();

		for (int i = 3; i < args.length; i += 2) {
			segments.put(args[i], Double.valueOf(args[i + 1]));
		}

		LOGGER.info("Splitting files in segments " + segments);
		splitFiles(fromDirectory, toDirectory, segments, fileFilter,
				UNIFORM_FILE_WEIGHT);
	}

	/**
	 * 
	 * @param fromDirectory
	 * @param toDirectory
	 * @param isIncluded
	 * @param segments
	 *            the partition percentages to be used in the split, along with
	 *            the name of the folders to be created.
	 * @param weightingFunction
	 *            the function that returns the weight of each file in the split
	 */
	public static void splitFiles(final File fromDirectory,
			final File toDirectory, final Map<String, Double> segments,
			final IOFileFilter isIncluded,
			final Function<File, Double> weightingFunction) {
		final Collection<File> fromFiles = FileUtils.listFiles(fromDirectory,
				isIncluded, DirectoryFileFilter.DIRECTORY);

		final Map<File, Double> fileWeights = Maps.newHashMap();
		for (final File f : fromFiles) {
			fileWeights.put(f, weightingFunction.apply(f));
		}

		final Multimap<String, File> fileSplit = SampleUtils.randomPartition(
				fileWeights, segments);
		// Start copying
		final String pathSeparator = System.getProperty("file.separator");
		for (final Entry<String, File> file : fileSplit.entries()) {
			final File targetFilename = new File(toDirectory.getAbsolutePath()
					+ pathSeparator + file.getKey() + pathSeparator
					+ file.getValue().getName());
			try {
				FileUtils.copyFile(file.getValue(), targetFilename);
			} catch (final IOException e) {
				LOGGER.severe("Failed to copy " + file.getValue() + " to "
						+ targetFilename);
			}
		}

	}

	private FilesetSplit() {
	}

}
