package apimining.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.LinkedListMultimap;

/**
 * Extract API calls into ARF Format. Attributes are fqCaller and fqCalls as
 * space separated string of API calls.
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 */
public class APICallExtractor {

	private static final String libFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/java_libraries/";
	private static final String namespaceFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/namespaces/";

	private static final String[] projFolders = new String[] { "androidLocation", "androidWifi", "elasticsearch",
			"hadoop", "hibernate", "jgit", "jsoup", "lucene", "neo4j", "netty", "opennlp", "rabbitmq", "rhino",
			"spatial4j", "twitter4j" };
	private static final String[] packageNames = new String[] { "android.location", "android.net.wifi",
			"org.elasticsearch", "org.apache.hadoop", "org.hibernate", "org.eclipse.jgit", "org.jsoup",
			"org.apache.lucene", "org.neo4j", "io.netty", "opennlp", "com.rabbitmq", "org.mozilla.javascript",
			"com.spatial4j", "twitter4j" };

	private static final String outFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/srclibs/calls/";

	public static void main(final String[] args) throws IOException {

		// For each java file in project
		for (int i = 0; i < packageNames.length; i++) {

			System.out.println("===== Processing " + projFolders[i]);

			final PrintWriter out = new PrintWriter(new File(outFolder + projFolders[i] + ".arff"), "UTF-8");

			// ARF Header
			out.println("@relation " + projFolders[i]);
			out.println();
			out.println("@attribute fqCaller string");
			out.println("@attribute fqCalls string");
			out.println();
			out.println("@data");

			// Get all java files in source folder
			final List<File> files = (List<File>) FileUtils.listFiles(new File(libFolder + projFolders[i]),
					new String[] { "java" }, true);
			Collections.sort(files);

			int count = 0;
			for (final File file : files) {
				// if (!file.getName().contains("TestSirenNumericRange"))
				// continue;

				System.out.println("\nFile: " + file);

				// Ignore empty files
				if (file.length() == 0)
					continue;

				if (count % 50 == 0)
					System.out.println("At file " + count + " of " + files.size());
				count++;

				final APICallVisitor acv = new APICallVisitor(ASTVisitors.getAST(file), namespaceFolder);
				acv.process();
				final LinkedListMultimap<String, String> fqAPICalls = acv.getAPINames(packageNames[i]);

				for (final String fqCaller : fqAPICalls.keySet()) {
					out.print("'" + fqCaller + "','");
					String prefix = "";
					for (final String fqCall : fqAPICalls.get(fqCaller)) {
						out.print(prefix + fqCall);
						prefix = " ";
					}
					out.println("'");
				}

			}

			out.close();
		}
	}

}
