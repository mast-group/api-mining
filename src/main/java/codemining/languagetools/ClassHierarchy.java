package codemining.languagetools;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A class hierarchy contains all the implemented types of a single type.
 *
 * The type hierarchy makes an effort to be as compact as possible, removing
 * redundant is-a relationships.
 *
 * Not thread safe.
 *
 */
public class ClassHierarchy implements Serializable {

	public static final class Type implements Serializable {

		public final String fullQualifiedName;

		private final Set<Type> implementingTypes = Sets.newIdentityHashSet();

		/**
		 * A closure of the implementing types.
		 */
		private final Set<Type> implementingTypesClosure = Sets
				.newIdentityHashSet();

		private final Set<Type> childTypes = Sets.newIdentityHashSet();
		/**
		 * A closure of the child Types
		 */
		private final Set<Type> childTypesClosure = Sets.newIdentityHashSet();

		private static final long serialVersionUID = -4245298170285828934L;

		public Type(final String fqName) {
			fullQualifiedName = fqName;
		}

		/**
		 * Add a childType for the given type, only if it does not belong to its
		 * transitive closure.
		 *
		 * @param childType
		 */
		private final void addChildType(final Type childType) {
			checkArgument(!implementingTypesClosure.contains(childType));
			if (childTypesClosure.contains(childType)) {
				return;
			}

			// If the given type is already a child of a parent type,
			// we need to remove it from its implementing type.
			implementingTypesClosure.forEach(t -> t.childTypes
					.remove(childType));

			childTypes.add(childType);

			// Update parents closures
			implementingTypesClosure.forEach(t -> t.childTypesClosure
					.add(childType));
		}

		/**
		 * Add an implementing type of this type only if it does not belong to
		 * its transitive closure.
		 *
		 * @param implementingType
		 */
		private final void addImplementingType(final Type implementingType) {
			checkArgument(!childTypesClosure.contains(implementingType));
			if (implementingTypesClosure.contains(implementingType)) {
				return;
			}

			// If the type is already an implementing type of a child,
			// we need to remove it from its child types
			childTypesClosure.forEach(t -> t.implementingTypes
					.remove(implementingType));

			implementingTypes.add(implementingType);

			// Update children closures
			childTypesClosure.forEach(t -> t.implementingTypesClosure
					.add(implementingType));
		}

		public Collection<Type> getImplementingTypesClosure() {
			return new ImmutableList.Builder<Type>()
					.addAll(implementingTypesClosure).addAll(implementingTypes)
					.build();
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(fullQualifiedName);
			if (!implementingTypes.isEmpty()) {
				sb.append("[ implements ");
				implementingTypes.forEach(t -> sb.append(t.fullQualifiedName
						+ " "));
			}
			if (!childTypes.isEmpty()) {
				sb.append(" isimplementedby ");
				childTypes.forEach(t -> sb.append(t.fullQualifiedName + " "));
			}
			sb.append("]");
			return sb.toString();
		}

	}

	private static final long serialVersionUID = 8866244164953568827L;

	private final Map<String, Type> nameToType = Maps.newTreeMap();

	/**
	 * Add a type relationship.
	 *
	 * @param type
	 * @param parentTypeFqn
	 */
	public void addParentToType(final String type, final String parentTypeFqn) {
		final Type childType = getTypeOrNew(type);
		final Type parentType = getTypeOrNew(parentTypeFqn);
		childType.addImplementingType(parentType);
		parentType.addChildType(childType);
	}

	public Optional<Type> getTypeForName(final String fqName) {
		if (nameToType.containsKey(fqName)) {
			return Optional.of(nameToType.get(fqName));
		}
		return Optional.absent();
	}

	/**
	 * Get a type that already exists or a create a new type.
	 *
	 * @param fqName
	 */
	private Type getTypeOrNew(final String fqName) {
		final Type type;
		if (nameToType.containsKey(fqName)) {
			type = nameToType.get(fqName);
		} else {
			type = new Type(fqName);
			nameToType.put(fqName, type);
		}
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Type type : nameToType.values()) {
			sb.append(type.toString());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

}