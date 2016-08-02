package apimining.java;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.Test;

import com.google.common.collect.LinkedListMultimap;

public class APICallVisitorTest {

	@Test
	public void testAPICallVisitor() throws IOException {

		final File file = getTestFile("AccessTokenGenerator.java");
		final APICallVisitor acv = new APICallVisitor(ASTVisitors.getAST(file), null);
		acv.process();
		final LinkedListMultimap<String, String> fqAPICalls = acv.getAPINames("twitter4j");
		for (final Entry<String, Collection<String>> entry : fqAPICalls.asMap().entrySet()) {
			System.out.println("\nMethod " + entry.getKey() + ":");
			for (final String call : entry.getValue())
				System.out.println(call);
		}
	}

	public File getTestFile(final String filename) throws UnsupportedEncodingException {
		final URL url = this.getClass().getClassLoader().getResource(filename);
		return new File(java.net.URLDecoder.decode(url.getPath(), "UTF-8"));
	}

}
