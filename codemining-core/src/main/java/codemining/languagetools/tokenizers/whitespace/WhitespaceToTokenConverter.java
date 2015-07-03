package codemining.languagetools.tokenizers.whitespace;


/**
 * A stateful whitespace to whitespace token converter.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class WhitespaceToTokenConverter {
	private int currentIdentationSpaces = 0;
	private int currentIdentationTabs = 0;

	/**
	 * Convert the given symbol to whitespace token.
	 * 
	 * @param token
	 * @return
	 */
	public String toWhiteSpaceSymbol(final String token) {
		final String symbol;
		int spaces = 0;
		int tabs = 0;
		int newLines = 0;
		for (final char c : token.replace("\r", "").toCharArray()) {
			if (c == '\n') {
				newLines++;
			} else if (c == '\t') {
				tabs++;
			} else if (c == ' ') {
				spaces++;
			}
		}

		if (newLines == 0) {
			symbol = "WS_s" + spaces + "t" + tabs;
		} else if (newLines > 0) {
			final int spaceDiff = spaces - currentIdentationSpaces;
			final int tabDiff = tabs - currentIdentationTabs;
			currentIdentationSpaces = spaces;
			currentIdentationTabs = tabs;

			if (spaceDiff >= 0 && tabDiff >= 0) {
				symbol = "WS_INDENTs" + spaceDiff + "t" + tabDiff + "n"
						+ newLines;
			} else {
				symbol = "WS_DEDENTs" + -spaceDiff + "t" + -tabDiff + "n"
						+ newLines;
			}
		} else {
			throw new IllegalStateException();
		}
		return symbol;
	}

}