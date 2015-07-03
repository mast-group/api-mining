/**
 *
 */
package codemining.languagetools.bindings;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * A single name binding in source code. A struct-like class.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class TokenNameBinding implements Serializable {
	private static final long serialVersionUID = 2020613810485746430L;

	/**
	 * The tokens of source code.
	 */
	public final List<String> sourceCodeTokens;

	/**
	 * The positions in sourceCodeTokens that contain the given name.
	 */
	public final Set<Integer> nameIndexes;

	/**
	 * Features of the binding
	 */
	public final Set<String> features;

	public TokenNameBinding(final Set<Integer> nameIndexes,
			final List<String> sourceCodeTokens, final Set<String> features) {
		checkArgument(nameIndexes.size() > 0);
		checkArgument(sourceCodeTokens.size() > 0);
		this.nameIndexes = Collections.unmodifiableSet(nameIndexes);
		this.sourceCodeTokens = Collections.unmodifiableList(sourceCodeTokens);
		this.features = features;
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
		final TokenNameBinding other = (TokenNameBinding) obj;
		return Objects.equal(nameIndexes, other.nameIndexes)
				&& Objects.equal(features, other.features)
				&& Objects.equal(sourceCodeTokens, other.sourceCodeTokens);
	}

	public String getName() {
		return sourceCodeTokens.get(nameIndexes.iterator().next());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(sourceCodeTokens, nameIndexes, features);
	}

	/**
	 * Rename this name to the given binding. The source code tokens included in
	 * this struct, now represent the new structure.
	 *
	 * @param name
	 * @return
	 */
	public TokenNameBinding renameTo(final String name) {
		final List<String> renamedCode = Lists.newArrayList(sourceCodeTokens);
		for (final int position : nameIndexes) {
			renamedCode.set(position, name);
		}
		return new TokenNameBinding(nameIndexes, renamedCode, features);
	}

	@Override
	public String toString() {
		return getName() + nameIndexes + " " + features;
	}
}
