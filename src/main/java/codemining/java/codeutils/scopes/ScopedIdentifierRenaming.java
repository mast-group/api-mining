/**
 * 
 */
package codemining.java.codeutils.scopes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.IScopeExtractor;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.ParseType;
import codemining.languagetools.Scope;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Rename an identifer given a scope.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class ScopedIdentifierRenaming {

	final IScopeExtractor scopeExtractor;

	final ITokenizer tokenizer = new JavaTokenizer();

	final ParseType parseKindToUseOnOriginal;

	public ScopedIdentifierRenaming(final IScopeExtractor scopeExtractor,
			final ParseType parseType) {
		this.scopeExtractor = scopeExtractor;
		parseKindToUseOnOriginal = parseType;
	}

	public String getFormattedRenamedCode(final String originalScopeCode,
			final String from, final String to, final String wholeFile) {
		final String code = getRenamedCode(originalScopeCode, from, to,
				wholeFile);
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return ex.getASTNode(code, parseKindToUseOnOriginal).toString();
	}

	/**
	 * @param originalScopeCode
	 * @param from
	 * @param to
	 * @param wholeFile
	 * @return
	 */
	public String getRenamedCode(final String originalScopeCode,
			final String from, final String to, final String wholeFile) {
		final Map<String, String> varMapping = Maps.newTreeMap();
		varMapping.put(from, to);
		return getRenamedCode(originalScopeCode, wholeFile, varMapping);
	}

	/**
	 * @param originalScopeCode
	 * @param wholeFile
	 * @param varMapping
	 * @return
	 */
	public String getRenamedCode(final String originalScopeCode,
			final String wholeFile, final Map<String, String> varMapping) {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		final String originalCode = renameVariableInSnippet(
				ex.getASTNode(wholeFile, parseKindToUseOnOriginal).toString(),
				Collections.EMPTY_MAP);
		final String snippetToBeReplaced = renameVariableInSnippet(
				originalScopeCode, varMapping);

		final String code = originalCode.replace(
				renameVariableInSnippet(originalScopeCode,
						Collections.EMPTY_MAP), snippetToBeReplaced);
		return code;
	}

	public Multimap<Scope, String> getRenamedScopes(final Scope originalScope,
			final String from, final String to, final String wholeFile) {
		return getRenamedScopes(originalScope.code, from, to, wholeFile);
	}

	public Multimap<Scope, String> getRenamedScopes(
			final String originalScopeCode, final String from, final String to,
			final String wholeFile) {
		final String code = getRenamedCode(originalScopeCode, from, to,
				wholeFile);
		return scopeExtractor.getFromString(code, parseKindToUseOnOriginal);
	}

	/**
	 * Crudely rename the name of an identifier by searching for similarly named
	 * tokens.
	 * 
	 * @param snippet
	 * @param variableMapping
	 *            from, to
	 * @return
	 */
	private String renameVariableInSnippet(final String snippet,
			final Map<String, String> variableMapping) {
		final List<String> tokens = tokenizer.tokenListFromCode(snippet
				.toCharArray());

		final StringBuffer bf = new StringBuffer();
		for (final String token : tokens) {
			if (variableMapping.containsKey(token)) {
				bf.append(variableMapping.get(token));
			} else if (token.equals(ITokenizer.SENTENCE_START)
					|| token.equals(ITokenizer.SENTENCE_END)) {
				continue;
			} else {
				bf.append(token);
			}
			bf.append(" ");
		}
		return bf.toString();

	}

}
