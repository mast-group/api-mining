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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.languagetools.IScopeExtractor;
import codemining.languagetools.ParseType;
import codemining.languagetools.Scope;
import codemining.languagetools.Scope.ScopeType;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Extract method names from a scope.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class MethodScopeExtractor {

	public static class Method {

		public final String name;
		public final ScopeType type;

		public Method(final String name, final ScopeType type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Method)) {
				return false;
			}
			Method other = (Method) obj;
			return name.equals(other.name) && type == other.type;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(name, type);
		}
	}

	public static final class MethodScopeSnippetExtractor implements
			IScopeExtractor {

		final boolean methodAsRoots;

		public MethodScopeSnippetExtractor(final boolean useMethodsAsRoots) {
			methodAsRoots = useMethodsAsRoots;
		}

		@Override
		public final Multimap<Scope, String> getFromFile(final File f) {
			try {
				return getScopeSnippets(f, methodAsRoots);
			} catch (IOException e) {
				LOGGER.severe("Unable to extract method scope snippets from file "
						+ f.getName());
				throw new IllegalArgumentException(
						"Unable to extract method scope snippets from file");
			}
		}

		@Override
		public Multimap<Scope, String> getFromNode(final ASTNode node) {
			return getScopeSnippets(node, methodAsRoots);
		}

		@Override
		public final Multimap<Scope, String> getFromString(final String code,
				final ParseType parseType) {
			return getScopeSnippets(code, methodAsRoots, parseType);
		}
	}

	private static class ScopeFinder extends ASTVisitor {

		final Multimap<ASTNode, Method> methods = HashMultimap.create();

		ASTNode classNode = null;
		ASTNode currentMethodNode = null;
		final boolean methodAsRoot;

		public ScopeFinder(final boolean methodAsRoots) {
			methodAsRoot = methodAsRoots;
		}

		@Override
		public void endVisit(MethodDeclaration node) {
			if (currentMethodNode == node) {
				currentMethodNode = null;
			}
			super.endVisit(node);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (currentMethodNode == null) {
				currentMethodNode = node;
			}
			if (node.isConstructor())
				return super.visit(node);
			final String name = node.getName().toString();

			final Method mth = new Method(name, ScopeType.SCOPE_CLASS);
			if (classNode != null) {
				methods.put(classNode, mth);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodInvocation node) {
			final String name = node.getName().toString();

			if (methodAsRoot && currentMethodNode != null) {
				final Method mth = new Method(name, ScopeType.SCOPE_METHOD);
				methods.put(currentMethodNode, mth);
			} else {
				final Method mth = new Method(name, ScopeType.SCOPE_CLASS);
				methods.put(classNode, mth);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (classNode == null) {
				classNode = node;
			}
			return super.visit(node);
		}
	}

	public static final String METHOD_CALL = "%MethodCall%";

	private static final Logger LOGGER = Logger
			.getLogger(MethodScopeExtractor.class.getName());

	public static Multimap<Scope, String> getScopeSnippets(final ASTNode node,
			final boolean methodAsRoots) {
		final ScopeFinder scopeFinder = new ScopeFinder(methodAsRoots);
		node.accept(scopeFinder);

		final Multimap<Scope, String> scopes = TreeMultimap.create();
		for (final Entry<ASTNode, Method> method : scopeFinder.methods
				.entries()) {
			scopes.put(new Scope(method.getKey().toString(),
					method.getValue().type, METHOD_CALL, 0, 0), method
					.getValue().name);
		}

		return scopes;

	}

	public static Multimap<Scope, String> getScopeSnippets(final File file,
			final boolean methodAsRoots) throws IOException {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return getScopeSnippets(ex.getAST(file), methodAsRoots);
	}

	public static Multimap<Scope, String> getScopeSnippets(final String code,
			final boolean methodAsRoots, final ParseType parseType) {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		return getScopeSnippets(ex.getAST(code, parseType), methodAsRoots);
	}
}
