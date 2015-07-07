package apimining.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import apimining.java.ASTVisitors.FQImportVisitor;
import apimining.java.ASTVisitors.WildcardImportVisitor;

/**
 * Collector for wildcard namespaces
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 */
public class WildcardNamespaceCollector {

	private static final String libFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/java_libraries/";
	private static final String namespaceFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/namespaces/";

	private static final String[] projFolders = new String[] { "androidLocation", "androidWifi", "elasticsearch",
			"hadoop", "hibernate", "jgit", "jsoup", "lucene", "neo4j", "netty", "opennlp", "rabbitmq", "rhino",
			"spatial4j", "twitter4j" };
	private static final String[] packageNames = new String[] { "android.location", "android.net.wifi",
			"org.elasticsearch", "org.apache.hadoop", "org.hibernate", "org.eclipse.jgit", "org.jsoup",
			"org.apache.lucene", "org.neo4j", "io.netty", "opennlp", "com.rabbitmq", "org.mozilla.javascript",
			"com.spatial4j", "twitter4j" };

	public static void main(final String[] args) throws IOException {

		for (int i = 0; i < packageNames.length; i++) {
			System.out.println("===== Package " + projFolders[i] + ", namespace " + packageNames[i]);

			final List<File> files = (List<File>) FileUtils.listFiles(new File(libFolder + projFolders[i]),
					new String[] { "java" }, true);
			Collections.sort(files);

			final WildcardImportVisitor wiv = getWildcardImports(packageNames[i], files);

			writeNamespaces("class", wiv.wildcardImports, files);
			writeNamespaces("method", wiv.wildcardMethodImports, files);
		}

	}

	public static WildcardImportVisitor getWildcardImports(final String packageName, final List<File> files) {

		final WildcardImportVisitor wiv = new WildcardImportVisitor(packageName.replaceAll("\\.", "\\\\.") + ".*");

		for (final File file : files) {

			if (file.length() == 0)
				continue; // Ignore empty files

			wiv.process(ASTVisitors.getAST(file));
		}

		return wiv;
	}

	public static void writeNamespaces(final String type, final Set<String> namespaces, final List<File> files)
			throws IOException {
		if (!namespaces.isEmpty())
			System.out.println("Looking for " + type + " namespaces for: ");

		// Class namespaces
		for (final String namespace : namespaces) {
			System.out.println("      " + namespace);

			final FQImportVisitor fqiv = new FQImportVisitor(namespace.replaceAll("\\.", "\\\\.") + ".*");
			for (final File file : files) {

				if (file.length() == 0)
					continue; // Ignore empty files

				fqiv.process(ASTVisitors.getAST(file));
			}

			Set<String> fqImports;
			if (type.equals("class"))
				fqImports = fqiv.fqImports;
			else
				fqImports = fqiv.fqMethodImports;

			if (!fqImports.isEmpty()) {
				final PrintWriter out = new PrintWriter(new File(namespaceFolder + "/" + type + "/" + namespace),
						"UTF-8");
				for (final String fqName : fqImports)
					out.println(fqName);
				out.close();
			}
		}
	}

}
