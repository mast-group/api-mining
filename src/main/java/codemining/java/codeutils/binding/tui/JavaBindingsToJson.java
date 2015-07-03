/**
 *
 */
package codemining.java.codeutils.binding.tui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.java.codeutils.binding.AbstractJavaNameBindingsExtractor;
import codemining.java.codeutils.binding.JavaApproximateVariableBindingExtractor;
import codemining.java.codeutils.binding.JavaMethodDeclarationBindingExtractor;
import codemining.java.codeutils.binding.JavaMethodInvocationBindingExtractor;
import codemining.java.codeutils.binding.JavaTypeDeclarationBindingExtractor;
import codemining.java.tokenizers.JavaTokenizer;
import codemining.java.tokenizers.JavaTypeTokenizer;
import codemining.languagetools.bindings.ResolvedSourceCode;
import codemining.languagetools.bindings.TokenNameBinding;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;

/**
 * Convert a set of files to a set of bindings and serialize in a msgpack
 * format.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaBindingsToJson {

	public static class SerializableResolvedSourceCode {

		public static SerializableResolvedSourceCode fromResolvedSourceCode(
				final ResolvedSourceCode rsc) {
			return new SerializableResolvedSourceCode(rsc);
		}

		public final String provenance;

		public final List<String> codeTokens;

		public final List<List<Integer>> boundVariables;

		public final List<List<String>> boundVariableFeatures;

		protected SerializableResolvedSourceCode(final ResolvedSourceCode rsc) {
			codeTokens = rsc.codeTokens;
			boundVariables = Lists.newArrayList();
			boundVariableFeatures = Lists.newArrayList();
			for (final TokenNameBinding binding : rsc.getAllBindings()) {
				boundVariables.add(new ArrayList<Integer>(binding.nameIndexes));
				boundVariableFeatures.add(new ArrayList<String>(
						binding.features));
			}
			provenance = rsc.name;
		}
	}

	/**
	 * Extract the bindings from the input folder to the output file, using the
	 * bindingExtractor.
	 *
	 * @param inputFolder
	 * @param outputFile
	 * @param bindingExtractor
	 * @throws IOException
	 * @throws JsonIOException
	 */
	public static void extractBindings(final File inputFolder,
			final File outputFile,
			final AbstractJavaNameBindingsExtractor bindingExtractor)
			throws IOException, JsonIOException {
		final Collection<File> allFiles = FileUtils
				.listFiles(inputFolder, JavaTokenizer.javaCodeFileFilter,
						DirectoryFileFilter.DIRECTORY);
		final List<SerializableResolvedSourceCode> resolvedCode = allFiles
				.parallelStream()
				.map(f -> getResolvedCode(f, bindingExtractor))
				.filter(r -> r != null)
				.map(r -> SerializableResolvedSourceCode
						.fromResolvedSourceCode(r))
				.filter(s -> !s.boundVariables.isEmpty())
				.collect(Collectors.toList());

		final FileWriter writer = new FileWriter(outputFile);
		try {
			final Gson gson = new Gson();
			gson.toJson(resolvedCode, writer);
		} finally {
			writer.close();
		}
	}

	/**
	 * @param bindingExtractor
	 * @param extractorType
	 * @param inputFolder
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static AbstractJavaNameBindingsExtractor getExtractorForName(
			final String extractorType, final File inputFolder)
			throws IllegalArgumentException {
		final AbstractJavaNameBindingsExtractor bindingExtractor;
		if (extractorType.equals("variables")) {
			bindingExtractor = new JavaApproximateVariableBindingExtractor();
		} else if (extractorType.equals("methodinvocations")) {
			bindingExtractor = new JavaMethodInvocationBindingExtractor();
		} else if (extractorType.equals("methodinvocations_typegram")) {
			bindingExtractor = new JavaMethodInvocationBindingExtractor(
					new JavaTypeTokenizer());
		} else if (extractorType.equals("methoddeclarations")) {
			bindingExtractor = new JavaMethodDeclarationBindingExtractor();
		} else if (extractorType.equals("methoddeclarations_nooverride")) {
			bindingExtractor = new JavaMethodDeclarationBindingExtractor(false,
					inputFolder);
		} else if (extractorType.equals("methoddeclarations_typegram")) {
			bindingExtractor = new JavaMethodDeclarationBindingExtractor(
					new JavaTypeTokenizer());
		} else if (extractorType.equals("types")) {
			bindingExtractor = new JavaTypeDeclarationBindingExtractor();
		} else {
			throw new IllegalArgumentException("Unrecognized option "
					+ extractorType);
		}
		return bindingExtractor;
	}

	public static ResolvedSourceCode getResolvedCode(final File f,
			final AbstractJavaNameBindingsExtractor extractor) {
		try {
			return extractor.getResolvedSourceCode(f);
		} catch (final Throwable t) {
			LOGGER.warning("Error for file " + f + ": "
					+ ExceptionUtils.getFullStackTrace(t));
		}
		return null;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws JsonIOException
	 */
	public static void main(final String[] args) throws JsonIOException,
			IOException {
		if (args.length != 3) {
			System.err
					.println("Usage <inputFolder> variables|methodinvocations|"
							+ "methodinvocations_typegram|methoddeclarations|methoddeclarations_nooverride"
							+ "methoddeclarations_typegram|types <outputFile>");
			System.exit(-1);
		}

		final File inputFolder = new File(args[0]);
		final File outputFile = new File(args[2]);
		final AbstractJavaNameBindingsExtractor bindingExtractor = getExtractorForName(
				args[1], inputFolder);

		extractBindings(inputFolder, outputFile, bindingExtractor);
	}

	private static final Logger LOGGER = Logger
			.getLogger(JavaBindingsToJson.class.getName());

	private JavaBindingsToJson() {
		// No instantations
	}

}
