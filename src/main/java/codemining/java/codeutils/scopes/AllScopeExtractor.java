/**
 * 
 */
package codemining.java.codeutils.scopes;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import codemining.languagetools.IScopeExtractor;
import codemining.languagetools.ParseType;
import codemining.languagetools.Scope;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Aggregate all extractors.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class AllScopeExtractor {

	public static final class AllScopeSnippetExtractor implements
			IScopeExtractor {

		public AllScopeSnippetExtractor() {
			allExtractors = Lists.newArrayList();
			allExtractors
					.add(new VariableScopeExtractor.VariableScopeSnippetExtractor());
			allExtractors
					.add(new MethodScopeExtractor.MethodScopeSnippetExtractor(
							true));
			allExtractors
					.add(new TypenameScopeExtractor.TypenameSnippetExtractor(
							true));
		}

		public AllScopeSnippetExtractor(final boolean variables,
				final boolean methods, final boolean types) {
			allExtractors = Lists.newArrayList();
			checkArgument(variables | methods | types,
					"At least one option must be set");
			if (variables) {
				allExtractors
						.add(new VariableScopeExtractor.VariableScopeSnippetExtractor());
			}
			if (methods) {
				allExtractors
						.add(new MethodScopeExtractor.MethodScopeSnippetExtractor(
								true));
			}
			if (types) {
				allExtractors
						.add(new TypenameScopeExtractor.TypenameSnippetExtractor(
								true));
			}
		}

		private final List<IScopeExtractor> allExtractors;

		@Override
		public Multimap<Scope, String> getFromFile(final File file)
				throws IOException {
			final Multimap<Scope, String> scopes = TreeMultimap.create();
			for (final IScopeExtractor extractor : allExtractors) {
				scopes.putAll(extractor.getFromFile(file));
			}
			return scopes;
		}

		@Override
		public Multimap<Scope, String> getFromNode(ASTNode node) {
			final Multimap<Scope, String> scopes = TreeMultimap.create();
			for (final IScopeExtractor extractor : allExtractors) {
				scopes.putAll(extractor.getFromNode(node));
			}
			return scopes;
		}

		@Override
		public Multimap<Scope, String> getFromString(final String file,
				final ParseType parseType) {
			final Multimap<Scope, String> scopes = TreeMultimap.create();
			for (final IScopeExtractor extractor : allExtractors) {
				scopes.putAll(extractor.getFromString(file, parseType));
			}
			return scopes;
		}
	}

	/**
	 * 
	 */
	private AllScopeExtractor() {
	}

}
