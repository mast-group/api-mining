package codemining.languagetools.tokenizers.whitespace;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility stateful class for converting whitespace tokens to whitespace.
 * 
 */
public final class WhitespaceTokenConverter {

	/**
	 * A struct class.
	 * 
	 */
	private static final class Whitespace {
		int nTabs;
		int nSpace;
		int nNewLines;
	}

	private int currentSpaceIndentation = 0;
	private int currentTabIndentation = 0;

	public static final Pattern INDENT_PATTERN = Pattern
			.compile("WS_INDENTs([0-9]+)t([0-9]+)n([0-9]+)");

	public static final Pattern DEDENT_PATTERN = Pattern
			.compile("WS_DEDENTs(-?\\d+)t(-?\\d+)n(\\d+)");

	public static final Pattern SPACE_PATTERN = Pattern
			.compile("WS_s(\\d+)t(\\d+)");

	/**
	 * Append whitespace to StringBuffer, given the specifications.
	 * 
	 * @param nSpace
	 * @param nTab
	 * @param startAtNewLine
	 * @return
	 */
	public static final void createWhitespace(
			final WhitespaceTokenConverter.Whitespace space, final StringBuffer sb) {
		for (int i = 0; i < space.nNewLines; i++) {
			sb.append(System.getProperty("line.separator"));
		}
		for (int i = 0; i < space.nSpace; i++) {
			sb.append(" ");
		}
		for (int i = 0; i < space.nTabs; i++) {
			sb.append("\t");
		}
	}

	/**
	 * Whitespace token converter.
	 * 
	 * @param wsToken
	 * @param buffer
	 */
	public void appendWS(final String wsToken, final StringBuffer buffer) {
		checkArgument(wsToken.startsWith("WS_"));
		final WhitespaceTokenConverter.Whitespace space;
		if (wsToken.startsWith("WS_INDENT")) {
			space = convert(wsToken, INDENT_PATTERN);
			currentSpaceIndentation += space.nSpace;
			currentTabIndentation += space.nTabs;
			space.nSpace = currentSpaceIndentation;
			space.nTabs = currentTabIndentation;

		} else if (wsToken.startsWith("WS_DEDENT")) {
			space = convert(wsToken, DEDENT_PATTERN);
			currentSpaceIndentation -= space.nSpace;
			if (currentSpaceIndentation < 0) {
				currentSpaceIndentation = 0;
			}
			currentTabIndentation -= space.nTabs;
			if (currentTabIndentation < 0) {
				currentTabIndentation = 0;
			}
			space.nSpace = currentSpaceIndentation;
			space.nTabs = currentTabIndentation;
		} else {
			space = convert(wsToken, SPACE_PATTERN);
		}
		createWhitespace(space, buffer);
	}

	private WhitespaceTokenConverter.Whitespace convert(final String wsToken,
			final Pattern patternToMatch) {
		final WhitespaceTokenConverter.Whitespace space = new Whitespace();
		final Matcher m = patternToMatch.matcher(wsToken);
		checkArgument(m.matches(), "Pattern " + patternToMatch.toString()
				+ " does not match " + wsToken);
		space.nSpace = Integer.parseInt(m.group(1));
		space.nTabs = Integer.parseInt(m.group(2));
		if (m.groupCount() == 3) {
			space.nNewLines = Integer.parseInt(m.group(3));
		}
		return space;
	}
}