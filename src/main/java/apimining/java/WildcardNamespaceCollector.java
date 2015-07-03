package apimining.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import apimining.java.ASTVisitors.FQImportVisitor;
import apimining.java.ASTVisitors.WildcardImportVisitor;

/**
 * Collector for wildcard namespaces
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 */
public class WildcardNamespaceCollector {

	private static final File srcFolder = new File("/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/netty/");
	private static final File corpusFolder = new File("/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/netty/");
	private static final String namespaceFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/namespaces/";

	public static void main(final String[] args) throws IOException {

		// Get wildcarded imports
		final WildcardImportVisitor wiv = new WildcardImportVisitor("io\\.netty.*");

		final List<File> files = (List<File>) FileUtils.listFiles(srcFolder, new String[] { "java" }, true);
		Collections.sort(files);

		int count = 0;
		for (final File file : files) {

			if (file.length() == 0)
				continue; // Ignore empty files

			if (count % 50 == 0)
				System.out.println("At file " + count + " of " + files.size());
			count++;

			wiv.process(APICallExtractor.getAST(file));
		}

		// Get wildcarded namespaces
		for (final String namespace : wiv.wildcardImports) {

			final FQImportVisitor fqiv = new FQImportVisitor(namespace.replaceAll("\\.", "\\\\.") + ".*");

			final List<File> files2 = (List<File>) FileUtils.listFiles(corpusFolder, new String[] { "java" }, true);
			Collections.sort(files2);

			int count2 = 0;
			for (final File file : files2) {

				if (file.length() == 0)
					continue; // Ignore empty files

				if (count2 % 50 == 0)
					System.out.println("At file " + count2 + " of " + files2.size());
				count2++;

				fqiv.process(APICallExtractor.getAST(file));
			}

			final File outFile = new File(namespaceFolder + "/class/" + namespace);
			final PrintWriter out = new PrintWriter(outFile, "UTF-8");

			for (final String fqName : fqiv.fqImports)
				out.println(fqName);

			out.close();

		}

	}

}
