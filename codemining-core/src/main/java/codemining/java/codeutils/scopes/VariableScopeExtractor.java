/**
 * 
 */
package codemining.java.codeutils.scopes;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.languagetools.IScopeExtractor;
import codemining.languagetools.ParseType;
import codemining.languagetools.Scope;
import codemining.languagetools.Scope.ScopeType;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Get the snippets were a variable is declared.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class VariableScopeExtractor {

	/**
	 * A variable struct object.
	 * 
	 */
	public static class Variable {
		public final String name;

		public final String type;

		public final ScopeType scope;

		public Variable(final String name, final String variableType,
				final ScopeType scope) {
			this.name = name;
			this.scope = scope;
			this.type = variableType;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Variable)) {
				return false;
			}
			final Variable other = (Variable) obj;
			return ComparisonChain.start().compare(name, other.name)
					.compare(type, other.type).compare(scope, other.scope)
					.result() == 0;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(name, type, scope);
		}

		@Override
		public String toString() {
			return name + "(" + type + ") at " + scope;
		}
	}

	/**
	 * An AST visitor that finds all the variables, along with the AST node
	 * where they are scoped in.
	 * 
	 */
	public static class VariableScopeFinder extends ASTVisitor {
		private final Multimap<ASTNode, Variable> variableScopes = HashMultimap
				.create();

		/**
		 * Return the variable scopes of the given node.
		 * 
		 * @param node
		 * @return
		 */
		public Multimap<ASTNode, Variable> getVariableScopes(final ASTNode node) {
			variableScopes.clear();
			node.accept(this);
			return ImmutableMultimap.copyOf(variableScopes);
		}

		@Override
		public boolean visit(final FieldDeclaration node) {
			final ASTNode parent = node.getParent();
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				variableScopes.put(parent, new Variable(frag.getName()
						.getIdentifier(), node.getType().toString(),
						ScopeType.SCOPE_CLASS));
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(final SingleVariableDeclaration node) {
			final ASTNode parent = node.getParent();
			if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
				variableScopes.put(parent, new Variable(node.getName()
						.getIdentifier(), node.getType().toString(),
						ScopeType.SCOPE_METHOD));
			} else {
				variableScopes.put(parent, new Variable(node.getName()
						.getIdentifier(), node.getType().toString(),
						ScopeType.SCOPE_LOCAL));
			}
			return false;
		}

		@Override
		public boolean visit(final VariableDeclarationExpression node) {
			final ASTNode parent = node.getParent();
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				variableScopes.put(parent, new Variable(frag.getName()
						.getIdentifier(), node.getType().toString(),
						ScopeType.SCOPE_LOCAL));
			}
			return false;
		}

		@Override
		public boolean visit(final VariableDeclarationStatement node) {
			final ASTNode parent = node.getParent();
			for (final Object fragment : node.fragments()) {
				final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
				variableScopes.put(parent, new Variable(frag.getName()
						.getIdentifier(), node.getType().toString(),
						ScopeType.SCOPE_LOCAL));
			}
			return false;
		}

	}

	/**
	 * Create a multimap with all the variables that are valid (under the scope)
	 * of each ASTNode.
	 * 
	 */
	private static class VariableScopeResolver extends ASTVisitor {
		private final Multimap<ASTNode, Variable> variables = HashMultimap
				.create();

		private final Multimap<ASTNode, Variable> variableDeclarations;

		public VariableScopeResolver(
				final Multimap<ASTNode, Variable> declarations) {
			variableDeclarations = declarations;
		}

		@Override
		public void preVisit(final ASTNode node) {
			if (node.getParent() != null) {
				variables.putAll(node, variables.get(node.getParent()));
			}
			variables.putAll(node, variableDeclarations.get(node));
			super.preVisit(node);
		}

	}

	public static final class VariableScopeSnippetExtractor implements
			IScopeExtractor {
		@Override
		public Multimap<Scope, String> getFromFile(final File f)
				throws IOException {
			return getScopeSnippets(f);
		}

		@Override
		public Multimap<Scope, String> getFromNode(final ASTNode node) {
			return getScopeSnippets(node);
		}

		@Override
		public Multimap<Scope, String> getFromString(final String code,
				final ParseType parseType) {
			return getScopeSnippets(code, parseType);
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(VariableScopeExtractor.class.getName());

	/**
	 * Return the variables that are valid at each ASTNode.
	 * 
	 * @param node
	 * @return
	 */
	public static Multimap<ASTNode, Variable> getDefinedVarsPerNode(
			final ASTNode node) {
		final Multimap<ASTNode, Variable> definedScopes = getVariableScopes(node);
		final VariableScopeResolver resolver = new VariableScopeResolver(
				definedScopes);
		node.accept(resolver);
		return resolver.variables;
	}

	/**
	 * Return a multimap containing all the (local) variables of the given
	 * scope.
	 * 
	 * @param cu
	 * @return Multimap<Snippet, VariableName>
	 */
	public static Multimap<Scope, String> getScopeSnippets(final ASTNode cu) {
		final VariableScopeFinder scopeFinder = new VariableScopeFinder();
		cu.accept(scopeFinder);

		final Multimap<Scope, String> scopes = TreeMultimap.create();
		for (final Entry<ASTNode, Variable> variable : scopeFinder.variableScopes
				.entries()) {
			final int astNodeType = variable.getKey().getNodeType();
			final int astNodeParentType;
			if (variable.getKey().getParent() == null) {
				astNodeParentType = -1;
			} else {
				astNodeParentType = variable.getKey().getParent().getNodeType();
			}
			scopes.put(
					new Scope(variable.getKey().toString(),
							variable.getValue().scope,
							variable.getValue().type, astNodeType,
							astNodeParentType), variable.getValue().name);
		}

		return scopes;

	}

	/**
	 * Retrieve variables scopes, given a file. The file is assumed to contain a
	 * full compilation unit.
	 * 
	 * @param file
	 * @return a Multimap for each scope (ASTNode) to the variables under that
	 *         scope.
	 * @throws IOException
	 */
	public static Multimap<Scope, String> getScopeSnippets(final File file)
			throws IOException {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return getScopeSnippets(ex.getAST(file));
	}

	/**
	 * Retrieve the variable scopes for the given snippet.
	 * 
	 * @param code
	 * @param parseType
	 * @return
	 */
	public static Multimap<Scope, String> getScopeSnippets(final String code,
			final ParseType parseType) {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return getScopeSnippets(ex.getAST(code, parseType));
	}

	/**
	 * Return a Multimap containing the variables that belong to the scope of
	 * the each ASTNode.
	 * 
	 * @param rootNode
	 * @return
	 */
	public static Multimap<ASTNode, Variable> getVariableScopes(
			final ASTNode rootNode) {
		final VariableScopeFinder scopeFinder = new VariableScopeFinder();
		rootNode.accept(scopeFinder);
		return scopeFinder.variableScopes;
	}

	public static Multimap<ASTNode, Variable> getVariableScopes(final File file)
			throws IOException {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return getVariableScopes(ex.getAST(file));
	}

	/**
	 * Return the variables defined (having scope) the each ASTNode.
	 * 
	 * @param code
	 * @param parseKind
	 * @return
	 * @throws IOException
	 */
	public static Multimap<ASTNode, Variable> getVariableScopes(
			final String code, final ParseType parseKind) {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return getVariableScopes(ex.getAST(code, parseKind));
	}
}
