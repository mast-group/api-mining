/**
 *
 */
package codemining.java.codeutils.binding;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codemining.java.codedata.metrics.CyclomaticCalculator;
import codemining.java.codeutils.MethodUtils;
import codemining.java.codeutils.ProjectTypeInformation;
import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Extract bindings (and features) for method delarations.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaMethodDeclarationBindingExtractor extends
		AbstractJavaNameBindingsExtractor {

	public static enum AvailableFeatures {
		ARGUMENTS, EXCEPTIONS, RETURN_TYPE, MODIFIERS, ANCESTRY, METHOD_TOPICS, IMPLEMENTOR_VOCABULARY, FIELDS, SIBLING_METHODS, CYCLOMATIC
	}

	private class MethodBindings extends ASTVisitor {

		Stack<String> className = new Stack<String>();

		private String currentPackageName;

		/**
		 * A map from the method name to the position.
		 */
		Multimap<String, ASTNode> methodNamePostions = HashMultimap.create();

		@Override
		public void endVisit(final TypeDeclaration node) {
			className.pop();
			super.endVisit(node);
		}

		/**
		 * @param node
		 * @return
		 */
		public boolean methodOverrides(final MethodDeclaration node) {
			final boolean hasAnnotation = MethodUtils
					.hasOverrideAnnotation(node);
			final boolean isOverride = pti.isMethodOverride(className.peek(),
					node);
			return hasAnnotation || isOverride;
		}

		@Override
		public boolean visit(final CompilationUnit node) {
			if (node.getPackage() != null) {
				currentPackageName = node.getPackage().getName()
						.getFullyQualifiedName();
			} else {
				currentPackageName = "";
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(final ImportDeclaration node) {
			// Don't visit. It's boring
			return false;
		}

		@Override
		public boolean visit(final MethodDeclaration node) {
			if (node.isConstructor()) {
				return super.visit(node);
			} else if (!includeOverrides && methodOverrides(node)) {
				return super.visit(node);
			}
			final String name = node.getName().toString();
			methodNamePostions.put(name, node.getName());
			return super.visit(node);
		}

		@Override
		public boolean visit(final TypeDeclaration node) {
			if (className.isEmpty()) {
				className.push(currentPackageName + "."
						+ node.getName().getIdentifier());
			} else {
				className.push(className.peek() + "."
						+ node.getName().getIdentifier());
			}
			return super.visit(node);
		}
	}

	private final boolean includeOverrides;

	private final Set<AvailableFeatures> activeFeatures = Sets
			.newHashSet(AvailableFeatures.values());

	private final ProjectTypeInformation pti;

	public JavaMethodDeclarationBindingExtractor() {
		super(new JavaTokenizer());
		this.includeOverrides = true;
		pti = null;
	}

	public JavaMethodDeclarationBindingExtractor(
			final boolean includeOverrides, final File inputFolder) {
		super(new JavaTokenizer());
		this.includeOverrides = includeOverrides;
		if (!includeOverrides) {
			pti = buildProjectTypeInformation(inputFolder);
		} else {
			pti = null;
		}
	}

	public JavaMethodDeclarationBindingExtractor(final ITokenizer tokenizer) {
		super(tokenizer);
		this.includeOverrides = true;
		pti = null;
	}

	public JavaMethodDeclarationBindingExtractor(final ITokenizer tokenizer,
			final boolean includeOverrides, final File inputFolder) {
		super(tokenizer);
		this.includeOverrides = includeOverrides;
		if (!includeOverrides) {
			pti = buildProjectTypeInformation(inputFolder);
		} else {
			pti = null;
		}
	}

	/**
	 * Add argument-related features.
	 *
	 * @param md
	 * @param features
	 */
	private void addArgumentFeatures(final MethodDeclaration md,
			final Set<String> features) {
		checkArgument(activeFeatures.contains(AvailableFeatures.ARGUMENTS));
		features.add("nParams:" + md.parameters().size());
		for (int i = 0; i < md.parameters().size(); i++) {
			final SingleVariableDeclaration varDecl = (SingleVariableDeclaration) md
					.parameters().get(i);
			features.add("param" + i + "Type:" + varDecl.getType().toString());
			for (final String namepart : JavaFeatureExtractor
					.getNameParts(varDecl.getName().toString())) {
				features.add("paramName:" + namepart);
			}
		}

		if (md.isVarargs()) {
			features.add("isVarArg");
		}
	}

	/**
	 * Add exception related features.
	 *
	 * @param md
	 * @param features
	 */
	private void addExceptionFeatures(final MethodDeclaration md,
			final Set<String> features) {
		checkArgument(activeFeatures.contains(AvailableFeatures.EXCEPTIONS));
		for (final Object exception : md.thrownExceptionTypes()) {
			final SimpleType ex = (SimpleType) exception;
			features.add("thrownException:" + ex.toString());
		}
	}

	/**
	 * Add modifier-related features.
	 *
	 * @param md
	 * @param features
	 */
	private void addModifierFeatures(final MethodDeclaration md,
			final Set<String> features) {
		checkArgument(activeFeatures.contains(AvailableFeatures.MODIFIERS));
		JavaFeatureExtractor.addModifierFeatures(features, md.modifiers());

		if (md.getBody() == null) {
			features.add("isInterfaceDeclaration");
		}
	}

	private ProjectTypeInformation buildProjectTypeInformation(
			final File inputFolder) {
		final ProjectTypeInformation pti = new ProjectTypeInformation(
				inputFolder);
		pti.collect();
		return pti;
	}

	@Override
	public Set<?> getAvailableFeatures() {
		return Sets.newHashSet(AvailableFeatures.values());
	}

	@Override
	protected Set<String> getFeatures(final Set<ASTNode> boundNodes) {
		checkArgument(boundNodes.size() == 1);
		final ASTNode method = boundNodes.iterator().next().getParent();
		final Set<String> features = Sets.newHashSet();

		checkArgument(method instanceof MethodDeclaration);
		final MethodDeclaration md = (MethodDeclaration) method;
		if (activeFeatures.contains(AvailableFeatures.ARGUMENTS)) {
			addArgumentFeatures(md, features);
		}
		if (activeFeatures.contains(AvailableFeatures.EXCEPTIONS)) {
			addExceptionFeatures(md, features);
		}

		if (activeFeatures.contains(AvailableFeatures.RETURN_TYPE)) {
			features.add("returnType:" + md.getReturnType2());
		}
		if (activeFeatures.contains(AvailableFeatures.MODIFIERS)) {
			addModifierFeatures(md, features);
		}

		if (activeFeatures.contains(AvailableFeatures.ANCESTRY)) {
			JavaFeatureExtractor.addAstAncestryFeatures(features, method);
		}
		if (activeFeatures.contains(AvailableFeatures.METHOD_TOPICS)) {
			JavaFeatureExtractor.addMethodTopicFeatures(md, features);
		}
		if (activeFeatures.contains(AvailableFeatures.IMPLEMENTOR_VOCABULARY)) {
			JavaFeatureExtractor.addImplementorVocab(method, features);
		}
		if (activeFeatures.contains(AvailableFeatures.FIELDS)) {
			JavaFeatureExtractor.addFields(method, features);
		}
		if (activeFeatures.contains(AvailableFeatures.SIBLING_METHODS)) {
			JavaFeatureExtractor.addSiblingMethodNames(md, features);
		}
		if (activeFeatures.contains(AvailableFeatures.CYCLOMATIC)) {
			features.add("cyclomatic:"
					+ (int) (new CyclomaticCalculator()
							.getMetricForASTNode(method)));
		}
		return features;
	}

	@Override
	public Set<Set<ASTNode>> getNameBindings(final ASTNode node) {
		final MethodBindings mb = new MethodBindings();
		node.accept(mb);

		final Set<Set<ASTNode>> nameBindings = Sets.newHashSet();
		for (final Entry<String, ASTNode> entry : mb.methodNamePostions
				.entries()) {
			final Set<ASTNode> boundNodes = Sets.newIdentityHashSet();
			boundNodes.add(entry.getValue());
			nameBindings.add(boundNodes);
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
