/**
 * 
 */
package codemining.java.codeutils.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Set;

import codemining.languagetools.bindings.TokenNameBinding;

import com.google.common.collect.Sets;

/**
 * Utility class for testing bindings.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class BindingTester {

	private BindingTester() {
	}

	public static void checkAllBindings(final List<TokenNameBinding> bindings) {
		final Set<Integer> indexes = Sets.newHashSet();
		for (final TokenNameBinding binding : bindings) {
			BindingTester.checkBinding(binding);
			assertFalse("Indexes appear only once",
					indexes.removeAll(binding.nameIndexes));
			indexes.addAll(binding.nameIndexes);
		}
	}

	public static void checkBinding(final TokenNameBinding binding) {
		final String tokenName = binding.sourceCodeTokens
				.get(binding.nameIndexes.iterator().next());
		for (final int idx : binding.nameIndexes) {
			assertEquals(tokenName, binding.sourceCodeTokens.get(idx));
		}
	};
}
