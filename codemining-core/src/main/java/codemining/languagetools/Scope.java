package codemining.languagetools;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * A utility class to represent scopes.
 * 
 */
public class Scope implements Comparable<Scope> {

	public enum ScopeType {
		SCOPE_CLASS, SCOPE_LOCAL, SCOPE_METHOD
	}

	public final String code;

	public final ScopeType scopeType;

	public final String type;

	public final int astNodeType;
	public final int astParentNodeType;

	public Scope(final String code, final ScopeType scopeType,
			final String type, final int astNodeType,
			final int astParentNodeType) {
		this.code = code;
		this.scopeType = scopeType;
		this.type = type;
		this.astNodeType = astNodeType;
		this.astParentNodeType = astParentNodeType;
	}

	@Override
	public int compareTo(final Scope other) {
		return ComparisonChain.start().compare(code, other.code)
				.compare(scopeType, other.scopeType).compare(type, other.type)
				.compare(astNodeType, other.astNodeType)
				.compare(astParentNodeType, other.astParentNodeType).result();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Scope)) {
			return false;
		}
		final Scope other = (Scope) obj;
		return other.code.equals(code) && other.scopeType == scopeType
				&& other.astNodeType == astNodeType
				&& other.astParentNodeType == astParentNodeType
				&& other.type.equals(type);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(code, scopeType, type, astNodeType,
				astParentNodeType);
	}

	@Override
	public String toString() {
		return scopeType + " " + code;
	}
}