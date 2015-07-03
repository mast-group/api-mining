package codemining.java.tokenizers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
 * A Java Code tokenizer using Eclipse JDT.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaTokenizer implements ITokenizer {

	private static final long serialVersionUID = 505587999946057082L;
	private static final Logger LOGGER = Logger.getLogger(JavaTokenizer.class
			.getName());

	/**
	 * A filter for the files being tokenized.
	 */
	public static final RegexFileFilter javaCodeFileFilter = new RegexFileFilter(
			".*\\.java$");

	private final boolean tokenizeComments;

	public static final String IDENTIFIER_ID = Integer
			.toString(ITerminalSymbols.TokenNameIdentifier);
	public static final String[] KEYWORD_TYPE_IDs = new String[] {
		Integer.toString(ITerminalSymbols.TokenNameboolean),
		Integer.toString(ITerminalSymbols.TokenNamebyte),
		Integer.toString(ITerminalSymbols.TokenNamechar),
		Integer.toString(ITerminalSymbols.TokenNamedouble),
		Integer.toString(ITerminalSymbols.TokenNamefloat),
		Integer.toString(ITerminalSymbols.TokenNameint),
		Integer.toString(ITerminalSymbols.TokenNamelong),
		Integer.toString(ITerminalSymbols.TokenNameshort),
		Integer.toString(ITerminalSymbols.TokenNamevoid) };
	public static final String[] STRING_LITERAL_IDs = new String[] {
		Integer.toString(ITerminalSymbols.TokenNameStringLiteral),
		Integer.toString(ITerminalSymbols.TokenNameCharacterLiteral) };
	public static final String[] NUMBER_LITERAL_IDs = new String[] {
		Integer.toString(ITerminalSymbols.TokenNameDoubleLiteral),
		Integer.toString(ITerminalSymbols.TokenNameFloatingPointLiteral),
		Integer.toString(ITerminalSymbols.TokenNameIntegerLiteral),
		Integer.toString(ITerminalSymbols.TokenNameLongLiteral) };
	public static final String[] COMMENT_IDs = new String[] {
		Integer.toString(ITerminalSymbols.TokenNameCOMMENT_BLOCK),
		Integer.toString(ITerminalSymbols.TokenNameCOMMENT_JAVADOC),
		Integer.toString(ITerminalSymbols.TokenNameCOMMENT_LINE) };
	public static final String[] OPERATOR_IDs = new String[] {
		Integer.toString(ITerminalSymbols.TokenNameAND),
		Integer.toString(ITerminalSymbols.TokenNameAND_AND),
		Integer.toString(ITerminalSymbols.TokenNameAND_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameCOLON),
		Integer.toString(ITerminalSymbols.TokenNameCOMMA),
		Integer.toString(ITerminalSymbols.TokenNameDIVIDE),
		Integer.toString(ITerminalSymbols.TokenNameDIVIDE_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameDOT),
		Integer.toString(ITerminalSymbols.TokenNameELLIPSIS),
		Integer.toString(ITerminalSymbols.TokenNameEQUAL),
		Integer.toString(ITerminalSymbols.TokenNameEQUAL_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameGREATER),
		Integer.toString(ITerminalSymbols.TokenNameGREATER_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameLBRACKET),
		Integer.toString(ITerminalSymbols.TokenNameLEFT_SHIFT),
		Integer.toString(ITerminalSymbols.TokenNameLEFT_SHIFT_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameLESS),
		Integer.toString(ITerminalSymbols.TokenNameLESS_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameLPAREN),
		Integer.toString(ITerminalSymbols.TokenNameMINUS),
		Integer.toString(ITerminalSymbols.TokenNameMINUS_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameMINUS_MINUS),
		Integer.toString(ITerminalSymbols.TokenNameMULTIPLY),
		Integer.toString(ITerminalSymbols.TokenNameMULTIPLY_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameNOT),
		Integer.toString(ITerminalSymbols.TokenNameNOT_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameOR),
		Integer.toString(ITerminalSymbols.TokenNameOR_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameOR_OR),
		Integer.toString(ITerminalSymbols.TokenNamePLUS),
		Integer.toString(ITerminalSymbols.TokenNamePLUS_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNamePLUS_PLUS),
		Integer.toString(ITerminalSymbols.TokenNameQUESTION),
		Integer.toString(ITerminalSymbols.TokenNameRBRACKET),
		Integer.toString(ITerminalSymbols.TokenNameREMAINDER),
		Integer.toString(ITerminalSymbols.TokenNameREMAINDER_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameRIGHT_SHIFT),
		Integer.toString(ITerminalSymbols.TokenNameRIGHT_SHIFT_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameRPAREN),
		Integer.toString(ITerminalSymbols.TokenNameSEMICOLON),
		Integer.toString(ITerminalSymbols.TokenNameTWIDDLE),
		Integer.toString(ITerminalSymbols.TokenNameUNSIGNED_RIGHT_SHIFT),
		Integer.toString(ITerminalSymbols.TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL),
		Integer.toString(ITerminalSymbols.TokenNameXOR),
		Integer.toString(ITerminalSymbols.TokenNameXOR_EQUAL) };
	public static final String[] BRACE_IDs = new String[] {
		Integer.toString(ITerminalSymbols.TokenNameLBRACE),
		Integer.toString(ITerminalSymbols.TokenNameRBRACE), };
	public static final String[] SYNTAX_IDs = {
		Integer.toString(ITerminalSymbols.TokenNameCOMMA),
		Integer.toString(ITerminalSymbols.TokenNameDOT),
		Integer.toString(ITerminalSymbols.TokenNameELLIPSIS),
		Integer.toString(ITerminalSymbols.TokenNameSEMICOLON),
		Integer.toString(ITerminalSymbols.TokenNameLBRACE),
		Integer.toString(ITerminalSymbols.TokenNameRBRACE),
		Integer.toString(ITerminalSymbols.TokenNameLPAREN),
		Integer.toString(ITerminalSymbols.TokenNameRPAREN),
		Integer.toString(ITerminalSymbols.TokenNameLBRACKET),
		Integer.toString(ITerminalSymbols.TokenNameRBRACKET) };

	public JavaTokenizer() {
		tokenizeComments = false;
	}

	public JavaTokenizer(final boolean tokenizeComments) {
		this.tokenizeComments = tokenizeComments;
	}

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		// TODO Duplicate Code
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
					final int position = scanner.getCurrentTokenStartPosition();
					tokens.put(position,
							new FullToken(nxtToken, Integer.toString(token)));
				} catch (final InvalidInputException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
			} while (!scanner.atEnd());

		}
		return tokens;
	}

	@Override
	public AbstractFileFilter getFileFilter() {
		return javaCodeFileFilter;
	}

	@Override
	public String getIdentifierType() {
		return IDENTIFIER_ID;
	}

	@Override
	public Collection<String> getKeywordTypes() {
		return Arrays.asList(KEYWORD_TYPE_IDs);
	}

	@Override
	public Collection<String> getLiteralTypes() {
		final List<String> allLiterals = Lists.newArrayList(Arrays
				.asList(NUMBER_LITERAL_IDs));
		allLiterals.addAll(Arrays.asList(STRING_LITERAL_IDs));
		return allLiterals;
	}

	@Override
	public FullToken getTokenFromString(final String token) {
		if (token.equals(ITokenizer.SENTENCE_START)) {
			return new FullToken(ITokenizer.SENTENCE_START,
					ITokenizer.SENTENCE_START);
		}

		if (token.equals(ITokenizer.SENTENCE_END)) {
			return new FullToken(ITokenizer.SENTENCE_END,
					ITokenizer.SENTENCE_END);
		}
		return getTokenListFromCode(token.toCharArray()).get(1);
	}

	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
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

				tokens.add(new FullToken(stripTokenIfNeeded(nxtToken), Integer
						.toString(token)));
			} catch (final InvalidInputException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			} catch (final StringIndexOutOfBoundsException e) {
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

	/**
	 * Create the scanner.
	 *
	 * @return
	 */
	protected PublicScanner prepareScanner() {
		final PublicScanner scanner = new PublicScanner();
		scanner.tokenizeComments = tokenizeComments;
		return scanner;
	}

	/**
	 * @param token
	 * @return
	 */
	protected String stripTokenIfNeeded(final String token) {
		return token.replace('\n', ' ').replace('\t', ' ').replace('\r', ' ')
				.replace("\n", " ").replace("\t", " ").replace("\r", " ")
				.replace("\'\\\\\'", "\'|\'").replace("\\", "|");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see uk.ac.ed.inf.javacodeutils.ITokenizer#tokenListFromCode(char[])
	 */
	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final PublicScanner scanner = prepareScanner();
		final List<String> tokens = Lists.newArrayList();
		tokens.add(SENTENCE_START);
		scanner.setSource(code);
		do {
			try {
				final int token = scanner.getNextToken();
				if (token == ITerminalSymbols.TokenNameEOF) {
					break;
				}
				final String nxtToken = transformToken(token,
						scanner.getCurrentTokenString());

				tokens.add(stripTokenIfNeeded(nxtToken));
			} catch (final InvalidInputException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			} catch (final StringIndexOutOfBoundsException e) {
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
		final PublicScanner scanner = prepareScanner();
		final SortedMap<Integer, String> tokens = Maps.newTreeMap();
		tokens.put(-1, SENTENCE_START);
		tokens.put(Integer.MAX_VALUE, SENTENCE_END);
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
					final int position = scanner.getCurrentTokenStartPosition();
					tokens.put(position, stripTokenIfNeeded(nxtToken));
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

	/**
	 * Function used to transform the tokens. Useful when overriding some tokens
	 * in subclasses.
	 *
	 * @param tokenType
	 * @param token
	 * @return
	 */
	protected String transformToken(final int tokenType, final String token) {
		return token;
	}

}
