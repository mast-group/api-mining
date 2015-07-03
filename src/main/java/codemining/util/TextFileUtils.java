/**
 * 
 */
package codemining.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * Text file utilities
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public final class TextFileUtils {

	/**
	 * Return the number of lines in this file.
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static int numberOfLinesInFile(final File f) throws IOException {
		return Files.readLines(f, Charset.defaultCharset(),
				new LineProcessor<Integer>() {
					int count = 0;

					@Override
					public Integer getResult() {
						return count;
					}

					@Override
					public boolean processLine(final String line) {
						count++;
						return true;
					}
				});
	};

	private TextFileUtils() {
	}

}
