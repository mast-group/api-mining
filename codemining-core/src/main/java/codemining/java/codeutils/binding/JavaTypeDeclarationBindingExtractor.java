/**
 *
 */
package codemining.java.codeutils.binding;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Extract Java type name bindings.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaTypeDeclarationBindingExtractor extends
		AbstractJavaNameBindingsExtractor {

	public static enum AvailableFeatures {
		METHOD_VOCABULARY, FIELD_VOCABULARY, IMPLEMENTOR_VOCABULARY
	}

	private static class ClassnameFinder extends ASTVisitor {

		final Multimap<String, ASTNode> classNamePostions = HashMultimap
				.create();

		@Override
		public boolean visit(final TypeDeclaration node) {
			classNamePostions.put(node.getName().getIdentifier(),
					node.getName());
			return super.visit(node);
		}
	}

	private final Set<AvailableFeatures> activeFeatures = Sets
			.newHashSet(AvailableFeatures.values());

	public JavaTypeDeclarationBindingExtractor() {
		super(new JavaTokenizer());
	}

	public JavaTypeDeclarationBindingExtractor(final ITokenizer tokenizer) {
		super(tokenizer);
	}

	/**
	 * @param td
	 * @param currentTypeName
	 * @param features
	 */
	private void addFieldVocabulary(final TypeDeclaration td,
			final String currentTypeName, final Set<String> features) {
		for (final FieldDeclaration fd : td.getFields()) {
			for (final Object vdf : fd.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) vdf;
				for (final String namePart : JavaFeatureExtractor
						.getNameParts(frag.getName().toString())) {
					features.add("fieldVoc:" + namePart);
				}
			}
			if (!currentTypeName.equals(fd.getType().toString())) {
				features.add("fieldType:" + fd.getType().toString());
				for (final String namePart : JavaFeatureExtractor
						.getNameParts(fd.getType().toString())) {
					features.add("fieldVoc:" + namePart);
				}
			}
		}
	}

	/**
	 * @param td
	 * @param features
	 */
	private void addImplementorVocabulary(final TypeDeclaration td,
			final Set<String> features) {
		if (td.isInterface()) {
			features.add("isInterface");
		}

		for (final Object suptype : td.superInterfaceTypes()) {
			final Type supertype = (Type) suptype;
			for (final String namePart : JavaFeatureExtractor
					.getNameParts(supertype.toString())) {
				features.add("implementVoc:" + namePart);
			}
		}

		if (td.getSuperclassType() != null) {
			for (final String namePart : JavaFeatureExtractor.getNameParts(td
					.getSuperclassType().toString())) {
				features.add("implementVoc:" + namePart);
			}
		}
	}

	/**
	 * @param td
	 * @param currentTypeName
	 * @param features
	 */
	private void addMethodFeatures(final TypeDeclaration td,
			final String currentTypeName, final Set<String> features) {
		for (final MethodDeclaration md : td.getMethods()) {
			if (md.isConstructor()) {
				continue;
			}
			for (final String namePart : JavaFeatureExtractor.getNameParts(md
					.getName().getIdentifier())) {
				features.add("methodVoc:" + namePart);
			}
			for (final Object arg : md.parameters()) {
				final SingleVariableDeclaration svd = (SingleVariableDeclaration) arg;
				for (final String namePart : JavaFeatureExtractor
						.getNameParts(svd.getName().toString())) {
					features.add("methodVoc:" + namePart);
				}
				if (!svd.getType().toString().equals(currentTypeName)) {
					for (final String namePart : JavaFeatureExtractor
							.getNameParts(svd.getType().toString())) {
						features.add("methodVoc:" + namePart);
					}
				}
			}
		}
	}

	@Override
	public Set<?> getAvailableFeatures() {
		return Sets.newHashSet(AvailableFeatures.values());
	}

	@Override
	protected Set<String> getFeatures(final Set<ASTNode> boundNodes) {
		checkArgument(boundNodes.size() == 1);
		final ASTNode decl = boundNodes.iterator().next().getParent();

		checkArgument(decl instanceof TypeDeclaration);
		final TypeDeclaration td = (TypeDeclaration) decl;
		final String currentTypeName = td.getName().getIdentifier();
		final Set<String> features = Sets.newHashSet();
		if (activeFeatures.contains(AvailableFeatures.IMPLEMENTOR_VOCABULARY)) {
			addImplementorVocabulary(td, features);
		}

		if (activeFeatures.contains(AvailableFeatures.FIELD_VOCABULARY)) {
			addFieldVocabulary(td, currentTypeName, features);
		}

		if (activeFeatures.contains(AvailableFeatures.METHOD_VOCABULARY)) {
			addMethodFeatures(td, currentTypeName, features);
		}

		return features;
	}

	@Override
	public Set<Set<ASTNode>> getNameBindings(final ASTNode node) {
		final ClassnameFinder finder = new ClassnameFinder();
		node.accept(finder);

		final Set<Set<ASTNode>> nameBindings = Sets.newHashSet();
		for (final String typeName : finder.classNamePostions.keySet()) {
			for (final ASTNode nameNode : finder.classNamePostions
					.get(typeName)) {
				final Set<ASTNode> boundNodes = Sets.newIdentityHashSet();
				boundNodes.add(nameNode);
				nameBindings.add(boundNodes);
			}
		}
		return nameBindings;
	}

	@Override
	public void setActiveFeatures(final Set<?> activeFeatures) {
		this.activeFeatures.clear();
		this.activeFeatures
				.addAll((Collection<? extends AvailableFeatures>) activeFeatures);
	}

}
