package codemining.js.codedata.metrics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

import codemining.js.codeutils.JavascriptASTExtractor;

public final class JavascriptFunctionCounter {

	public static class MethodClassCountVisitor extends ASTVisitor {

		public int noFunctions = 0;

		@Override
		public void postVisit(final ASTNode node) {

			if (node.getNodeType() == ASTNode.FUNCTION_DECLARATION)
				noFunctions++;
		}

	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage <inputDirectory>");
			System.exit(-1);
		}
		final File directory = new File(args[0]);
		countFunctions(directory);
	}

	public static void countFunctions(final File projectDir) throws IOException {

		System.out.println("\n===== Project " + projectDir);
		final MethodClassCountVisitor mccv = new MethodClassCountVisitor();
		final JavascriptASTExtractor astExtractor = new JavascriptASTExtractor(
				false);

		final List<File> files = (List<File>) FileUtils.listFiles(projectDir,
				new String[] { "js" }, true);

		int count = 0;
		for (final File file : files) {

			final JavaScriptUnit cu = astExtractor.getAST(file);
			cu.accept(mccv);

			if (count % 1000 == 0)
				System.out.println("At file " + count + " of " + files.size());
			count++;
		}

		System.out.println("Project " + projectDir);
		System.out.println("No. *.js files " + files.size());
		System.out.println("No. Functions: " + mccv.noFunctions);
	}

	private JavascriptFunctionCounter() {

	}

}
