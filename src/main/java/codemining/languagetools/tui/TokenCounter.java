/**
 * 
 */
package codemining.languagetools.tui;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.TokenizerUtils;

/**
 * Utility for counting all the tokens in a folder.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class TokenCounter {

	private static final Logger LOGGER = Logger.getLogger(TokenCounter.class
			.getName());

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main(final String[] args) throws IOException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		if (args.length != 2) {
			System.err.println("Usage <codeDir> <TokenizerClass>");
			return;
		}

		long tokenCount = 0;

		final ITokenizer tokenizer = TokenizerUtils.tokenizerForClass(args[1]);

		for (final File fi : FileUtils.listFiles(new File(args[0]),
				tokenizer.getFileFilter(), DirectoryFileFilter.DIRECTORY)) {
			try {
				final char[] code = FileUtils.readFileToString(fi)
						.toCharArray();
				tokenCount += tokenizer.tokenListFromCode(code).size() - 2; // Remove
																			// sentence
																			// start/end
			} catch (final IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}

		System.out.println("Tokens: " + tokenCount);
	}
}
