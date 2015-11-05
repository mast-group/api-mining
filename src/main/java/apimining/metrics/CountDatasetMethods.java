package apimining.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.LinkedListMultimap;

import apimining.java.APICallVisitor;
import apimining.java.ASTVisitors;

public class CountDatasetMethods {

	static final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/";
	private static final String exampleFolder = "/disk/data2/jfowkes/example_dataset/java_libraries_examples/";
	private static final String namespaceFolder = "/disk/data2/jfowkes/example_dataset/namespaces/";

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Settings

		final String[] projects = new String[] { "netty", "hadoop", "twitter4j", "mahout", "neo4j", "drools",
				"andengine", "spring-data-neo4j", "camel", "weld", "resteasy", "webobjects", "wicket",
				"restlet-framework-java", "cloud9", "hornetq", "spring-data-mongodb" };
		final String[] projFQNames = new String[] { "io.netty", "org.apache.hadoop", "twitter4j", "org.apache.mahout",
				"org.neo4j", "org.drools", "org.andengine", "org.springframework.data.neo4j", "org.apache.camel",
				"org.jboss.weld", "org.jboss.resteasy", "com.webobjects", "org.apache.wicket", "org.restlet",
				"edu.umd.cloud9", "org.hornetq", "org.springframework.data.mongodb" };

		int total = 0;
		for (int i = 0; i < projects.length; i++) {
			final int count = countDatasetMethods(projects[i]);
			total += count;
			System.out.println(projects[i] + " no. API methods: " + count);
		}
		System.out.println("Total no. API methods: " + total);

		int total2 = 0;
		for (int i = 0; i < projects.length; i++) {
			final int count = countExampleMethods(projects[i], projFQNames[i]);
			total2 += count;
			System.out.println(projects[i] + " no. example methods: " + count);
		}
		System.out.println("Total no. example methods: " + total2);

	}

	static int countDatasetMethods(final String project) throws IOException {
		final Set<String> callingMethods = new HashSet<>();
		final BufferedReader br = new BufferedReader(
				new FileReader(new File(baseFolder + "calls/" + project + ".arff")));
		boolean found = false;
		for (String line; (line = br.readLine()) != null;) {
			if (line.startsWith("@data"))
				found = true;
			else if (found) {
				callingMethods.add(line.split(",")[0]);
			}
		}
		br.close();
		return callingMethods.size();
	}

	static int countExampleMethods(final String project, final String projFQName) throws IOException {

		// Get all java files in source folder
		final Set<String> callingMethods = new HashSet<>();
		final List<File> files = (List<File>) FileUtils.listFiles(new File(exampleFolder + project),
				new String[] { "java" }, true);
		Collections.sort(files);

		for (final File file : files) {
			// Ignore empty files
			if (file.length() == 0)
				continue;

			final APICallVisitor acv = new APICallVisitor(ASTVisitors.getAST(file), namespaceFolder);
			acv.process();
			final LinkedListMultimap<String, String> fqAPICalls = acv.getAPINames(projFQName);
			for (final String fqCaller : fqAPICalls.keySet()) {
				callingMethods.add(fqCaller);
			}
		}

		return callingMethods.size();
	}

}
