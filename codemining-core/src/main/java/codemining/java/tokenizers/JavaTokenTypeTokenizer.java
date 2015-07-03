/**
 *
 */
package codemining.java.tokenizers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.core.util.PublicScanner;

import codemining.languagetools.ITokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A tokenizer of Java code but it returns the token type.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaTokenTypeTokenizer implements ITokenizer {

	/**
	 * @param token
	 * @return
	 */
	public static boolean isLiteralToken(final int token) {
		return token == ITerminalSymbols.TokenNameStringLiteral
				|| token == ITerminalSymbols.TokenNameCharacterLiteral
				|| token == ITerminalSymbols.TokenNameFloatingPointLiteral
				|| token == ITerminalSymbols.TokenNameIntegerLiteral
				|| token == ITerminalSymbols.TokenNameLongLiteral
				|| token == ITerminalSymbols.TokenNameDoubleLiteral;
	}

	private static final long serialVersionUID = 7532823864395627836L;

	private static final Logger LOGGER = Logger
			.getLogger(JavaTokenTypeTokenizer.class.getName());
	private final RegexFileFilter javaCodeFiler = new RegexFileFilter(
			".*\\.java$");
	private final boolean tokenizeComments;
	public static final String LITERAL_TOKEN = "LITERAL";
	public static final String IDENTIFIER_TOKEN = "IDENTIFIER";
	public static final String COMMENT_JAVADOC = "COMMENT_JAVADOC";
	public static final String COMMENT_LINE = "COMMENT_LINE";

	public static final String COMMENT_BLOCK = "COMMENT_BLOCK";

	public JavaTokenTypeTokenizer() {
		this.tokenizeComments = false;
	}

	public JavaTokenTypeTokenizer(final boolean tokenizeComments) {
		this.tokenizeComments = tokenizeComments;
	}

	/**
	 * @return
	 */
	private PublicScanner createScanner() {
		final PublicScanner scanner = new PublicScanner();
		scanner.tokenizeComments = tokenizeComments;
		return scanner;
	}

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		final SortedMap<Integer, FullToken> tokens = Maps.newTreeMap();
		tokens.put(-1, new FullToken(SENTENCE_START, SENTENCE_START));
		tokens.put(Integer.MAX_VALUE, new FullToken(SENTENCE_END, SENTENCE_END));
		final PublicScanner scanner = createScanner();
		scanner.setSource(code);
		while (!scanner.atEnd()) {
			do {
				try {
					final int token = scanner.getNextToken();
					final int position = scanner.getCurrentTokenStartPosition();

					if (token == ITerminalSymbols.TokenNameEOF) {
						break;
					} else if (token == ITerminalSymbols.TokenNameIdentifier) {
						tokens.put(position,
								new FullToken(IDENTIFIER_TOKEN, ""));
					} else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
						tokens.put(position, new FullToken(COMMENT_BLOCK, ""));
					} else if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
						tokens.put(position, new FullToken(COMMENT_JAVADOC, ""));
					} else if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
						tokens.put(position, new FullToken(COMMENT_LINE, ""));
					} else if (isLiteralToken(token)) {
						tokens.put(position, new FullToken(LITERAL_TOKEN, ""));
					} else {
						tokens.put(position,
								new FullToken(scanner.getCurrentTokenString(),
										""));
					}

				} catch (final InvalidInputException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
			} while (!scanner.atEnd());

		}
		return tokens;
	}

	@Override
	public AbstractFileFilter getFileFilter() {
		return javaCodeFiler;
	}

	@Override
	public String getIdentifierType() {
		// We do not return types here...
		throw new IllegalArgumentException("Retrieving types is not possible");
	}

	@Override
	public Collection<String> getKeywordTypes() {
		throw new IllegalArgumentException("Retrieving types is not possible");
	}

	@Override
	public Collection<String> getLiteralTypes() {
		throw new IllegalArgumentException("Retrieving types is not possible");
	}

	@Override
	public FullToken getTokenFromString(final String token) {
		return new FullToken(token, "");
	}

	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final List<FullToken> tokens = Lists.newArrayList();
		tokens.add(new FullToken(SENTENCE_START, SENTENCE_START));
		final PublicScanner scanner = createScanner();
		scanner.setSource(code);
		do {
			try {
				final int token = scanner.getNextToken();
				if (token == ITerminalSymbols.TokenNameEOF) {
					break;
				} else if (token == ITerminalSymbols.TokenNameIdentifier) {
					tokens.add(new FullToken(IDENTIFIER_TOKEN, ""));
				} else if (isLiteralToken(token)) {
					tokens.add(new FullToken(LITERAL_TOKEN, ""));
				} else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
					tokens.add(new FullToken(COMMENT_BLOCK, ""));
				} else if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
					tokens.add(new FullToken(COMMENT_JAVADOC, ""));
				} else if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
					tokens.add(new FullToken(COMMENT_LINE, ""));
				} else {
					tokens.add(new FullToken(scanner.getCurrentTokenString(),
							""));
				}

			} catch (final InvalidInputException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		} while (!scanner.atEnd());
		tokens.add(new FullToken(SENTENCE_END, SENTENCE_END));
		return tokens;
	}

	@Override
	public List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException {
		return getTokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see uk.ac.ed.inf.javacodeutils.ITokenizer#tokenListFromCode(char[])
	 */
	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final List<String> tokens = Lists.newArrayList();
		tokens.add(SENTENCE_START);
		final PublicScanner scanner = createScanner();
		scanner.setSource(code);
		do {
			try {
				final int token = scanner.getNextToken();
				if (token == ITerminalSymbols.TokenNameEOF) {
					break;
				} else if (token == ITerminalSymbols.TokenNameIdentifier) {
					tokens.add(IDENTIFIER_TOKEN);
				} else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
					tokens.add(COMMENT_BLOCK);
				} else if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
					tokens.add(COMMENT_LINE);
				} else if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
					tokens.add(COMMENT_JAVADOC);
				} else if (isLiteralToken(token)) {
					tokens.add(LITERAL_TOKEN);
				} else {
					tokens.add(scanner.getCurrentTokenString());
				}

			} catch (final InvalidInputException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		} while (!scanner.atEnd());
		tokens.add(SENTENCE_END);
		return tokens;
	}

	@Override
	public List<String> tokenListFromCode(final File codeFile)
			throws IOException {
		return tokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see uk.ac.ed.inf.javacodeutils.ITokenizer#tokenListWithPos(char[])
	 */
	@Override
	public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
		final SortedMap<Integer, String> tokens = Maps.newTreeMap();
		tokens.put(-1, SENTENCE_START);
		tokens.put(Integer.MAX_VALUE, SENTENCE_END);
		final PublicScanner scanner = createScanner();
		scanner.setSource(code);
		while (!scanner.atEnd()) {
			do {
				try {
					final int token = scanner.getNextToken();
					final int position = scanner.getCurrentTokenStartPosition();

					if (token == ITerminalSymbols.TokenNameEOF) {
						break;
					} else if (token == ITerminalSymbols.TokenNameIdentifier) {
						tokens.put(position, IDENTIFIER_TOKEN);
					} else if (isLiteralToken(token)) {
						tokens.put(position, LITERAL_TOKEN);
					} else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
						tokens.put(position, COMMENT_BLOCK);
					} else if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
						tokens.put(position, COMMENT_JAVADOC);
					} else if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
						tokens.put(position, COMMENT_LINE);
					} else {
						tokens.put(position, scanner.getCurrentTokenString());
					}

				} catch (final InvalidInputException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
			} while (!scanner.atEnd());

		}
		return tokens;
	}

	@Override
	public SortedMap<Integer, FullToken> tokenListWithPos(final File file)
			throws IOException {
		return fullTokenListWithPos(FileUtils.readFileToString(file)
				.toCharArray());
	}
}
