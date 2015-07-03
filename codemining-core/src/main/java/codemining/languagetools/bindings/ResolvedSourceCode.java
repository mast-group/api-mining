/**
 *
 */
package codemining.languagetools.bindings;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;

/**
 * A full piece of source code that has the variable bindings resolved. The
 * variable bindings are "attached" to the source code, so any changes in the
 * token stream, will be reflected to the bindings.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class ResolvedSourceCode {

	public final String name;

	public final List<String> codeTokens;

	private final ArrayListMultimap<String, TokenNameBinding> variableBindings;

	/**
	 * Assumes that the variable bindings use the same (as in ==) token list.
	 *
	 * @param name
	 * @param codeTokens
	 * @param variableBindings
	 */
	public ResolvedSourceCode(final List<String> codeTokens,
			final ArrayListMultimap<String, TokenNameBinding> variableBindings) {
		this.name = "UnkownSourceCodeName";
		this.codeTokens = codeTokens;
		this.variableBindings = variableBindings;
	}

	/**
	 * Assumes that the variable bindings use the same (as in ==) token list.
	 *
	 * @param name
	 * @param codeTokens
	 * @param variableBindings
	 */
	public ResolvedSourceCode(final String name, final List<String> codeTokens,
			final ArrayListMultimap<String, TokenNameBinding> variableBindings) {
		this.name = name;
		this.codeTokens = codeTokens;
		this.variableBindings = variableBindings;
	}

	/**
	 * Return all the bindings in source code.
	 *
	 * @return
	 */
	public Collection<TokenNameBinding> getAllBindings() {
		return variableBindings.values();
	}

	/**
	 * Return the bindings for a single name.
	 *
	 * @param name
	 * @return
	 */
	public Collection<TokenNameBinding> getBindingsForName(final String name) {
		return variableBindings.get(name);
	}

	/**
	 * Rename a single bound set of tokens.
	 *
	 * @param binding
	 * @param name
	 */
	public void renameVariableTo(final TokenNameBinding binding,
			final String name) {
		checkArgument(variableBindings.values().contains(binding),
				"Binding is not pointing to this source code");

		for (final int position : binding.nameIndexes) {
			codeTokens.set(position, name);
		}
	}

}
