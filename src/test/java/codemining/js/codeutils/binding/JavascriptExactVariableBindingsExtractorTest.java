package codemining.js.codeutils.binding;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import codemining.java.codeutils.binding.BindingTester;
import codemining.js.codeutils.JavascriptASTExtractorTest;
import codemining.languagetools.bindings.TokenNameBinding;

// FIXME Tests commented out until binding resolution is fixed
public class JavascriptExactVariableBindingsExtractorTest {

	File classContent;

	File classContent2;

	@Before
	public void setUp() throws IOException {
		classContent = new File(JavascriptASTExtractorTest.class
				.getClassLoader().getResource("SampleJavascript.txt").getFile());
		classContent2 = new File(JavascriptASTExtractorTest.class
				.getClassLoader().getResource("SampleJavascript2.txt")
				.getFile());
	}

	@Test
	public void testClassBindings() throws IOException {
		final JavascriptExactVariableBindingsExtractor jbe = new JavascriptExactVariableBindingsExtractor();
		final List<TokenNameBinding> classVariableBindings = jbe
				.getNameBindings(classContent);
		BindingTester.checkAllBindings(classVariableBindings);
		// assertEquals(classVariableBindings.size(), 1);

		final List<TokenNameBinding> classVariableBindings2 = jbe
				.getNameBindings(classContent2);

		// assertEquals(classVariableBindings2.size(), 3);
	}
}
