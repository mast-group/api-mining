/**
 * 
 */
package codemining.java.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import codemining.java.tokenizers.JavaTokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Given a Java file and a fully qualified name of a class, find those blocks
 * that use the class in question.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class UsagePointExtractor {

	private static final class UsageExtractor extends ASTVisitor {
		/**
		 * Return the imported class.
		 * 
		 * @param qName
		 * @return
		 */
		private static String getImportedClass(final String qName) {
			return qName.substring(qName.lastIndexOf('.') + 1);
		}

		final List<ASTNode> interestingNodes = Lists.newArrayList();

		final String fullyQualifiedName;
		final Set<String> className = Sets.newTreeSet();

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .ImportDeclaration)
		 */
		UsageExtractor(final String fullyQualifiedName) {
			this.fullyQualifiedName = fullyQualifiedName;
			// Add the fully qualified name in the rare case where
			// no import is needed (i.e. in java.lang.)
			className.add(fullyQualifiedName);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#preVisit2(org.eclipse.jdt.core
		 * .dom.ASTNode)
		 */
		@Override
		public boolean preVisit2(final ASTNode node) {
			return !interestingNodes.contains(node);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .FieldDeclaration)
		 */
		@Override
		public boolean visit(final FieldDeclaration node) {
			if (className.contains(node.getType().toString())) {
				interestingNodes.add(node.getParent());
			}
			return false;
		}

		@Override
		public boolean visit(final ImportDeclaration node) {
			final String qualifiedName = node.getName().getFullyQualifiedName();
			if (qualifiedName.startsWith(fullyQualifiedName)) {
				className.add(getImportedClass(qualifiedName));
				className.add(qualifiedName);
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .SingleVariableDeclaration)
		 */
		@Override
		public boolean visit(final SingleVariableDeclaration node) {
			if (className.contains(node.getType().toString())) {
				interestingNodes.add(node.getParent());
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .VariableDeclarationExpression)
		 */
		@Override
		public boolean visit(final VariableDeclarationExpression node) {
			if (className.contains(node.getType().toString())) {
				interestingNodes.add(node.getParent());
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .VariableDeclarationStatement)
		 */
		@Override
		public boolean visit(final VariableDeclarationStatement node) {
			if (className.contains(node.getType().toString())) {
				interestingNodes.add(node.getParent());
			}
			return false;
		}

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length != 2) {
			System.err.println("Usage <fullyQualifiedClass> <directory>");
			System.exit(-1);
		}

		final File directory = new File(args[1]);
		final String qualifiedClass = args[0];

		for (final File fi : FileUtils
				.listFiles(directory, JavaTokenizer.javaCodeFileFilter,
						DirectoryFileFilter.DIRECTORY)) {
			try {
				final List<ASTNode> usages = usagePoints(qualifiedClass, fi);
				if (!usages.isEmpty()) {
					System.out.println(fi.getAbsolutePath());
					for (final ASTNode node : usages) {
						System.out
								.println("----------------------------------------------");
						System.out.println(node);
					}
				}
			} catch (final Exception e) {
				System.err.println("Error processing " + fi.getName());
			}

		}

	}

	/**
	 * 
	 * @param qualifiedName
	 *            the fully qualified name of the class or the package
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static List<ASTNode> usagePoints(final String qualifiedName,
			final File f) throws IOException {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		final UsageExtractor usageExtractor = new UsageExtractor(qualifiedName);
		ex.getAST(f).accept(usageExtractor);
		return usageExtractor.interestingNodes;
	}

}
