/**
 *
 */
package codemining.java.codeutils.binding;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.languagetools.bindings.AbstractNameBindingsExtractor;
import codemining.languagetools.bindings.ResolvedSourceCode;
import codemining.languagetools.bindings.TokenNameBinding;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A name bindings extractor interface for Java.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public abstract class AbstractJavaNameBindingsExtractor extends
		AbstractNameBindingsExtractor {

	/**
	 * Return the token index for the given position.
	 *
	 * @param sourceCode
	 * @return
	 */
	protected static SortedMap<Integer, Integer> getTokenIndexForPostion(
			final SortedMap<Integer, String> tokenPositions) {
		final SortedMap<Integer, Integer> positionToIndex = Maps.newTreeMap();
		int i = 0;
		for (final int position : tokenPositions.keySet()) {
			positionToIndex.put(position, i);
			i++;
		}
		return positionToIndex;
	}

	final ITokenizer tokenizer;

	public AbstractJavaNameBindingsExtractor(final ITokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	protected JavaASTExtractor createExtractor() {
		return new JavaASTExtractor(false);
	}

	protected abstract Set<String> getFeatures(final Set<ASTNode> boundNodes);

	/**
	 * Return a set of sets of SimpleName ASTNode objects that are bound
	 * together
	 *
	 * @param node
	 * @return
	 */
	public abstract Set<Set<ASTNode>> getNameBindings(final ASTNode node);

	public final List<TokenNameBinding> getNameBindings(final ASTNode node,
			final File file) throws IOException {
		final Set<Set<ASTNode>> nodeBindings = getNameBindings(node);
		final SortedMap<Integer, String> tokenPositions = Maps.transformValues(
				tokenizer.tokenListWithPos(file),
				FullToken.TOKEN_NAME_CONVERTER);
		return getTokenBindings(tokenPositions, nodeBindings);
	}

	/**
	 * Get the name bindings for the given ASTNode. This assumes that the
	 * ASTNode has been produced by the sourceCode, code with no variation.
	 *
	 * @param node
	 *            the ASTNode where bindings will be computed.
	 * @param sourceCode
	 *            the sourceCode from which the ASTNode has been extracted.
	 * @return
	 */
	public final List<TokenNameBinding> getNameBindings(final ASTNode node,
			final String sourceCode) {
		final Set<Set<ASTNode>> nodeBindings = getNameBindings(node);
		final SortedMap<Integer, String> tokenPositions = tokenizer
				.tokenListWithPos(sourceCode.toCharArray());
		return getTokenBindings(tokenPositions, nodeBindings);
	}

	@Override
	public List<TokenNameBinding> getNameBindings(final File f)
			throws IOException {
		final JavaASTExtractor ex = createExtractor();
		return getNameBindings(ex.getAST(f), f);
	}

	@Override
	public List<TokenNameBinding> getNameBindings(final String code) {
		final JavaASTExtractor ex = createExtractor();
		try {
			return getNameBindings(ex.getBestEffortAstNode(code), code);
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public ResolvedSourceCode getResolvedSourceCode(final File f)
			throws IOException {
		final JavaASTExtractor ex = createExtractor();
		return getResolvedSourceCode(FileUtils.readFileToString(f),
				getNameBindings(ex.getAST(f)), f.getAbsolutePath());
	}

	public ResolvedSourceCode getResolvedSourceCode(final File f,
			final Predicate<ASTNode> includeNode) throws IOException {
		final JavaASTExtractor ex = createExtractor();
		return getResolvedSourceCode(FileUtils.readFileToString(f),
				getNameBindings(ex.getAST(f)), f.getAbsolutePath(), includeNode);
	}

	@Override
	public ResolvedSourceCode getResolvedSourceCode(final String code) {
		final JavaASTExtractor ex = createExtractor();
		try {
			return getResolvedSourceCode(code,
					getNameBindings(ex.getBestEffortAstNode(code)),
					"UnkownSourceFile");
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public ResolvedSourceCode getResolvedSourceCode(final String sourceCode,
			final Set<Set<ASTNode>> nodeBindings, final String filename) {
		return getResolvedSourceCode(sourceCode, nodeBindings, filename,
				node -> true);
	}

	public ResolvedSourceCode getResolvedSourceCode(final String sourceCode,
			final Set<Set<ASTNode>> nodeBindings, final String filename,
			final Predicate<ASTNode> includeNode) {
		final SortedMap<Integer, String> tokenPositions = tokenizer
				.tokenListWithPos(sourceCode.toCharArray());
		final SortedMap<Integer, Integer> positionToIndex = getTokenIndexForPostion(tokenPositions);
		final List<String> tokens = Lists.newArrayList(tokenPositions.values());

		final ArrayListMultimap<String, TokenNameBinding> bindings = ArrayListMultimap
				.create();

		for (final Set<ASTNode> boundName : nodeBindings) {
			if (boundName.isEmpty()
					|| boundName.stream().noneMatch(includeNode)) {
				continue;
			}
			final List<Integer> boundPositions = Lists.newArrayList();
			for (final ASTNode name : boundName) {
				// Convert position to token index and add
				final int tokenIdx = positionToIndex.get(name
						.getStartPosition());
				boundPositions.add(tokenIdx);
			}
			bindings.put(tokens.get(boundPositions.get(0)),
					new TokenNameBinding(Sets.newTreeSet(boundPositions),
							tokens, getFeatures(boundName)));
		}

		return new ResolvedSourceCode(filename, tokens, bindings);
	}

	/**
	 * Get the token bindings given the ASTNode bindings and the source code
	 * positions.
	 *
	 * @param sourceCode
	 * @param nodeBindings
	 * @return
	 */
	public List<TokenNameBinding> getTokenBindings(
			final SortedMap<Integer, String> tokenPositions,
			final Set<Set<ASTNode>> nodeBindings) {
		final SortedMap<Integer, Integer> positionToIndex = getTokenIndexForPostion(tokenPositions);
		final List<String> tokens = Lists.newArrayList(tokenPositions.values());

		final List<TokenNameBinding> bindings = Lists.newArrayList();

		for (final Set<ASTNode> boundName : nodeBindings) {
			final List<Integer> boundPositions = Lists.newArrayList();
			for (final ASTNode name : boundName) {
				// Convert position to token index and add
				final int tokenIdx = positionToIndex.get(name
						.getStartPosition());
				boundPositions.add(tokenIdx);
			}
			bindings.add(new TokenNameBinding(Sets.newTreeSet(boundPositions),
					tokens, getFeatures(boundName)));
		}

		return bindings;
	}

}
