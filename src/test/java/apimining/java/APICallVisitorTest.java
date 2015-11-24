package apimining.java;

import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.Test;

import com.google.common.collect.LinkedListMultimap;

public class APICallVisitorTest {

	@Test
	public void testAPICallVisitor() {

		final String namespaceFolder = "/disk/data2/jfowkes/example_dataset/namespaces/";
		final File file = new File(
				"/disk/data2/jfowkes/example_dataset/java_libraries/twitter4j/AccessTokenGenerator.java");

		final APICallVisitor acv = new APICallVisitor(ASTVisitors.getAST(file), namespaceFolder);
		acv.process();
		final LinkedListMultimap<String, String> fqAPICalls = acv.getAPINames("twitter4j");
		for (final Entry<String, Collection<String>> entry : fqAPICalls.asMap().entrySet()) {
			System.out.println("\nMethod " + entry.getKey() + ":");
			for (final String call : entry.getValue())
				System.out.println(call);
		}
	}

}
