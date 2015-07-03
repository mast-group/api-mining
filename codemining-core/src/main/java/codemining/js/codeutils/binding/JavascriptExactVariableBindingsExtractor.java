/**
 *
 */
package codemining.js.codeutils.binding;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.wst.jsdt.core.dom.*;

import codemining.js.codeutils.JavascriptASTExtractor;
import codemining.languagetools.bindings.TokenNameBinding;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Retrieve the variable bindings, given an ASTNode. This finds exact bindings
 * to the detriment of recall. Partial code snippets are not supported.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavascriptExactVariableBindingsExtractor extends
		AbstractJavascriptNameBindingsExtractor {

	/**
	 * This class looks for declarations of variables and the references to
	 * them.
	 *
	 */
	private static class VariableBindingFinder extends ASTVisitor {
		/**
		 * Map of variables (represented as bindings) to all token positions
		 * where the variable is referenced.
		 */
		Map<IVariableBinding, List<ASTNode>> variableScope = Maps
				.newIdentityHashMap();

		private void addBinding(final IVariableBinding binding) {
			variableScope.put(binding, Lists.<ASTNode> newArrayList());
		}

		/**
		 * @param binding
		 */
		private void addBindingData(final IVariableBinding binding,
				final ASTNode nameNode) {
			if (binding == null) {
				return; // Sorry, cannot do anything.
			}
			final List<ASTNode> thisVarBindings = checkNotNull(
					variableScope.get(binding),
					"Binding was not previously found");
			thisVarBindings.add(nameNode);
		}

		/**
		 * Looks for field declarations (i.e. class member variables).
		 */
		@Override
		public boolean visit(final FieldDeclaration node) {
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				final IVariableBinding binding = frag.resolveBinding();
				addBinding(binding);
			}
			return true;
		}

		/**
		 * Visits {@link SimpleName} AST nodes. Resolves the binding of the
		 * simple name and looks for it in the {@link #variableScope} map. If
		 * the binding is found, this is a reference to a variable.
		 *
		 * @param node
		 *            the node to visit
		 */
		@Override
		public boolean visit(final SimpleName node) {
			final IBinding binding = node.resolveBinding();
			if (variableScope.containsKey(binding)) {
				addBindingData((IVariableBinding) binding, node);
			}
			return true;
		}

		/**
		 * Looks for Method Parameters.
		 */
		@Override
		public boolean visit(final SingleVariableDeclaration node) {
			final IVariableBinding binding = node.resolveBinding();
			if (binding != null) {
				addBinding(binding);
			}
			return true;
		}

		/**
		 * Looks for variables declared in for loops.
		 */
		@Override
		public boolean visit(final VariableDeclarationExpression node) {
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				final IVariableBinding binding = frag.resolveBinding();
				if (binding != null) {
					addBinding(binding);
				}
			}
			return true;
		}

		/**
		 * Looks for local variable declarations. For every declaration of a
		 * variable, the parent {@link Block} denoting the variable's scope is
		 * stored in {@link #variableScope} map.
		 *
		 * @param node
		 *            the node to visit
		 */
		@Override
		public boolean visit(final VariableDeclarationStatement node) {
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				final IVariableBinding binding = frag.resolveBinding();
				if (binding != null) {
					addBinding(binding);
				}
			}
			return true;
		}
	}

	@Override
	protected JavascriptASTExtractor createExtractor() {
		return new JavascriptASTExtractor(true);
	}

	@Override
	public Set<?> getAvailableFeatures() {
		return Collections.emptySet();
	}

	@Override
	public Set<Set<ASTNode>> getNameBindings(final ASTNode node) {
		final VariableBindingFinder bindingFinder = new VariableBindingFinder();
		node.accept(bindingFinder);

		final Set<Set<ASTNode>> nameBindings = Sets.newHashSet();
		for (final Entry<IVariableBinding, List<ASTNode>> variableBindings : bindingFinder.variableScope
				.entrySet()) {
			final Set<ASTNode> boundNodes = Sets.newIdentityHashSet();
			boundNodes.addAll(variableBindings.getValue());
			nameBindings.add(boundNodes);
		}
		return nameBindings;
	}

	@Override
	public List<TokenNameBinding> getNameBindings(final String code) {
		throw new UnsupportedOperationException(
				"Partial snippets cannot be resolved due to the "
						+ "lack of support from Eclipse JSDT. Consider using the approximate binding extractor.");
	}

	@Override
	public void setActiveFeatures(final Set<?> activeFeatures) {
		throw new NotImplementedException();
	}
}
