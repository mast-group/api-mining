package apimining.java;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codemining.java.codeutils.JavaASTExtractor;

/**
 * Container class for AST Visitors
 */
public class ASTVisitors {

	/**
	 * Visitor to find fully qualified names of locally declared classes and
	 * methods
	 *
	 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
	 */
	public static class MethodClassDeclarationVisitor extends ASTVisitor {

		private String currentPackage = "";
		private final StringBuilder scopeName = new StringBuilder();
		public final Set<String> decClasses = new HashSet<>();
		public final Map<String, Type> methodReturnTypes = new HashMap<>();

		@Override
		public boolean visit(final PackageDeclaration node) {
			currentPackage = node.getName().getFullyQualifiedName();
			return false;
		}

		@Override
		public boolean visit(final TypeDeclaration node) {
			scopeName.append("." + node.getName().toString());
			decClasses.add(currentPackage + scopeName);
			return super.visit(node);
		}

		@Override
		public void endVisit(final TypeDeclaration node) {
			scopeName.delete(scopeName.lastIndexOf("."), scopeName.length());
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			scopeName.append("." + node.getName().toString());
			decClasses.add(currentPackage + scopeName);
			return super.visit(node);
		}

		@Override
		public void endVisit(final EnumDeclaration node) {
			scopeName.delete(scopeName.lastIndexOf("."), scopeName.length());
		}

		@Override
		public boolean visit(final MethodDeclaration node) {
			final String name = node.getName().toString();
			final Type returnType = node.getReturnType2();
			methodReturnTypes.put(currentPackage + scopeName + "." + name, returnType);
			return super.visit(node);
		}

		public void process(final CompilationUnit unit) {
			unit.accept(this);
		}

	}

	/**
	 * Visitor to find the parent block/class.
	 *
	 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
	 */
	public static class CoveringBlockFinderVisitor extends ASTVisitor {
		private final int fStart;
		private final int fEnd;
		private ASTNode fCoveringBlock;

		CoveringBlockFinderVisitor(final int start, final int length) {
			super(); // exclude Javadoc tags
			this.fStart = start;
			this.fEnd = start + length;
		}

		@Override
		public boolean visit(final Block node) {
			return findCoveringNode(node);
		}

		@Override
		public boolean visit(final TypeDeclaration node) {
			return findCoveringNode(node);
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			return findCoveringNode(node);
		}

		/**
		 * @see {@link org.eclipse.jdt.core.dom.NodeFinder.NodeFinderVisitor}
		 **/
		private boolean findCoveringNode(final ASTNode node) {
			final int nodeStart = node.getStartPosition();
			final int nodeEnd = nodeStart + node.getLength();
			if (nodeEnd < this.fStart || this.fEnd < nodeStart) {
				return false;
			}
			if (nodeStart <= this.fStart && this.fEnd <= nodeEnd) {
				this.fCoveringBlock = node;
			}
			if (this.fStart <= nodeStart && nodeEnd <= this.fEnd) {
				if (this.fCoveringBlock == node) { // nodeStart == fStart &&
													// nodeEnd == fEnd
					return true; // look further for node with same length
									// as
									// parent
				}
				return false;
			}
			return true;
		}

		/**
		 * Returns the covering Block/Class node. If more than one nodes are
		 * covering the selection, the returned node is last covering
		 * Block/Class node found in a top-down traversal of the AST
		 *
		 * @return Block/Class ASTNode
		 */
		public ASTNode getCoveringBlock() {
			return this.fCoveringBlock;
		}

	}

	/**
	 * Get covering Block/Class node, returning the root node if there is none
	 */
	public static ASTNode getCoveringBlock(final CompilationUnit root, final ASTNode node) {

		final CoveringBlockFinderVisitor finder = new CoveringBlockFinderVisitor(node.getStartPosition(),
				node.getLength());
		root.accept(finder);
		final ASTNode coveringBlock = finder.getCoveringBlock();

		if (coveringBlock != null)
			return coveringBlock;
		else
			return root;
	}

	/**
	 * Visitor to find wildcard imports from a given package
	 *
	 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
	 */
	public static class WildcardImportVisitor extends ASTVisitor {

		private final String pattern;
		public final Set<String> wildcardImports = new HashSet<>();
		public final Set<String> wildcardMethodImports = new HashSet<>();

		@Override
		public boolean visit(final ImportDeclaration node) {
			final String qName = node.getName().getFullyQualifiedName();
			final String imprt = node.toString().trim();
			if (!node.isStatic()) {
				if (imprt.endsWith(".*;") && qName.matches(pattern))
					wildcardImports.add(qName);
			} else {
				if (imprt.endsWith(".*;") && qName.matches(pattern))
					wildcardMethodImports.add(qName);
			}
			return false;
		}

		public WildcardImportVisitor(final String pattern) {
			this.pattern = pattern;
		}

		public void process(final CompilationUnit unit) {
			unit.accept(this);
		}

	}

	/**
	 * Visitor to find fully qualified imports matching a given pattern
	 *
	 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
	 */
	public static class FQImportVisitor extends ASTVisitor {

		private final String pattern;
		public final Set<String> fqImports = new HashSet<>();
		public final Set<String> fqMethodImports = new HashSet<>();

		@Override
		public boolean visit(final ImportDeclaration node) {
			final String qName = node.getName().getFullyQualifiedName();
			final String imprt = node.toString().trim();
			if (!node.isStatic()) {
				if (!imprt.endsWith(".*;") && qName.matches(pattern))
					fqImports.add(qName);
			} else {
				if (!imprt.endsWith(".*;") && qName.matches(pattern)) {
					final String name = qName.substring(qName.lastIndexOf('.') + 1);
					if (Character.isLowerCase(name.charAt(0)))
						fqMethodImports.add(qName);
				}
			}
			return false;
		}

		public FQImportVisitor(final String pattern) {
			this.pattern = pattern;
		}

		public void process(final CompilationUnit unit) {
			unit.accept(this);
		}

	}

	/**
	 * Get AST for source file
	 *
	 * @author Jaroslav Fowkes
	 */
	public static CompilationUnit getAST(final File fin) {

		CompilationUnit cu = null;
		final JavaASTExtractor ext = new JavaASTExtractor(false, true);
		try {
			cu = ext.getAST(fin);
		} catch (final Exception exc) {
			System.out.println("=+=+=+=+= AST Parse " + exc);
		}
		return cu;
	}

	private ASTVisitors() {
	}

}
