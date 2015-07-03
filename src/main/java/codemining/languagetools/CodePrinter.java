package codemining.languagetools;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import codemining.languagetools.ITokenizer.FullToken;
import codemining.util.SettingsLoader;

/**
 * Output Java code to HTML with optional coloring. Not thread-safe.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class CodePrinter {

	/**
	 * The tokenizer used to tokenize code.
	 */
	final ITokenizer tokenizer;

	/**
	 * The background Color of the output HTML document.
	 */
	final Color documentBackgroundColor;

	int lineNumber = 1;

	private final boolean ignoreTokBG = SettingsLoader.getBooleanSetting(
			"ignoreTokenBackground", true);

	public static final String CSS_STYLE = "<style>\n.line {font-family:monospace; "
			+ "font: 14px/1.3 \"Source Code Pro\", \"Fira Mono OT\", monospace;white-space:pre;}\n"
			+ ".line:hover {font-family:monospace; "
			+ "font: 14px/1.3 \"Source Code Pro\", \"Fira Mono OT\", monospace;white-space:pre; background-color:rgb(240,240,240);}\n"
			+ "</style>";

	public CodePrinter(final ITokenizer tokenizer,
			final Color documentBackgroundColor) {
		this.tokenizer = tokenizer;
		this.documentBackgroundColor = documentBackgroundColor;
	}

	private void addSlack(final String substring, final StringBuffer buf) {
		for (final char c : StringEscapeUtils.escapeHtml(substring)
				.toCharArray()) {
			if (c == '\n') {
				appendLineDiv(buf, true);
			} else {
				buf.append(c);
			}
		}

	}

	private void appendLineDiv(final StringBuffer buf,
			final boolean closePrevious) {
		if (closePrevious) {
			buf.append("<br/></div>\n");
		}
		buf.append("<div class='line' id='C" + lineNumber + "'>");
		lineNumber++;
	}

	/**
	 * Return a StringBuffer with colored tokens as specified from the
	 * coloredTokens. There should be one-to-one correspondence with the actual
	 * tokens.
	 */
	public StringBuffer getHTMLwithColors(
			final List<ColoredToken> coloredTokens, final File codeFile)
			throws IOException, InstantiationException, IllegalAccessException {
		final String code = FileUtils.readFileToString(codeFile);
		lineNumber = 1;

		final StringBuffer buf = new StringBuffer();

		final SortedMap<Integer, FullToken> toks = tokenizer
				.fullTokenListWithPos(code.toCharArray());

		int i = 0;
		int prevPos = 0;
		buf.append("<html>\n<head>\n<link href='http://fonts.googleapis.com/css?family=Source+Code+Pro:300,400,500,600,700,800,900' rel='stylesheet' type='text/css'>\n");
		buf.append(CSS_STYLE);
		buf.append("</head>\n<body style='background-color:rgb("
				+ documentBackgroundColor.getRed() + ","
				+ documentBackgroundColor.getGreen() + ","
				+ documentBackgroundColor.getBlue() + ")'>");
		appendLineDiv(buf, false);
		for (final Entry<Integer, FullToken> entry : toks.entrySet()) {
			if (i == 0 || entry.getKey() == Integer.MAX_VALUE) {
				i++;
				continue;
			}
			addSlack(code.substring(prevPos, entry.getKey()), buf);
			final ColoredToken tok = coloredTokens.get(i);

			buf.append("<span style='background-color:rgba("
					+ tok.bgColor.getRed() + "," + tok.bgColor.getGreen() + ","
					+ tok.bgColor.getBlue() + "," + (ignoreTokBG ? "0" : "1")
					+ "); color:rgb(" + tok.fontColor.getRed() + ","
					+ tok.fontColor.getGreen() + "," + tok.fontColor.getBlue()
					+ "); " + tok.extraStyle + "'>"
					+ StringEscapeUtils.escapeHtml(entry.getValue().token)
					+ "</span>");
			i++;
			prevPos = entry.getKey() + entry.getValue().token.length();
		}
		buf.append("</div></body></html>");
		return buf;

	}
}