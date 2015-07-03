/**
 *
 */
package codemining.java.codeutils.binding;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Utility class to get various features, related to bindings.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaFeatureExtractor {

	/**
	 * Extract the token parts from the contents of a method declaration.
	 *
	 */
	private static class MethodTopicNames extends ASTVisitor {
		final Set<String> nameParts = Sets.newHashSet();

		private String methodName = "";

		void populateNames(final MethodDeclaration declaration) {
			methodName = declaration.getName().getIdentifier();
			for (final Object param : declaration.parameters()) {
				((ASTNode) param).accept(this);
			}
			if (declaration.getBody() != null) {
				declaration.getBody().accept(this);
			}
		}

		@Override
		public boolean visit(final SimpleName node) {
			if (!node.getIdentifier().equals(methodName)) {
				nameParts.addAll(JavaFeatureExtractor.getNameParts(node
						.getIdentifier()));
			}
			return super.visit(node);
		}
	}

	/**
	 * Add ancestry features (parent and grandparent) of a node)
	 *
	 * @param features
	 * @param node
	 */
	public static void addAstAncestryFeatures(final Set<String> features,
			final ASTNode node) {
		features.add("DeclParentAstType:"
				+ ASTNode.nodeClassForType(node.getParent().getNodeType())
						.getSimpleName());
		features.add("DeclGrandparentAstType:"
				+ ASTNode.nodeClassForType(
						node.getParent().getParent().getNodeType())
						.getSimpleName());
	}

	public static void addFields(final ASTNode node, final Set<String> features) {
		checkArgument(node.getRoot() instanceof CompilationUnit);
		final CompilationUnit cu = (CompilationUnit) node.getRoot();
		for (final Object type : cu.types()) {
			if (type instanceof TypeDeclaration) {
				final TypeDeclaration td = (TypeDeclaration) type;
				for (final FieldDeclaration fd : td.getFields()) {
					for (final Object decl : fd.fragments()) {
						final VariableDeclarationFragment vdf = (VariableDeclarationFragment) decl;
						final List<String> nameParts = getNameParts(vdf
								.getName().getIdentifier());
						nameParts.forEach(np -> features.add("inScope:" + np));
					}
				}
			}
		}
	}

	/**
	 * Add the token parts of the method and class where the current node is
	 * placed.
	 *
	 * @param node
	 * @param features
	 */
	public static void addImplementorVocab(final ASTNode node,
			final Set<String> features) {
		ASTNode currentNode = node;
		final List<String> tokenParts = Lists.newArrayList();
		while (currentNode.getParent() != null) {
			currentNode = currentNode.getParent();
			if (currentNode instanceof MethodDeclaration) {
				final MethodDeclaration md = (MethodDeclaration) currentNode;
				tokenParts.addAll(JavaFeatureExtractor.getNameParts(md
						.getName().toString()));
			} else if (currentNode instanceof TypeDeclaration) {
				final TypeDeclaration td = (TypeDeclaration) currentNode;
				tokenParts.addAll(JavaFeatureExtractor.getNameParts(td
						.getName().toString()));
				if (td.getSuperclassType() != null) {
					tokenParts.addAll(JavaFeatureExtractor.getNameParts(td
							.getSuperclassType().toString()));
				}
				for (final Object ifaceType : td.superInterfaceTypes()) {
					tokenParts.addAll(JavaFeatureExtractor
							.getNameParts((((Type) ifaceType).toString())));
				}
			}
		}

		if (tokenParts != null) {
			for (final String tokenPart : tokenParts) {
				features.add("inName:" + tokenPart);
			}
		}
	}

	/**
	 * Return the features to
	 *
	 * @param declaration
	 * @param features
	 *            where the features will be added.
	 */
	public static void addMethodTopicFeatures(
			final MethodDeclaration declaration, final Set<String> features) {
		final MethodTopicNames namesExtractor = new MethodTopicNames();
		namesExtractor.populateNames(declaration);
		features.addAll(namesExtractor.nameParts);
	}

	/**
	 * Add any modifiers as features.
	 *
	 * @param features
	 * @param modifiers
	 */
	public static void addModifierFeatures(final Set<String> features,
			final List<?> modifiers) {
		for (final Object modifier : modifiers) {
			final IExtendedModifier extendedModifier = (IExtendedModifier) modifier;
			features.add(extendedModifier.toString());
		}
	}

	public static void addSiblingMethodNames(
			final MethodDeclaration declaration, final Set<String> features) {
		if (!(declaration.getParent() instanceof TypeDeclaration)) {
			return;
		}
		final TypeDeclaration td = (TypeDeclaration) declaration.getParent();
		for (final MethodDeclaration md : td.getMethods()) {
			if (md.getName().getIdentifier()
					.equals(declaration.getName().getIdentifier())) {
				continue;
			}
			final List<String> nameparts = getNameParts(md.getName()
					.getIdentifier());
			nameparts.forEach(p -> features.add("sibling:" + p));
		}
	}

	/**
	 * @param type
	 * @param features
	 */
	public static void addTypeFeatures(final Type type,
			final Set<String> features) {
		features.add(type.toString());
		if (type.isParameterizedType()) {
			features.add("isParameterizedType");
			final ParameterizedType paramType = (ParameterizedType) type;
			features.add(paramType.getType().toString());
		} else if (type.isArrayType()) {
			features.add("isArrayType");
			final ArrayType arrayType = (ArrayType) type;
			features.add("arrayDims:" + arrayType.dimensions().size());
			features.add("arrayType:" + arrayType.getElementType().toString());
		}
	}

	public static List<String> getNameParts(final String name) {
		final List<String> nameParts = Lists.newArrayList();
		for (final String snakecasePart : name.split("_")) {
			for (final String w : snakecasePart
					.split("(?<!(^|[A-Z]))(?=[A-Z0-9])|(?<!^)(?=[A-Z][a-z])")) {
				nameParts.add(w.toLowerCase());
			}
		}
		return nameParts;
	}

	private JavaFeatureExtractor() {
	}

}
