/**
 * 
 */
package codemining.java.tokenizers;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.core.util.PublicScanner;

import codemining.java.codeutils.IdentifierPerType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

/**
 * A Java tokenizer that annotates the type of the identifier tokens.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavaIdentifierAnnotatedTokenizer extends JavaTokenizer {

	private static class IdentifierTypeRetriever {

		final Map<String, RangeSet<Integer>> variables;
		final Map<String, RangeSet<Integer>> methods;
		final Map<String, RangeSet<Integer>> types;

		public IdentifierTypeRetriever(final char[] code) throws Exception {
			variables = IdentifierPerType.getVariableIdentifiersRanges(code);
			methods = IdentifierPerType.getMethodIdentifiersRanges(code);
			types = IdentifierPerType.getTypeIdentifiersRanges(code);
		}

		public String getIdentifierType(final PublicScanner scanner) {
			final int startPos = scanner.getCurrentTokenStartPosition();
			final int endPos = scanner.getCurrentTokenEndPosition();
			final Range<Integer> tokenRange = Range
					.closedOpen(startPos, endPos);

			final String tokenName = scanner.getCurrentTokenString();

			// TODO: Find the tightest of all
			if (isInSet(tokenName, tokenRange, variables)) {
				return IDENTIFIER_PREFIX + "_VAR";
			} else if (isInSet(tokenName, tokenRange, methods)) {
				return IDENTIFIER_PREFIX + "_METHOD";
			} else if (isInSet(tokenName, tokenRange, types)) {
				return IDENTIFIER_PREFIX + "_TYPE";
			}
			return IDENTIFIER_PREFIX + "_UNK";
		}

		private boolean isInSet(final String token,
				final Range<Integer> tokenRange,
				final Map<String, RangeSet<Integer>> set) {
			if (!set.containsKey(token)) {
				return false;
			}
			// TODO: Check if in scope
			return true;
		}
	}

	private static final long serialVersionUID = -4779695380807928575L;

	private static final Logger LOGGER = Logger
			.getLogger(JavaIdentifierAnnotatedTokenizer.class.getName());

	public static final String IDENTIFIER_PREFIX = "IDENTIFIER";

	public static final String LITERAL = "LITERAL";

	public JavaIdentifierAnnotatedTokenizer() {
		super();
	}

	public JavaIdentifierAnnotatedTokenizer(final boolean tokenizeComments) {
		super(tokenizeComments);
	}

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		IdentifierTypeRetriever idRetriever;
		try {
			idRetriever = new IdentifierTypeRetriever(code);
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}

		final PublicScanner scanner = prepareScanner();
		final SortedMap<Integer, FullToken> tokens = Maps.newTreeMap();
		tokens.put(-1, new FullToken(SENTENCE_START, SENTENCE_START));
		tokens.put(Integer.MAX_VALUE, new FullToken(SENTENCE_END, SENTENCE_END));
		scanner.setSource(code);
		while (!scanner.atEnd()) {
			do {
				try {
					final int token = scanner.getNextToken();
					if (token == ITerminalSymbols.TokenNameEOF) {
						break;
					}
					final String nxtToken = transformToken(token,
							scanner.getCurrentTokenString());
					final String tokenType = getTokenType(token, scanner,
							idRetriever);

					final int position = scanner.getCurrentTokenStartPosition();
					tokens.put(position, new FullToken(nxtToken, tokenType));
				} catch (final InvalidInputException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
			} while (!scanner.atEnd());

		}
		return tokens;
	}

	@Override
	public String getIdentifierType() {
		throw new UnsupportedOperationException(
				"There is no single indentifier type for this tokenizer.");
	}

	@Override
	public FullToken getTokenFromString(final String token) {
		throw new UnsupportedOperationException(
				"Cannot compute token from just a string using this tokenizer.");
	}

	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		IdentifierTypeRetriever idRetriever;
		try {
			idRetriever = new IdentifierTypeRetriever(code);
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}

		final List<FullToken> tokens = Lists.newArrayList();
		tokens.add(new FullToken(SENTENCE_START, SENTENCE_START));
		final PublicScanner scanner = prepareScanner();
		scanner.setSource(code);
		do {
			try {
				final int token = scanner.getNextToken();
				if (token == ITerminalSymbols.TokenNameEOF) {
					break;
				}
				final String nxtToken = transformToken(token,
						scanner.getCurrentTokenString());

				final String tokenType = getTokenType(token, scanner,
						idRetriever);
				tokens.add(new FullToken(stripTokenIfNeeded(nxtToken),
						tokenType));
			} catch (final InvalidInputException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			} catch (final StringIndexOutOfBoundsException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		} while (!scanner.atEnd());
		tokens.add(new FullToken(SENTENCE_END, SENTENCE_END));
		return tokens;
	}

	private final String getTokenType(final int tokenType,
			final PublicScanner scanner, final IdentifierTypeRetriever retriever) {
		if (tokenType == ITerminalSymbols.TokenNameIdentifier) {
			return retriever.getIdentifierType(scanner);
		} else if (JavaTokenTypeTokenizer.isLiteralToken(tokenType)) {
			return LITERAL;
		} else {
			return scanner.getCurrentTokenString();
		}
	}
}
