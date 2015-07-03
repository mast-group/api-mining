package codemining.java.codedata.metrics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import codemining.java.codeutils.JavaASTExtractor;

public final class JavaMethodClassCounter {

	public static class MethodClassCountVisitor extends ASTVisitor {

		public int noMethods = 0;
		public int noClasses = 0;

		@Override
		public void postVisit(final ASTNode node) {

			if (node.getNodeType() == ASTNode.METHOD_DECLARATION)
				noMethods++;

			if (node.getNodeType() == ASTNode.TYPE_DECLARATION
					|| node.getNodeType() == ASTNode.ENUM_DECLARATION)
				noClasses++;
		}

	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage <inputDirectory>");
			System.exit(-1);
		}
		final File directory = new File(args[0]);
		countMethodsClasses(directory);
	}

	public static void countMethodsClasses(final File projectDir)
			throws IOException {

		System.out.println("\n===== Project " + projectDir);
		final MethodClassCountVisitor mccv = new MethodClassCountVisitor();
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);

		final List<File> files = (List<File>) FileUtils.listFiles(projectDir,
				new String[] { "java" }, true);

		int count = 0;
		for (final File file : files) {

			final CompilationUnit cu = astExtractor.getAST(file);
			cu.accept(mccv);

			if (count % 1000 == 0)
				System.out.println("At file " + count + " of " + files.size());
			count++;
		}

		System.out.println("Project " + projectDir);
		System.out.println("No. *.java files " + files.size());
		System.out.println("No. Methods: " + mccv.noMethods);
		System.out.println("No. Classes: " + mccv.noClasses);
	}

	private JavaMethodClassCounter() {

	}

}
