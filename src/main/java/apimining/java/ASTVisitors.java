package apimining.java;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Container class for AST Visitors
 */
public class ASTVisitors {

	/**
	 * Visitor to find names of locally declared classes
	 *
	 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
	 */
	public static class ClassDeclarationVisitor extends ASTVisitor {

		public Set<String> decClasses = new HashSet<>();

		@Override
		public boolean visit(final TypeDeclaration node) {
			decClasses.add(node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			decClasses.add(node.getName().toString());
			return super.visit(node);
		}

	}

	public static Set<String> getDeclaredClasses(final CompilationUnit root) {
		final ClassDeclarationVisitor cdv = new ClassDeclarationVisitor();
		root.accept(cdv);
		return cdv.decClasses;
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
					return true; // look further for node with same length as
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

	private ASTVisitors() {
	}

}
