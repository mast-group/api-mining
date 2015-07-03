package codemining.java.codeutils.scopes;

import java.io.File;
import java.io.IOException;

import codemining.java.codeutils.scopes.AllScopeExtractor.AllScopeSnippetExtractor;
import codemining.languagetools.IScopeExtractor;

public class ScopesTUI {

	/**
	 * @param name
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public static IScopeExtractor getScopeExtractorByName(final String name)
			throws UnsupportedOperationException {
		final IScopeExtractor scopeExtractor;
		if (name.equals("variable")) {
			scopeExtractor = new VariableScopeExtractor.VariableScopeSnippetExtractor();
		} else if (name.equals("method")) {
			scopeExtractor = new MethodScopeExtractor.MethodScopeSnippetExtractor(
					true);
		} else if (name.equals("type")) {
			scopeExtractor = new TypenameScopeExtractor.TypenameSnippetExtractor(
					true);
		} else if (name.equals("all")) {
			scopeExtractor = new AllScopeSnippetExtractor();
		} else {
			throw new UnsupportedOperationException(
					"Unknown type of identifier.");
		}
		return scopeExtractor;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage <file> all|variable|method|type");
			return;
		}
		final String name = args[1];
		final IScopeExtractor scopeExtractor = getScopeExtractorByName(name);

		System.out.println(scopeExtractor.getFromFile(new File(args[0])));

	}
}
