/**
 *
 */
package codemining.java.codeutils.binding.tui;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import codemining.java.codeutils.binding.AbstractJavaNameBindingsExtractor;

import com.google.common.collect.Sets;
import com.google.gson.JsonIOException;

/**
 * Extract bindings for a given type, including one type of feature per time.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaBindingsPerFeatureTypeToJson {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length != 3) {
			System.err
					.println("Usage <inputFolder> variables|methodinvocations|"
							+ "methodinvocations_typegram|methoddeclarations|methoddeclarations_nooverride"
							+ "methoddeclarations_typegram|types <outputFolderAndPrefix>");
			System.exit(-1);
		}

		final File inputFolder = new File(args[0]);
		final String outputFolderAndPrefix = args[2];
		final AbstractJavaNameBindingsExtractor bindingExtractor = JavaBindingsToJson
				.getExtractorForName(args[1], inputFolder);

		for (final Object featureType : bindingExtractor.getAvailableFeatures()) {
			try {
				System.out.println("Using only " + featureType + " feature");
				bindingExtractor
						.setActiveFeatures(Sets.newHashSet(featureType));
				final File outputFile = new File(outputFolderAndPrefix
						+ featureType.toString() + ".json");
				System.out.println("Generating at " + outputFile);
				JavaBindingsToJson.extractBindings(inputFolder, outputFile,
						bindingExtractor);
			} catch (JsonIOException | IOException e) {
				e.printStackTrace();
			}
		}

		try {
			System.out.println("Using no features");
			bindingExtractor.setActiveFeatures(Collections.EMPTY_SET);
			final File outputFile = new File(outputFolderAndPrefix
					+ "NO_FEAT.json");
			System.out.println("Generating at " + outputFile);
			JavaBindingsToJson.extractBindings(inputFolder, outputFile,
					bindingExtractor);
		} catch (JsonIOException | IOException e) {
			e.printStackTrace();
		}
	}

	private JavaBindingsPerFeatureTypeToJson() {
		// No instantiations.
	}

}
