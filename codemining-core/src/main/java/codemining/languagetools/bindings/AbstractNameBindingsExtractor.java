/**
 *
 */
package codemining.languagetools.bindings;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A NameBindings extractor from arbitrary code.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public abstract class AbstractNameBindingsExtractor {

	public abstract Set<?> getAvailableFeatures();

	/**
	 * Return all the name bindings for file f
	 *
	 * @param f
	 * @return a multimap containing for each name all the relavant name
	 *         bindings in the file.
	 * @throws IOException
	 */
	public Multimap<String, TokenNameBinding> getBindingsForName(final File f)
			throws IOException {
		return getBindingsForName(getNameBindings(f));
	}

	protected Multimap<String, TokenNameBinding> getBindingsForName(
			final List<TokenNameBinding> bindings) {
		final Multimap<String, TokenNameBinding> toks = HashMultimap.create();
		for (final TokenNameBinding binding : bindings) {
			toks.put(binding.getName(), binding);
		}
		return toks;
	}

	/**
	 * Return the name bindings given the code.
	 *
	 * @param code
	 * @return a multimap containing for each name all the relavant name
	 *         bindings in the code snippet.
	 */
	public Multimap<String, TokenNameBinding> getBindingsForName(
			final String code) {
		return getBindingsForName(getNameBindings(code));
	}

	/**
	 * Get the name bindings for the given file.
	 *
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public abstract List<TokenNameBinding> getNameBindings(final File f)
			throws IOException;

	/**
	 * Get the name bindings given the code.
	 *
	 * @param code
	 * @return
	 */
	public abstract List<TokenNameBinding> getNameBindings(final String code);

	/**
	 * Return a ResolvedSourceCode instance for the given code.
	 *
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public abstract ResolvedSourceCode getResolvedSourceCode(final File f)
			throws IOException;

	/**
	 * Return a ResolvedSourceCode instance for the given code.
	 *
	 * @param code
	 * @return
	 */
	public abstract ResolvedSourceCode getResolvedSourceCode(final String code);

	public abstract void setActiveFeatures(Set<?> activeFeatures);
}
