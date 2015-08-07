package apimining.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

	// private static final String libFolder =
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/library_dataset/java_libraries/";
	// private static final String namespaceFolder =
	// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/library_dataset/namespaces/";
	//
	// private static final String[] projFolders = new String[] {
	// "androidLocation", "androidWifi", "elasticsearch",
	// "hadoop", "hibernate", "jgit", "jsoup", "lucene", "neo4j", "netty",
	// "opennlp", "rabbitmq", "rhino",
	// "spatial4j", "twitter4j" };
	// private static final String[] packageNames = new String[] {
	// "android.location", "android.net.wifi",
	// "org.elasticsearch", "org.apache.hadoop", "org.hibernate",
	// "org.eclipse.jgit", "org.jsoup",
	// "org.apache.lucene", "org.neo4j", "io.netty", "opennlp", "com.rabbitmq",
	// "org.mozilla.javascript",
	// "com.spatial4j", "twitter4j" };

	private static final String libFolder = "/disk/data2/jfowkes/example_dataset/java_libraries/";
	private static final String exampleFolder = "/disk/data2/jfowkes/example_dataset/java_libraries_examples/";
	private static final String namespaceFolder = "/disk/data2/jfowkes/example_dataset/namespaces/";
	private static final String corpusFolder = "/disk/data1/mallamanis/java_projects/";

	private static final String[] projFolders = new String[] { "netty", "springside", "hadoop", "twitter4j", "mahout",
			"jersey", "neo4j", "drools", "andengine", "spring-data-neo4j", "camel", "weld", "jooq", "resteasy",
			"webobjects", "wicket", "maven-android-plugin", "restlet-framework-java", "cloud9", "hornetq",
			"spring-data-mongodb", "og-platform" };
	private static final String[] packageNames = new String[] { "io.netty", "org.springside", "org.apache.hadoop",
			"twitter4j", "org.apache.mahout", "org.glassfish.jersey", "org.neo4j", "org.drools", "org.andengine",
			"org.springframework.data.neo4j", "org.apache.camel", "org.jboss.weld", "org.jooq", "org.jboss.resteasy",
			"com.webobjects", "org.apache.wicket", "com.jayway.maven.plugins.android", "org.restlet", "edu.umd.cloud9",
			"org.hornetq", "org.springframework.data.mongodb", "com.opengamma" };
	private static final String[][] srcFolders = new String[][] { { "netty" }, { "springside4" },
			{ "hadoop-common", "hadoop-20", "hadoop", "hadoop-mapreduce" }, { "twitter4j_1" }, { "mahout" },
			{ "jersey" }, { "community" }, { "drools" }, { "AndEngine" }, { "spring-data-neo4j" }, { "camel" },
			{ "core" }, { "jOOQ" }, { "Resteasy" }, {}, { "wicket" }, { "maven-android-plugin" },
			{ "restlet-framework-java" }, { "Cloud9" }, { "hornetq" }, { "spring-data-mongodb" }, { "OG-Platform" } };

	public static void main(final String[] args) throws IOException {

		for (int i = 0; i < packageNames.length; i++) {
			System.out.println("===== Package " + projFolders[i] + ", namespace " + packageNames[i]);

			final List<File> files = (List<File>) FileUtils.listFiles(new File(libFolder + projFolders[i]),
					new String[] { "java" }, true);
			final List<File> filesEx = (List<File>) FileUtils.listFiles(new File(exampleFolder + projFolders[i]),
					new String[] { "java" }, true);
			files.addAll(filesEx);
			Collections.sort(files);

			final WildcardImportVisitor wiv = getWildcardImports(packageNames[i], files);
			final List<File> filesSrc = new ArrayList<>();
			for (final String srcDir : srcFolders[i])
				filesSrc.addAll((List<File>) FileUtils.listFiles(new File(corpusFolder + srcDir),
						new String[] { "java" }, true));
			files.addAll(filesSrc);
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
