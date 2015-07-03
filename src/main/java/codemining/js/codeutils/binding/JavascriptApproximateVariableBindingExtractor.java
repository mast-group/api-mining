/**
 *
 */
package codemining.js.codeutils.binding;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * An approximate best-effort (worse-precision) variable binding extractor.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavascriptApproximateVariableBindingExtractor extends
		AbstractJavascriptNameBindingsExtractor {

	/**
	 * This class looks for declarations of variables and the references to
	 * them.
	 *
	 */
	private static class VariableBindingFinder extends ASTVisitor {
		private int nextDeclarId = 0;

		/**
		 * Map the names that are defined in each ast node, with their
		 * respective ids.
		 */
		private final Map<ASTNode, Map<String, Integer>> variableNames = Maps
				.newIdentityHashMap();

		/**
		 * Map of variables (represented with their ids) to all token positions
		 * where the variable is referenced.
		 */
		Map<Integer, List<ASTNode>> variableBinding = Maps.newTreeMap();

		/**
		 * Add the binding to the current scope.
		 *
		 * @param scopeBindings
		 * @param name
		 */
		private void addBinding(final ASTNode node, final String name) {
			final int bindingId = nextDeclarId;
			nextDeclarId++;
			variableNames.get(node).put(name, bindingId);
			variableNames.get(node.getParent()).put(name, bindingId);
			variableBinding.put(bindingId, Lists.<ASTNode> newArrayList());
		}

		/**
		 * Add the binding data for the given name at the given scope and
		 * position.
		 */
		private void addBindingData(final String name, final ASTNode nameNode,
				final Map<String, Integer> scopeBindings) {
			// Get varId or abort
			final Integer variableId = scopeBindings.get(name);
			if (variableId == null || !variableBinding.containsKey(variableId)) {
				return;
			}
			variableBinding.get(variableId).add(nameNode);
		}

		@Override
		public void preVisit(final ASTNode node) {
			final ASTNode parent = node.getParent();
			if (parent != null && variableNames.containsKey(parent)) {
				// inherit all variables in parent scope
				final Map<String, Integer> bindingsCopy = Maps.newTreeMap();
				for (final Entry<String, Integer> binding : variableNames.get(
						parent).entrySet()) {
					bindingsCopy.put(binding.getKey(), binding.getValue());
				}

				variableNames.put(node, bindingsCopy);
			} else {
				// Start from scratch
				variableNames.put(node, Maps.<String, Integer> newTreeMap());
			}
			super.preVisit(node);
		}

		/**
		 * Looks for field declarations (i.e. class member variables).
		 */
		@Override
		public boolean visit(final FieldDeclaration node) {
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				addBinding(node, frag.getName().getIdentifier());
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
			addBindingData(node.getIdentifier(), node, variableNames.get(node));
			return true;
		}

		/**
		 * Looks for Method Parameters.
		 */
		@Override
		public boolean visit(final SingleVariableDeclaration node) {
			addBinding(node, node.getName().getIdentifier());
			return true;
		}

		/**
		 * Looks for variables declared in for loops.
		 */
		@Override
		public boolean visit(final VariableDeclarationExpression node) {
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				addBinding(node, frag.getName().getIdentifier());
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
				addBinding(node, frag.getName().getIdentifier());
			}
			return true;
		}
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
		for (final Entry<Integer, List<ASTNode>> variableBindings : bindingFinder.variableBinding
				.entrySet()) {
			final Set<ASTNode> boundNodes = Sets.newIdentityHashSet();
			boundNodes.addAll(variableBindings.getValue());
			nameBindings.add(boundNodes);
		}
		return nameBindings;
	}

	@Override
	public void setActiveFeatures(final Set<?> activeFeatures) {
		throw new NotImplementedException();
	}
}
