/**
 *
 */
package codemining.java.codeutils;

import java.io.File;
import java.util.Collection;
import java.util.Stack;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codemining.java.tokenizers.JavaTokenizer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Retrieve all the methods contained in a given class.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class MethodsInClass {

	private class MethodExtractor extends ASTVisitor {

		Stack<String> className = new Stack<String>();

		private String currentPackageName;

		@Override
		public void endVisit(final EnumDeclaration node) {
			className.pop();
			super.endVisit(node);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core
		 * .dom.TypeDeclaration)
		 */
		@Override
		public void endVisit(final TypeDeclaration node) {
			className.pop();
			super.endVisit(node);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .CompilationUnit)
		 */
		@Override
		public boolean visit(final CompilationUnit node) {
			if (node.getPackage() != null) {
				currentPackageName = node.getPackage().getName()
						.getFullyQualifiedName();
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			if (className.isEmpty()) {
				className.push(currentPackageName + "."
						+ node.getName().getIdentifier());
			} else {
				className.push(className.peek() + "."
						+ node.getName().getIdentifier());
			}
			return super.visit(node);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .ImportDeclaration)
		 */
		@Override
		public boolean visit(final ImportDeclaration node) {
			// Don't visit. It's boring
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .MethodDeclaration)
		 */
		@Override
		public boolean visit(final MethodDeclaration node) {
			final String methodType = MethodUtils.getMethodType(node);
			methodsForClasses.put(className.peek(), node.getName()
					.getIdentifier() + ":" + methodType);
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .TypeDeclaration)
		 */
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

	public static void main(final String[] args) {
		if (args.length != 1) {
			System.err.println("Usage <projectDir>");
			System.exit(-1);
		}

		final MethodsInClass mic = new MethodsInClass();
		mic.scan(FileUtils
				.listFiles(new File(args[0]), JavaTokenizer.javaCodeFileFilter,
						DirectoryFileFilter.DIRECTORY));
		System.out.println(mic);
	}

	/**
	 * Class -> MethodName
	 */
	private final Multimap<String, String> methodsForClasses = HashMultimap
			.create();

	private static final Logger LOGGER = Logger.getLogger(MethodsInClass.class
			.getName());

	public MethodsInClass() {
		methodsForClasses.put("java.lang.Object", "toString:String()");
		methodsForClasses.put("java.lang.Object", "equals:boolean(Object,)");
		methodsForClasses.put("java.lang.Object", "hashCode:int()");
		methodsForClasses.put("java.lang.Runnable", "run:void()");
	}

	public Collection<String> getMethodsForClass(final String classname) {
		return methodsForClasses.get(classname);
	}

	public void scan(final Collection<File> files) {
		final MethodExtractor me = new MethodExtractor();
		final JavaASTExtractor jEx = new JavaASTExtractor(false);
		for (final File f : files) {
			try {
				final CompilationUnit cu = jEx.getAST(f);
				cu.accept(me);
			} catch (final Throwable e) {
				LOGGER.warning("Failed to get methods from " + f);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return methodsForClasses.toString();
	}

}
