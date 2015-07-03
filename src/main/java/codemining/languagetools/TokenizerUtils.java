/**
 * 
 */
package codemining.languagetools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkPositionIndex;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import codemining.languagetools.ITokenizer.FullToken;
import codemining.util.SettingsLoader;

/**
 * Utility function relevant to tokenization.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class TokenizerUtils {

	public static final int TAB_INDENT_SIZE = (int) SettingsLoader
			.getNumericSetting("tabSize", 4);

	/**
	 * Return the column of the given position.
	 * 
	 * @param code
	 * @param position
	 * @return
	 */
	public static int getColumnOfPosition(final String code, final int position) {
		checkPositionIndex(position, code.length());
		int newLinePosition = code.substring(0, position).lastIndexOf("\n");
		if (newLinePosition == -1) {
			newLinePosition = 0; // Start of file.
		}
		final int tabCount = StringUtils.countMatches(
				code.substring(newLinePosition, position), "\t");
		return position - newLinePosition + (TAB_INDENT_SIZE - 1) * tabCount;
	}

	/**
	 * Crudely join tokens together.
	 * 
	 * @param tokens
	 * @param sb
	 * @return
	 */
	public final static StringBuffer joinFullTokens(
			final List<FullToken> tokens, final StringBuffer sb) {
		for (final FullToken token : tokens) {
			sb.append(token.token);
			sb.append(" ");
		}

		return sb;
	}

	/**
	 * Crudely join tokens together.
	 * 
	 * @param tokens
	 * @param sb
	 * @return
	 */
	public final static StringBuffer joinTokens(final List<String> tokens) {
		final StringBuffer sb = new StringBuffer();
		for (final String token : tokens) {
			sb.append(token);
			sb.append(" ");
		}

		return sb;
	}

	/**
	 * Crudely join tokens together.
	 * 
	 * @param tokens
	 * @param sb
	 * @return
	 */
	public final static StringBuffer joinTokens(final List<String> tokens,
			final StringBuffer sb) {
		for (final String token : tokens) {
			sb.append(token);
			sb.append(" ");
		}

		return sb;
	}

	/**
	 * Remove the sentence start/end FullTokens.
	 * 
	 * @param tokenSequence
	 */
	public static final void removeSentenceStartEndFullTokens(
			final List<FullToken> tokenSequence) {
		checkArgument(tokenSequence.get(0).token
				.equals(ITokenizer.SENTENCE_START));
		tokenSequence.remove(0);
		checkArgument(tokenSequence.get(tokenSequence.size() - 1).token
				.equals(ITokenizer.SENTENCE_END));
		tokenSequence.remove(tokenSequence.size() - 1);
	}

	/**
	 * Remove the sentence start/end tokens.
	 * 
	 * @param tokenSequence
	 */
	public static final void removeSentenceStartEndTokens(
			final List<String> tokenSequence) {
		checkArgument(tokenSequence.get(0).equals(ITokenizer.SENTENCE_START));
		tokenSequence.remove(0);
		checkArgument(tokenSequence.get(tokenSequence.size() - 1).equals(
				ITokenizer.SENTENCE_END));
		tokenSequence.remove(tokenSequence.size() - 1);
	}

	private TokenizerUtils() {
		// Utilty class
	}

	/**
	 * @param tokenizerClass
	 * @param tokenizerArguments
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 */
	public static ITokenizer tokenizerForClass(final String tokenizerClass,
			final String tokenizerArguments) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException {
		return (ITokenizer) Class.forName(tokenizerClass)
				.getDeclaredConstructor(String.class)
				.newInstance(tokenizerArguments);
	}

	public static ITokenizer tokenizerForClass(final String tokenizerClass,
			final Boolean tokenizerArguments) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException {
		return (ITokenizer) Class.forName(tokenizerClass)
				.getDeclaredConstructor(Boolean.TYPE)
				.newInstance(tokenizerArguments);
	}

	/**
	 * @param tokenizerClass
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static ITokenizer tokenizerForClass(final String tokenizerClass)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		return (ITokenizer) Class.forName(tokenizerClass).newInstance();
	}

}
