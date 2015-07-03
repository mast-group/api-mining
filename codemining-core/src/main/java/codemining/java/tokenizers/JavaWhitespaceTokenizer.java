/**
 *
 */
package codemining.java.tokenizers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.core.util.PublicScanner;

import codemining.languagetools.IFormattingTokenizer;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.tokenizers.whitespace.WhitespaceToTokenConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A token type tokenizer that tokenizes whitespace. The implementation is
 * thread safe.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaWhitespaceTokenizer implements IFormattingTokenizer {

	/**
	 * The non-thread-safe implementation.
	 *
	 */
	static class TokenizerImplementation implements ITokenizer {

		private static final long serialVersionUID = 3466332155585174404L;

		private final RegexFileFilter javaCodeFilter = new RegexFileFilter(
				".*\\.java$");

		WhitespaceToTokenConverter whitespaceConverter = new WhitespaceToTokenConverter();

		/*
		 * (non-Javadoc)
		 *
		 * @see codemining.languagetools.ITokenizer#fullTokenListWithPos(char[])
		 */
		@Override
		public SortedMap<Integer, FullToken> fullTokenListWithPos(
				final char[] code) {
			final SortedMap<Integer, FullToken> tokenList = Maps.newTreeMap();
			for (final Entry<Integer, String> token : tokenListWithPos(code)
					.entrySet()) {
				tokenList.put(token.getKey(), new FullToken(token.getValue(),
						""));
			}
			return tokenList;
		}

		/**
		 * @param tokens
		 * @param scanner
		 * @param token
		 * @return
		 * @throws InvalidInputException
		 */
		private List<String> getConvertedToken(final PublicScanner scanner,
				final int token) throws InvalidInputException {
			final List<String> tokens = Lists.newArrayList();
			if (token == ITerminalSymbols.TokenNameEOF) {
				return Collections.emptyList();
			}
			final String tokenString = scanner.getCurrentTokenString();

			if (token == ITerminalSymbols.TokenNameWHITESPACE) {
				tokens.add(whitespaceConverter.toWhiteSpaceSymbol(tokenString));
			} else if (token == ITerminalSymbols.TokenNameIdentifier) {
				tokens.add(toIdentifierSymbol(tokenString));
			} else if (JavaTokenTypeTokenizer.isLiteralToken(token)) {
				tokens.add(toLiteralSymbol(tokenString));
			} else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
				tokens.add(JavaTokenTypeTokenizer.COMMENT_BLOCK);
			} else if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
				tokens.add(JavaTokenTypeTokenizer.COMMENT_LINE);
				final int nextToken = scanner.getNextToken();
				if (nextToken == ITerminalSymbols.TokenNameWHITESPACE) {
					tokens.add(whitespaceConverter.toWhiteSpaceSymbol("\n"
							+ scanner.getCurrentTokenString()));
				} else {
					tokens.add(whitespaceConverter.toWhiteSpaceSymbol("\n"));
					tokens.addAll(getConvertedToken(scanner, nextToken));
				}
			} else if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
				tokens.add(JavaTokenTypeTokenizer.COMMENT_JAVADOC);
			} else {
				tokens.add(tokenString);
			}

			return tokens;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see codemining.languagetools.ITokenizer#getFileFilter()
		 */
		@Override
		public AbstractFileFilter getFileFilter() {
			return javaCodeFilter;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see codemining.languagetools.ITokenizer#getIdentifierType()
		 */
		@Override
		public String getIdentifierType() {
			// We do not return types here...
			throw new IllegalArgumentException("No token types are available");
		}

		@Override
		public Collection<String> getKeywordTypes() {
			throw new IllegalArgumentException("No token types are available");
		}

		@Override
		public Collection<String> getLiteralTypes() {
			throw new IllegalArgumentException("No token types are available");
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * codemining.languagetools.ITokenizer#getTokenFromString(java.lang.
		 * String)
		 */
		@Override
		public FullToken getTokenFromString(final String token) {
			return new FullToken(token, "");
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see codemining.languagetools.ITokenizer#getTokenListFromCode(char[])
		 */
		@Override
		public List<FullToken> getTokenListFromCode(final char[] code) {
			final List<FullToken> tokens = Lists.newArrayList();
			tokens.add(new FullToken(SENTENCE_START, SENTENCE_START));
			final PublicScanner scanner = prepareScanner(code);
			do {
				try {
					final int token = scanner.getNextToken();
					if (token == ITerminalSymbols.TokenNameEOF) {
						break;
					}
					for (final String cToken : getConvertedToken(scanner, token)) {
						tokens.add(new FullToken(cToken, ""));
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

		public List<WhitespaceAnnotatedToken> getTokensWithWidthData(
				final char[] code) {
			final List<WhitespaceAnnotatedToken> tokens = Lists.newArrayList();
			tokens.add(new WhitespaceAnnotatedToken(SENTENCE_START,
					SENTENCE_START, 0, 0));
			final PublicScanner scanner = prepareScanner(code);
			do {
				try {
					final int token = scanner.getNextToken();
					if (token == ITerminalSymbols.TokenNameEOF) {
						break;
					}
					for (final String cToken : getConvertedToken(scanner, token)) {
						final int currentPosition = scanner
								.getCurrentTokenStartPosition();
						final int currentLine = scanner
								.getLineNumber(currentPosition);
						final int lineStart = scanner.getLineStart(currentLine);
						tokens.add(new WhitespaceAnnotatedToken(cToken, "",
								currentPosition - lineStart, scanner
								.getCurrentTokenString().length()));
					}
				} catch (final InvalidInputException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
			} while (!scanner.atEnd());
			tokens.add(new WhitespaceAnnotatedToken(SENTENCE_END, SENTENCE_END,
					0, 0));
			return tokens;
		}

		/**
		 * @param code
		 * @return
		 */
		private PublicScanner prepareScanner(final char[] code) {
			final PublicScanner scanner = new PublicScanner();
			scanner.setSource(code);
			scanner.tokenizeWhiteSpace = true;
			scanner.recordLineSeparator = true;
			scanner.tokenizeComments = true;
			whitespaceConverter = new WhitespaceToTokenConverter();
			return scanner;
		}

		public String toIdentifierSymbol(final String token) {
			return JavaTokenTypeTokenizer.IDENTIFIER_TOKEN;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see codemining.languagetools.ITokenizer#tokenListFromCode(char[])
		 */
		@Override
		public List<String> tokenListFromCode(final char[] code) {
			final List<String> tokens = Lists.newArrayList();
			tokens.add(SENTENCE_START);
			final PublicScanner scanner = prepareScanner(code);
			do {
				try {
					final int token = scanner.getNextToken();
					if (token == ITerminalSymbols.TokenNameEOF) {
						break;
					}
					tokens.addAll(getConvertedToken(scanner, token));
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
		 * @see codemining.languagetools.ITokenizer#tokenListWithPos(char[])
		 */
		@Override
		public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
			final SortedMap<Integer, String> tokens = Maps.newTreeMap();
			tokens.put(-1, SENTENCE_START);
			tokens.put(Integer.MAX_VALUE, SENTENCE_END);
			final PublicScanner scanner = prepareScanner(code);

			while (!scanner.atEnd()) {
				do {
					try {
						final int token = scanner.getNextToken();
						final int position = scanner
								.getCurrentTokenStartPosition();
						if (token == ITerminalSymbols.TokenNameEOF) {
							break;
						}
						int i = 0;
						final List<String> cTokens = getConvertedToken(scanner,
								token);
						for (final String cToken : cTokens) {
							tokens.put(position + i, cToken);
							i++;
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

		public SortedMap<Integer, WhitespaceAnnotatedToken> tokenListWithPosAndWidth(
				final char[] code) {
			final SortedMap<Integer, WhitespaceAnnotatedToken> tokens = Maps
					.newTreeMap();
			tokens.put(-1, new WhitespaceAnnotatedToken(SENTENCE_START,
					SENTENCE_START, 0, 0));
			tokens.put(Integer.MAX_VALUE, new WhitespaceAnnotatedToken(
					SENTENCE_END, SENTENCE_END, 0, 0));
			final PublicScanner scanner = prepareScanner(code);

			while (!scanner.atEnd()) {
				do {
					try {
						final int token = scanner.getNextToken();
						final int currentPosition = scanner
								.getCurrentTokenStartPosition();
						final int currentLine = scanner
								.getLineNumber(currentPosition);
						final int lineStart = scanner.getLineStart(currentLine);
						final int position = scanner
								.getCurrentTokenStartPosition();
						if (token == ITerminalSymbols.TokenNameEOF) {
							break;
						}
						int i = 0;
						final List<String> cTokens = getConvertedToken(scanner,
								token);
						for (final String cToken : cTokens) {
							tokens.put(position + i,
									new WhitespaceAnnotatedToken(cToken, "",
											currentPosition - lineStart,
											scanner.getCurrentTokenString()
											.length()));
							i++;
						}
					} catch (final InvalidInputException e) {
						LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
					}
				} while (!scanner.atEnd());
			}
			return tokens;
		}

		public String toLiteralSymbol(final String token) {
			return JavaTokenTypeTokenizer.LITERAL_TOKEN;
		}
	}

	/**
	 * A struct of an annotated token.
	 *
	 */
	public static class WhitespaceAnnotatedToken {

		public final String token;

		public final String tokenType;

		/**
		 * The column where this token starts.
		 */
		public final int column;

		/**
		 * The number of characters that this token has.
		 */
		public final int width;

		public WhitespaceAnnotatedToken(final String value,
				final String tokenType, final int column, final int width) {
			token = value;
			this.tokenType = tokenType;
			this.width = width;
			this.column = column;
		}
	}

	private static final long serialVersionUID = -3956186603216801513L;

	private static final Logger LOGGER = Logger
			.getLogger(JavaWhitespaceTokenizer.class.getName());

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.fullTokenListWithPos(code);
	}

	@Override
	public AbstractFileFilter getFileFilter() {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getFileFilter();
	}

	@Override
	public String getIdentifierType() {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getIdentifierType();
	}

	@Override
	public Collection<String> getKeywordTypes() {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getKeywordTypes();
	}

	@Override
	public Collection<String> getLiteralTypes() {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getLiteralTypes();
	}

	@Override
	public FullToken getTokenFromString(final String token) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getTokenFromString(token);
	}

	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getTokenListFromCode(code);
	}

	@Override
	public List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException {
		return getTokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	public List<WhitespaceAnnotatedToken> getTokensWithWidthData(
			final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.getTokensWithWidthData(code);
	}

	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.tokenListFromCode(code);
	}

	@Override
	public List<String> tokenListFromCode(final File codeFile)
			throws IOException {
		return tokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	@Override
	public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		return tok.tokenListWithPos(code);
	}

	@Override
	public SortedMap<Integer, FullToken> tokenListWithPos(final File file)
			throws IOException {
		return fullTokenListWithPos(FileUtils.readFileToString(file)
				.toCharArray());
	}
}
