/**
 * 
 */
package codemining.langs.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import codemining.languagetools.ITokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.threecrickets.jygments.ResolutionException;
import com.threecrickets.jygments.grammar.Lexer;
import com.threecrickets.jygments.grammar.Token;
import com.threecrickets.jygments.grammar.TokenType;

/**
 * Tokenize all languages
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public abstract class AbstractJygmentsTokenizer implements ITokenizer {

	final Lexer lexer;

	private final RegexFileFilter codeFilter;

	private static final long serialVersionUID = 8826779180772076954L;

	public AbstractJygmentsTokenizer(final String fileSuffix)
			throws ResolutionException {
		lexer = Lexer.getForFileName("sample." + fileSuffix);
		// lexer.setStripAll(true);
		// lexer.setStripNewLines(true);
		// lexer.setTabSize(1);
		codeFilter = new RegexFileFilter(".*\\." + fileSuffix + "$");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.languagetools.ITokenizer#getFileFilter()
	 */
	@Override
	public AbstractFileFilter getFileFilter() {
		return codeFilter;
	}

	@Override
	public List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException {
		return getTokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	public abstract String getTokenString(final Token tok);

	/**
	 * @param tok
	 * @return
	 */
	protected boolean isProgramToken(final Token tok) {
		final TokenType tokenType = tok.getType();
		return tokenType == TokenType.Comment
				|| tokenType == TokenType.Comment_Multiline
				|| tokenType == TokenType.Comment_Single
				|| tokenType == TokenType.Comment_Special
				|| tokenType == TokenType.Comment_Preproc
				|| tokenType == TokenType.Text || tok.getValue().equals(" ")
				|| tok.getValue().equals("\n") || tok.getValue().equals("\t");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.languagetools.ITokenizer#tokenListFromCode(char[])
	 */
	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final Iterable<Token> tokens = lexer.getTokens(new String(code));
		final List<String> toks = Lists.newArrayList();
		toks.add(SENTENCE_START);
		for (final Token tok : tokens) {
			if (isProgramToken(tok)) {
				continue;
			}
			toks.add(getTokenString(tok));
		}
		toks.add(SENTENCE_END);
		return toks;
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
		final Iterable<Token> tokens = lexer.getTokens(new String(code));
		final SortedMap<Integer, String> tokensWithPos = Maps.newTreeMap();
		tokensWithPos.put(-1, SENTENCE_START);
		tokensWithPos.put(Integer.MAX_VALUE, SENTENCE_END);
		for (final Token tok : tokens) {
			if (isProgramToken(tok)) {
				continue;
			}
			tokensWithPos.put(tok.getPos(), getTokenString(tok));
		}
		return tokensWithPos;
	}

}
