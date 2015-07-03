/**
 *
 */
package codemining.java.codeutils.binding;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.google.common.collect.Sets;

/**
 * Utility class to extract features from a variable.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaVariableFeatureExtractor {

	public static enum AvailableFeatures {
		IMPLEMENTOR_VOCABULARY, TYPE, MODIFIERS, ANCESTRY
	}

	private Set<AvailableFeatures> activeFeatures = Sets
			.newHashSet(AvailableFeatures.values());

	public JavaVariableFeatureExtractor() {
	}

	/**
	 * @param features
	 * @param declarationPoint
	 */
	private void getDeclarationFeatures(final Set<String> features,
			final ASTNode declarationPoint) {
		final Type variableType;
		final List modifiers;
		final ASTNode ancestryFrom;
		if (declarationPoint.getParent() instanceof SingleVariableDeclaration) {
			final SingleVariableDeclaration declaration = (SingleVariableDeclaration) declarationPoint
					.getParent();
			variableType = declaration.getType();
			modifiers = declaration.modifiers();
			ancestryFrom = declaration;
		} else if (declarationPoint.getParent() instanceof VariableDeclarationStatement) {
			final VariableDeclarationStatement declaration = (VariableDeclarationStatement) declarationPoint
					.getParent();
			variableType = declaration.getType();
			modifiers = declaration.modifiers();
			ancestryFrom = declaration;
		} else if (declarationPoint.getParent() instanceof VariableDeclarationFragment) {
			if (declarationPoint.getParent().getParent() instanceof VariableDeclarationStatement) {
				final VariableDeclarationStatement declaration = (VariableDeclarationStatement) declarationPoint
						.getParent().getParent();
				variableType = declaration.getType();
				modifiers = declaration.modifiers();
				ancestryFrom = declaration;
			} else if (declarationPoint.getParent().getParent() instanceof FieldDeclaration) {
				final FieldDeclaration declaration = (FieldDeclaration) declarationPoint
						.getParent().getParent();
				variableType = declaration.getType();
				modifiers = declaration.modifiers();
				ancestryFrom = declaration;
			} else if (declarationPoint.getParent().getParent() instanceof VariableDeclarationExpression) {
				final VariableDeclarationExpression declaration = (VariableDeclarationExpression) declarationPoint
						.getParent().getParent();
				variableType = declaration.getType();
				modifiers = declaration.modifiers();
				ancestryFrom = declaration;
			} else {
				return;
			}
		} else {
			throw new IllegalStateException("Should not reach this");
		}

		if (activeFeatures.contains(AvailableFeatures.TYPE)) {
			JavaFeatureExtractor.addTypeFeatures(variableType, features);
		}
		if (activeFeatures.contains(AvailableFeatures.MODIFIERS)) {
			JavaFeatureExtractor.addModifierFeatures(features, modifiers);
		}
		if (activeFeatures.contains(AvailableFeatures.ANCESTRY)) {
			JavaFeatureExtractor.addAstAncestryFeatures(features, ancestryFrom);
		}
	}

	public void setActiveFeatures(final Collection<AvailableFeatures> features) {
		activeFeatures = Sets.newHashSet(features);
	}

	public Set<String> variableFeatures(final Set<ASTNode> boundNodesOfVariable) {
		// Find the declaration and extract features
		final Set<String> features = Sets.newHashSet();
		for (final ASTNode node : boundNodesOfVariable) {
			if (!(node.getParent() instanceof VariableDeclaration)) {
				continue;
			}
			getDeclarationFeatures(features, node);
			if (activeFeatures
					.contains(AvailableFeatures.IMPLEMENTOR_VOCABULARY)) {
				JavaFeatureExtractor.addImplementorVocab(node, features);
			}
			break;
		}
		return features;
	}

}
