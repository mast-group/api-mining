/**
 * 
 */
package codemining.java.codeutils.scopes;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.languagetools.IScopeExtractor;
import codemining.languagetools.ParseType;
import codemining.languagetools.Scope;
import codemining.languagetools.Scope.ScopeType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Get all the class names (imports not included).
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class TypenameScopeExtractor {

	public static class ClassnameFinder extends ASTVisitor {

		private final boolean methodsAsRoot;

		final Multimap<ASTNode, String> types = HashMultimap.create();

		ASTNode topClass = null;

		ASTNode topMethod = null;

		public ClassnameFinder(final boolean useMethodsAsRoot) {
			methodsAsRoot = useMethodsAsRoot;
		}

		@Override
		public void endVisit(MethodDeclaration node) {
			topMethod = null;
			super.endVisit(node);
		}

		@Override
		public boolean visit(CastExpression node) {
			final String type = node.getType().toString();
			if (topMethod != null && methodsAsRoot) {
				types.put(topMethod, type);
			} else if (topClass != null) {
				types.put(topClass, type);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			final String type = node.getType().toString();
			if (topMethod != null && methodsAsRoot) {
				types.put(topMethod, type);
			} else if (topClass != null) {
				types.put(topClass, type);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			if (topClass == null) {
				topClass = node;
			}
			types.put(topClass, node.getName().getIdentifier());
			return super.visit(node);
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			final String type = node.getType().toString();
			if (topClass != null) {
				types.put(topClass, type);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			topMethod = node;
			return super.visit(node);
		}

		@Override
		public boolean visit(SingleVariableDeclaration node) {
			final String type = node.getType().toString();
			if (topMethod != null && methodsAsRoot) {
				types.put(topMethod, type);
			} else if (topClass != null) {
				types.put(topClass, type);
			}
			return false;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (topClass == null) {
				topClass = node;
			}
			types.put(topClass, node.getName().getIdentifier());
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeLiteral node) {
			final String type = node.getType().toString();
			if (topMethod != null && methodsAsRoot) {
				types.put(topMethod, type);
			} else if (topClass != null) {
				types.put(topClass, type);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclarationExpression node) {
			final String type = node.getType().toString();
			if (topMethod != null && methodsAsRoot) {
				types.put(topMethod, type);
			} else if (topClass != null) {
				types.put(topClass, type);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclarationStatement node) {
			final String type = node.getType().toString();
			if (topMethod != null && methodsAsRoot) {
				types.put(topMethod, type);
			} else if (topClass != null) {
				types.put(topClass, type);
			}
			return super.visit(node);
		}

	}

	public static final class TypenameSnippetExtractor implements
			IScopeExtractor {

		private final boolean methodsAsRoots;

		public TypenameSnippetExtractor(final boolean useMethodsAsRoots) {
			methodsAsRoots = useMethodsAsRoots;
		}

		private Multimap<Scope, String> getClassnames(final ASTNode node) {
			final ClassnameFinder cf = new ClassnameFinder(methodsAsRoots);
			node.accept(cf);

			final Multimap<Scope, String> classnames = TreeMultimap.create();

			for (final Entry<ASTNode, String> classname : cf.types.entries()) {
				final ASTNode parentNode = classname.getKey();
				final Scope sc = new Scope(
						classname.getKey().toString(),
						parentNode.getNodeType() == ASTNode.METHOD_DECLARATION ? ScopeType.SCOPE_METHOD
								: ScopeType.SCOPE_CLASS, TYPENAME,
						parentNode.getNodeType(), -1);
				classnames.put(sc, classname.getValue());
			}
			return classnames;
		}

		@Override
		public Multimap<Scope, String> getFromFile(final File file)
				throws IOException {
			final JavaASTExtractor ex = new JavaASTExtractor(false);
			return getClassnames(ex.getAST(file));
		}

		@Override
		public Multimap<Scope, String> getFromNode(final ASTNode node) {
			return getClassnames(node);
		}

		@Override
		public Multimap<Scope, String> getFromString(final String file,
				final ParseType parseType) {
			final JavaASTExtractor ex = new JavaASTExtractor(false);
			return getClassnames(ex.getAST(file, parseType));
		}

	}

	public static final String TYPENAME = "%typename%";

	private TypenameScopeExtractor() {
	}

}
