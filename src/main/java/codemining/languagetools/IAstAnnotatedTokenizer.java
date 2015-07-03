package codemining.languagetools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;

import com.google.common.base.Function;
import com.google.common.base.Objects;

/**
 * A tokenizer interface that returns AST annotated tokens.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public interface IAstAnnotatedTokenizer extends ITokenizer {

	/**
	 * A struct class for representing AST annotated tokens.
	 */
	public static class AstAnnotatedToken implements Serializable {

		private static final long serialVersionUID = -8505721476537620929L;

		public static final Function<AstAnnotatedToken, FullToken> TOKEN_FLATTEN_FUNCTION = new Function<AstAnnotatedToken, FullToken>() {
			@Override
			public FullToken apply(final AstAnnotatedToken input) {
				if (input.tokenAstNode != null
						&& input.parentTokenAstNode != null) {
					return new FullToken(input.token.token + "->in{"
							+ input.tokenAstNode + "->"
							+ input.parentTokenAstNode + "}",
							input.token.tokenType);
				} else {
					return new FullToken(input.token);
				}
			}
		};

		public final FullToken token;
		public final String tokenAstNode;
		public final String parentTokenAstNode;

		public AstAnnotatedToken(final FullToken token,
				final String tokenAstNode, final String parentTokenAstNode) {
			this.token = token;
			this.tokenAstNode = tokenAstNode;
			this.parentTokenAstNode = parentTokenAstNode;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final AstAnnotatedToken other = (AstAnnotatedToken) obj;
			return Objects.equal(other.token, token)
					&& Objects.equal(other.tokenAstNode, tokenAstNode)
					&& Objects.equal(other.parentTokenAstNode,
							parentTokenAstNode);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(token, tokenAstNode, parentTokenAstNode);
		}

		@Override
		public String toString() {
			return TOKEN_FLATTEN_FUNCTION.apply(this).toString();
		}

	}

	public abstract List<AstAnnotatedToken> getAnnotatedTokenListFromCode(
			char[] code);

	public abstract List<AstAnnotatedToken> getAnnotatedTokenListFromCode(
			File codeFile) throws IOException;

	/**
	 * @param code
	 * @return
	 */
	public abstract SortedMap<Integer, AstAnnotatedToken> getAnnotatedTokens(
			char[] code);

	/**
	 * Return the base tokenizer whose tokens are annotated.
	 * 
	 * @return
	 */
	public ITokenizer getBaseTokenizer();

}