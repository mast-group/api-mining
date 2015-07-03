package apimining.java;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;

import codemining.java.codeutils.JavaASTExtractor;

public class APICallExtractor {

	public static void main(final String[] args) {

		// Get all java files in source folder
		final File srcFolder = new File("/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/netty/");
		final List<File> files = (List<File>) FileUtils.listFiles(srcFolder, new String[] { "java" }, true);
		Collections.sort(files);

		int count = 0;
		for (final File file : files) {
			System.out.println("\nFile: " + file);

			// Ignore empty files
			if (file.length() == 0)
				continue;

			if (count % 50 == 0)
				System.out.println("At file " + count + " of " + files.size());
			count++;

			final APICallVisitor acv = new APICallVisitor();
			acv.process(getAST(file));

			for (final String fqName : new HashSet<>(acv.getAPINames()))
				if (!fqName.contains("java.") && !fqName.contains("LOCAL."))
					System.out.println(fqName);

			// System.out.println("Press any key to continue...");
			// try {
			// System.in.read();
			// } catch (final Exception e) {
			// }

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

}
