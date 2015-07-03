/**
 * 
 */
package codemining.languagetools.tui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.TokenizerUtils;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;

/**
 * Print to stdout the total count of all unique tokens in the text.
 * 
 * Used to answer the question: Do we have a zipf-ian distribution of tokens in
 * Java Code?
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class DistinctTokenCount {

	private static final Logger LOGGER = Logger
			.getLogger(DistinctTokenCount.class.getName());

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main(final String[] args) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {

		if (args.length != 2) {
			System.err.println("Usage: <directory> <tokenizerClass>");
			return;
		}

		final DistinctTokenCount tokCount = new DistinctTokenCount(args[1]);
		for (final File fi : FileUtils.listFiles(new File(args[0]),
				new RegexFileFilter(".*\\.java$"),
				DirectoryFileFilter.DIRECTORY)) {
			try {
				tokCount.addTokens(fi);
			} catch (final IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}

		tokCount.printCounts();
	}

	private final TreeMultiset<String> allTokens = TreeMultiset.create();

	private final ITokenizer tokenizer;

	public DistinctTokenCount(final String tokenizerClass)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		tokenizer = TokenizerUtils.tokenizerForClass(tokenizerClass);
	}

	public void addTokens(final File file) throws IOException {
		LOGGER.finer("Reading file " + file.getAbsolutePath());
		final char[] code = FileUtils.readFileToString(file).toCharArray();
		final List<String> tokens = tokenizer.tokenListFromCode(code);
		allTokens.addAll(tokens);

	}

	/**
	 * Prints the counts.
	 */
	public void printCounts() {
		for (final Entry<String> token : allTokens.entrySet()) {
			System.out.println(token.getCount());
		}
	}

}
