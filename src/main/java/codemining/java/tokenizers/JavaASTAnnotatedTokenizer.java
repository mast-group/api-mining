/**
 *
 */
package codemining.java.tokenizers;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.languagetools.IAstAnnotatedTokenizer;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.ParseType;
import codemining.util.SettingsLoader;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A Java tokenizer that decorates another tokenizer's tokens with AST
 * information.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaASTAnnotatedTokenizer implements IAstAnnotatedTokenizer {

	/**
	 * Visit all AST nodes and annotate tokens.
	 *
	 */
	private class TokenDecorator extends ASTVisitor {
		final SortedMap<Integer, FullToken> baseTokens;
		final SortedMap<Integer, AstAnnotatedToken> annotatedTokens;

		public TokenDecorator(final SortedMap<Integer, FullToken> baseTokens) {
			this.baseTokens = baseTokens;
			annotatedTokens = Maps.newTreeMap();
		}

		SortedMap<Integer, AstAnnotatedToken> getAnnotatedTokens(
				final ASTNode node) {
			annotatedTokens.putAll(Maps.transformValues(baseTokens,
					new Function<FullToken, AstAnnotatedToken>() {
				@Override
				public AstAnnotatedToken apply(final FullToken input) {
					return new AstAnnotatedToken(input, NONE, NONE);
				}
			}));

			node.accept(this);
			checkArgument(baseTokens.size() == annotatedTokens.size());
			return annotatedTokens;
		}

		@Override
		public void preVisit(final ASTNode node) {
			final int fromPosition = node.getStartPosition();
			final int endPosition = fromPosition + node.getLength();
			final int nodeType = node.getNodeType();
			final int parentType;
			if (node.getParent() != null) {
				parentType = node.getParent().getNodeType();
			} else {
				parentType = -1;
			}
			final SortedMap<Integer, FullToken> nodeTokens = baseTokens.subMap(
					fromPosition, endPosition);
			for (final Entry<Integer, FullToken> token : nodeTokens.entrySet()) {
				if (token.getValue().token.startsWith("WS_")
						&& baseTokenizer instanceof JavaWhitespaceTokenizer) {
					annotatedTokens.put(
							token.getKey(),
							new AstAnnotatedToken(new FullToken(token
									.getValue().token,
									token.getValue().tokenType), null, null));
				} else {
					annotatedTokens.put(
							token.getKey(),
							new AstAnnotatedToken(new FullToken(token
									.getValue().token,
									token.getValue().tokenType),
									nodeIdToString(nodeType),
									nodeIdToString(parentType)));
				}
			}
			super.preVisit(node);
		}
	}

	public static final String NONE = "NONE";

	private static final Logger LOGGER = Logger
			.getLogger(JavaASTAnnotatedTokenizer.class.getName());

	private static final long serialVersionUID = -4518140661119781220L;

	private final ITokenizer baseTokenizer;

	public JavaASTAnnotatedTokenizer() {
		try {
			final Class<? extends ITokenizer> tokenizerClass = (Class<? extends ITokenizer>) Class
					.forName(SettingsLoader.getStringSetting("baseTokenizer",
							JavaTokenizer.class.getName()));
			baseTokenizer = tokenizerClass.newInstance();
		} catch (final ClassNotFoundException e) {
			LOGGER.severe(ExceptionUtils.getFullStackTrace(e));
			throw new IllegalArgumentException(e);
		} catch (final InstantiationException e) {
			LOGGER.severe(ExceptionUtils.getFullStackTrace(e));
			throw new IllegalArgumentException(e);
		} catch (final IllegalAccessException e) {
			LOGGER.severe(ExceptionUtils.getFullStackTrace(e));
			throw new IllegalArgumentException(e);
		}
	}

	public JavaASTAnnotatedTokenizer(final ITokenizer base) {
		baseTokenizer = base;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#fullTokenListWithPos(char[])
	 */
	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		final SortedMap<Integer, AstAnnotatedToken> annotatedTokens = getAnnotatedTokens(code);
		return Maps.transformValues(annotatedTokens,
				AstAnnotatedToken.TOKEN_FLATTEN_FUNCTION);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.java.tokenizers.IAstAnnotatedTokenizer#
	 * getAnnotatedTokenListFromCode(char[])
	 */
	@Override
	public List<AstAnnotatedToken> getAnnotatedTokenListFromCode(
			final char[] code) {
		final List<AstAnnotatedToken> tokens = Lists.newArrayList();
		for (final Entry<Integer, AstAnnotatedToken> token : getAnnotatedTokens(
				code).entrySet()) {
			tokens.add(token.getValue());
		}
		return tokens;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.java.tokenizers.IAstAnnotatedTokenizer#
	 * getAnnotatedTokenListFromCode(java.io.File)
	 */
	@Override
	public List<AstAnnotatedToken> getAnnotatedTokenListFromCode(
			final File codeFile) throws IOException {
		// TODO Get ast through the file
		return getAnnotatedTokenListFromCode(FileUtils.readFileToString(
				codeFile).toCharArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * codemining.java.tokenizers.IAstAnnotatedTokenizer#getAnnotatedTokens(
	 * char[])
	 */
	@Override
	public SortedMap<Integer, AstAnnotatedToken> getAnnotatedTokens(
			final char[] code) {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		final ASTNode cu = ex.getASTNode(code, ParseType.COMPILATION_UNIT);

		final SortedMap<Integer, FullToken> baseTokens = baseTokenizer
				.fullTokenListWithPos(code);
		final TokenDecorator dec = new TokenDecorator(baseTokens);
		final SortedMap<Integer, AstAnnotatedToken> annotatedTokens = dec
				.getAnnotatedTokens(cu);
		return annotatedTokens;
	}

	@Override
	public ITokenizer getBaseTokenizer() {
		return baseTokenizer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getFileFilter()
	 */
	@Override
	public AbstractFileFilter getFileFilter() {
		return baseTokenizer.getFileFilter();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getIdentifierType()
	 */
	@Override
	public String getIdentifierType() {
		return baseTokenizer.getIdentifierType();
	}

	@Override
	public Collection<String> getKeywordTypes() {
		return baseTokenizer.getKeywordTypes();
	}

	@Override
	public Collection<String> getLiteralTypes() {
		return baseTokenizer.getLiteralTypes();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * codemining.languagetools.ITokenizer#getTokenFromString(java.lang.String)
	 */
	@Override
	public FullToken getTokenFromString(final String token) {
		throw new IllegalArgumentException(
				"ASTAnnotatedTokenizer cannot return a token from a single string.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getTokenListFromCode(char[])
	 */
	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final List<FullToken> tokens = Lists.newArrayList();
		for (final Entry<Integer, FullToken> token : fullTokenListWithPos(code)
				.entrySet()) {
			tokens.add(token.getValue());
		}
		return tokens;
	}

	@Override
	public List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException {
		// TODO Get ast through the file
		return getTokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	/**
	 * Convert the numeric id of a node to its textual representation.
	 *
	 * @param id
	 * @return
	 */
	private final String nodeIdToString(final int id) {
		if (id == -1) {
			return "NONE";
		} else {
			return ASTNode.nodeClassForType(id).getSimpleName();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#tokenListFromCode(char[])
	 */
	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final List<String> tokens = Lists.newArrayList();
		for (final Entry<Integer, FullToken> token : fullTokenListWithPos(code)
				.entrySet()) {
			tokens.add(token.getValue().token);
		}
		return tokens;
	}

	@Override
	public List<String> tokenListFromCode(final File codeFile)
			throws IOException {
		// TODO Get the ast directly from the file.
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
		for (final Entry<Integer, FullToken> token : fullTokenListWithPos(code)
				.entrySet()) {
			tokens.put(token.getKey(), token.getValue().token);
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
