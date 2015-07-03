package codemining.languagetools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.filefilter.AbstractFileFilter;

import com.google.common.base.Function;
import com.google.common.base.Objects;

/**
 * Interface of a code tokenizer.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public interface ITokenizer extends Serializable {

	public static class FullToken implements Serializable {

		private static final long serialVersionUID = -49456240173307314L;

		public static final Function<FullToken, String> TOKEN_NAME_CONVERTER = new Function<FullToken, String>() {
			@Override
			public String apply(final FullToken input) {
				return input.token;
			}
		};

		public final String token;

		public final String tokenType;

		public FullToken(final FullToken other) {
			token = other.token;
			tokenType = other.tokenType;
		}

		public FullToken(final String tokName, final String tokType) {
			token = tokName;
			tokenType = tokType;
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof FullToken)) {
				return false;
			}
			final FullToken other = (FullToken) obj;
			return other.token.equals(token)
					&& other.tokenType.equals(tokenType);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(token, tokenType);
		}

		@Override
		public String toString() {
			return token + " (" + tokenType + ")";
		}

	}

	/**
	 * A sentence end (constant) token
	 */
	static final String SENTENCE_END = "<SENTENCE_END/>";

	/**
	 * A sentence start (constant) token
	 */
	static final String SENTENCE_START = "<SENTENCE_START>";

	/**
	 * Return a list with the full tokens.
	 *
	 * @param code
	 * @return
	 */
	SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code);

	/**
	 * Return a file filter, filtering the files that can be tokenized.
	 *
	 * @return
	 *
	 */
	AbstractFileFilter getFileFilter();

	/**
	 * Return the token type that signifies that a token is an identifier.
	 *
	 * @return
	 */
	String getIdentifierType();

	/**
	 * Return the token types that are keywords.
	 * 
	 * @return
	 */
	Collection<String> getKeywordTypes();

	/**
	 * Return the types the represent literals.
	 *
	 * @return
	 */
	Collection<String> getLiteralTypes();

	/**
	 * Return a full token given a string token.
	 *
	 * @param token
	 * @return
	 */
	FullToken getTokenFromString(final String token);

	/**
	 * Get the list of tokens from the code.
	 *
	 * @param code
	 * @return
	 */
	List<FullToken> getTokenListFromCode(final char[] code);

	/**
	 * Get the list of tokens from the code.
	 *
	 * @param code
	 * @return
	 */
	List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException;

	/**
	 * Tokenize some code.
	 *
	 * @param code
	 *            the code
	 * @return a list of tokens
	 */
	List<String> tokenListFromCode(final char[] code);

	/**
	 * Tokenize code given a file.
	 *
	 * @param codeFile
	 * @return
	 */
	List<String> tokenListFromCode(final File codeFile) throws IOException;

	/**
	 * Return a list of tokens along with their positions.
	 *
	 * @param code
	 * @return
	 */
	SortedMap<Integer, String> tokenListWithPos(final char[] code);

	/**
	 * Return a list of tokens along with their positions.
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	SortedMap<Integer, FullToken> tokenListWithPos(File file)
			throws IOException;

}