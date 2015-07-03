/**
 *
 */
package codemining.java.codeutils.binding;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Extract Java method bindings. Each method call or definition is used by
 * itself
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaMethodInvocationBindingExtractor extends
		AbstractJavaNameBindingsExtractor {

	public static enum AvailableFeatures {
		IMPLEMENTOR_VOCABULARY, ANCESTRY, NUMBER_ARGUMENTS
	}

	private static class MethodBindings extends ASTVisitor {
		/**
		 * A map from the method name to the position.
		 */
		final Multimap<String, ASTNode> methodNamePostions = HashMultimap
				.create();

		@Override
		public boolean visit(final MethodInvocation node) {
			final String name = node.getName().toString();
			methodNamePostions.put(name, node.getName());
			return super.visit(node);
		}
	}

	private final Set<AvailableFeatures> activeFeatures = Sets
			.newHashSet(AvailableFeatures.values());

	public JavaMethodInvocationBindingExtractor() {
		super(new JavaTokenizer());
	}

	public JavaMethodInvocationBindingExtractor(final ITokenizer tokenizer) {
		super(tokenizer);
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
		checkArgument(method instanceof MethodInvocation);
		final MethodInvocation mi = (MethodInvocation) method;
		if (activeFeatures.contains(AvailableFeatures.NUMBER_ARGUMENTS)) {
			features.add("nArgs:" + mi.arguments().size());
		}
		if (activeFeatures.contains(AvailableFeatures.IMPLEMENTOR_VOCABULARY)) {
			JavaFeatureExtractor.addImplementorVocab(mi, features);
		}
		if (activeFeatures.contains(AvailableFeatures.ANCESTRY)) {
			JavaFeatureExtractor.addAstAncestryFeatures(features, method);
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
