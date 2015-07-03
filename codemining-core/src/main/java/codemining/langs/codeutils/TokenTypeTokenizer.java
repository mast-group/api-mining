/**
 *
 */
package codemining.langs.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.threecrickets.jygments.ResolutionException;
import com.threecrickets.jygments.grammar.Token;

/**
 * Tokenize the code but return only the token types.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class TokenTypeTokenizer extends AbstractJygmentsTokenizer {

	private static final long serialVersionUID = 5822480321022420348L;

	/**
	 * @param fileSuffix
	 * @throws ResolutionException
	 */
	public TokenTypeTokenizer(final String fileSuffix)
			throws ResolutionException {
		super(fileSuffix);
	}

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		final Iterable<Token> tokens = lexer.getTokens(new String(code));
		final SortedMap<Integer, FullToken> tokensWithPos = Maps.newTreeMap();
		tokensWithPos.put(-1, new FullToken(SENTENCE_START, SENTENCE_START));
		tokensWithPos.put(Integer.MAX_VALUE, new FullToken(SENTENCE_END,
				SENTENCE_END));
		for (final Token tok : tokens) {
			if (isProgramToken(tok)) {
				continue;
			}
			tokensWithPos.put(tok.getPos(), new FullToken(getTokenString(tok),
					""));
		}
		return tokensWithPos;
	}

	@Override
	public String getIdentifierType() {
		throw new IllegalArgumentException("Token types may not be computed");
	}

	@Override
	public Collection<String> getKeywordTypes() {
		throw new IllegalArgumentException("Token types may not be computed");
	}

	@Override
	public Collection<String> getLiteralTypes() {
		throw new IllegalArgumentException("Token types may not be computed");
	}

	@Override
	public FullToken getTokenFromString(final String token) {
		return new FullToken(token, "");
	}

	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final Iterable<Token> tokens = lexer.getTokens(new String(code));
		final List<FullToken> toks = Lists.newArrayList();
		toks.add(new FullToken(SENTENCE_START, SENTENCE_START));
		for (final Token tok : tokens) {
			if (isProgramToken(tok)) {
				continue;
			}
			toks.add(new FullToken(tok.getType().getName(), ""));
		}
		toks.add(new FullToken(SENTENCE_END, SENTENCE_END));
		return toks;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.langs.codeutils.AbstractCodeTokenizer#getTokenString(com.
	 * threecrickets.jygments.grammar.Token)
	 */
	@Override
	public String getTokenString(final Token tok) {
		return tok.getType().getName();
	}

	@Override
	public SortedMap<Integer, FullToken> tokenListWithPos(final File file)
			throws IOException {
		return fullTokenListWithPos(FileUtils.readFileToString(file)
				.toCharArray());
	}

}
