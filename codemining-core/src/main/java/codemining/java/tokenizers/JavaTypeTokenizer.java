/**
 *
 */
package codemining.java.tokenizers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.eclipse.jdt.core.dom.ASTNode;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.java.codeutils.JavaApproximateTypeInferencer;
import codemining.languagetools.ITokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A best-effort type tokenizer. This tokenizer substitutes variable tokens with
 * their types in the special form 'var%TypeName%'
 *
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaTypeTokenizer implements ITokenizer {

	private static final long serialVersionUID = -5145031374089339996L;

	final JavaTokenizer baseTokenizer = new JavaTokenizer();

	/**
	 * @param tokens
	 * @param cu
	 * @return
	 */
	public SortedMap<Integer, FullToken> doApproximateTypeInference(
			final SortedMap<Integer, FullToken> tokens, final ASTNode cu) {
		final JavaApproximateTypeInferencer tInf = new JavaApproximateTypeInferencer(
				cu);
		tInf.infer();
		final Map<Integer, String> types = tInf.getVariableTypesAtPosition();

		final SortedMap<Integer, FullToken> typeTokenList = Maps.newTreeMap();
		for (final Entry<Integer, FullToken> token : tokens.entrySet()) {
			final String type = types.get(token.getKey());
			if (type != null) {
				typeTokenList.put(token.getKey(), new FullToken("var%" + type
						+ "%", token.getValue().tokenType));
			} else {
				typeTokenList.put(token.getKey(),
						new FullToken(token.getValue().token,
								token.getValue().tokenType));
			}
		}
		return typeTokenList;
	}

	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		final SortedMap<Integer, FullToken> tokens = baseTokenizer
				.fullTokenListWithPos(code);

		final JavaASTExtractor ex = new JavaASTExtractor(false);
		ASTNode cu;
		try {
			cu = ex.getBestEffortAstNode(code);
			return doApproximateTypeInference(tokens, cu);
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}
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
		if (token.startsWith("var%")) {
			return new FullToken(token, baseTokenizer.getIdentifierType());
		}
		// we can't get the type though, but that's not our problem...
		return baseTokenizer.getTokenFromString(token);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getTokenListFromCode(char[])
	 */
	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final SortedMap<Integer, FullToken> tokens = baseTokenizer
				.fullTokenListWithPos(code);

		final JavaASTExtractor ex = new JavaASTExtractor(false);
		ASTNode cu;
		try {
			cu = ex.getBestEffortAstNode(code);
			return getTypedTokens(tokens, cu);
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}

	}

	@Override
	public List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException {
		final SortedMap<Integer, FullToken> tokens = baseTokenizer
				.fullTokenListWithPos(FileUtils.readFileToString(codeFile)
						.toCharArray());

		final JavaASTExtractor ex = new JavaASTExtractor(false);
		final ASTNode cu = ex.getAST(codeFile);
		return getTypedTokens(tokens, cu);
	}

	/**
	 * @param tokens
	 * @param cu
	 * @return
	 */
	private List<FullToken> getTypedTokens(
			final SortedMap<Integer, FullToken> tokens, final ASTNode cu) {
		final JavaApproximateTypeInferencer tInf = new JavaApproximateTypeInferencer(
				cu);
		tInf.infer();
		final Map<Integer, String> types = tInf.getVariableTypesAtPosition();

		final List<FullToken> typeTokenList = Lists.newArrayList();
		for (final Entry<Integer, FullToken> token : tokens.entrySet()) {
			final String type = types.get(token.getKey());
			if (type != null) {
				typeTokenList.add(new FullToken("var%" + type + "%", token
						.getValue().tokenType));
			} else {
				typeTokenList.add(new FullToken(token.getValue().token, token
						.getValue().tokenType));
			}
		}
		return typeTokenList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#tokenListFromCode(char[])
	 */
	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final List<FullToken> tokens = getTokenListFromCode(code);
		final List<String> stringTokens = Lists.newArrayList();
		for (final FullToken token : tokens) {
			stringTokens.add(token.token);
		}
		return stringTokens;
	}

	@Override
	public List<String> tokenListFromCode(final File codeFile)
			throws IOException {
		final SortedMap<Integer, FullToken> tokens = baseTokenizer
				.fullTokenListWithPos(FileUtils.readFileToString(codeFile)
						.toCharArray());

		final JavaASTExtractor ex = new JavaASTExtractor(false);
		final ASTNode cu = ex.getAST(codeFile);
		final JavaApproximateTypeInferencer tInf = new JavaApproximateTypeInferencer(
				cu);
		tInf.infer();
		final Map<Integer, String> types = tInf.getVariableTypesAtPosition();

		final List<String> typeTokenList = Lists.newArrayList();
		for (final Entry<Integer, FullToken> token : tokens.entrySet()) {
			final String type = types.get(token.getKey());
			if (type != null) {
				typeTokenList.add("var%" + type + "%");
			} else {
				typeTokenList.add(token.getValue().token);
			}
		}
		return typeTokenList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#tokenListWithPos(char[])
	 */
	@Override
	public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
		return Maps.transformValues(fullTokenListWithPos(code),
				FullToken.TOKEN_NAME_CONVERTER);
	}

	@Override
	public SortedMap<Integer, FullToken> tokenListWithPos(final File f)
			throws IOException {
		final SortedMap<Integer, FullToken> tokens = baseTokenizer
				.fullTokenListWithPos(FileUtils.readFileToString(f)
						.toCharArray());

		final JavaASTExtractor ex = new JavaASTExtractor(false);
		ASTNode cu;
		try {
			cu = ex.getAST(f);
			return doApproximateTypeInference(tokens, cu);
		} catch (final Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

}
