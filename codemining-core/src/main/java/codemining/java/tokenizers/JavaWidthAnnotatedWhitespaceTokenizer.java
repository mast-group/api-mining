/**
 * 
 */
package codemining.java.tokenizers;

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import codemining.util.SettingsLoader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A Java whitespace tokenizer that annotates its non-whitespace tokens with
 * width data.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavaWidthAnnotatedWhitespaceTokenizer extends
		JavaWhitespaceTokenizer {

	private static final long serialVersionUID = -3365546393414164809L;

	/**
	 * Constant for getting the size quantization of width, column of the
	 * annotation.
	 */
	public static final int SIZE_QUANTIAZATION = (int) SettingsLoader
			.getNumericSetting("sizeQuantization", 20);

	/**
	 * @param token
	 * @return
	 */
	protected String annotatedTokenToString(final WhitespaceAnnotatedToken token) {
		final int columnQ = token.column / SIZE_QUANTIAZATION;
		final int widthQ = token.width / SIZE_QUANTIAZATION;
		final String annotatedToken = token.token + "_" + columnQ + "_"
				+ widthQ;
		return annotatedToken;
	}

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		final SortedMap<Integer, WhitespaceAnnotatedToken> annotatedTokens = tok
				.tokenListWithPosAndWidth(code);
		final SortedMap<Integer, FullToken> tokens = Maps.newTreeMap();

		for (final Entry<Integer, WhitespaceAnnotatedToken> entry : annotatedTokens
				.entrySet()) {
			tokens.put(entry.getKey(), new FullToken(
					annotatedTokenToString(entry.getValue()),
					entry.getValue().tokenType));
		}
		return tokens;
	}

	@Override
	public FullToken getTokenFromString(final String token) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		final List<WhitespaceAnnotatedToken> annotatedTokens = tok
				.getTokensWithWidthData(code);
		final List<FullToken> tokens = Lists.newArrayList();
		for (final WhitespaceAnnotatedToken token : annotatedTokens) {
			if (token.token.startsWith("WS_")) {
				tokens.add(new FullToken(token.token, token.tokenType));
			} else {
				final String annotatedToken = annotatedTokenToString(token);
				tokens.add(new FullToken(annotatedToken, token.tokenType));
			}
		}
		return tokens;
	}

	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		final List<WhitespaceAnnotatedToken> annotatedTokens = tok
				.getTokensWithWidthData(code);
		final List<String> tokens = Lists.newArrayList();
		for (final WhitespaceAnnotatedToken token : annotatedTokens) {
			if (token.token.startsWith("WS_")) {
				tokens.add(token.token);
			} else {
				tokens.add(annotatedTokenToString(token));
			}
		}
		return tokens;
	}

	@Override
	public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
		final TokenizerImplementation tok = new TokenizerImplementation();
		final SortedMap<Integer, WhitespaceAnnotatedToken> annotatedTokens = tok
				.tokenListWithPosAndWidth(code);
		final SortedMap<Integer, String> tokens = Maps.newTreeMap();

		for (final Entry<Integer, WhitespaceAnnotatedToken> entry : annotatedTokens
				.entrySet()) {
			tokens.put(entry.getKey(), annotatedTokenToString(entry.getValue()));
		}
		return tokens;
	}

}
