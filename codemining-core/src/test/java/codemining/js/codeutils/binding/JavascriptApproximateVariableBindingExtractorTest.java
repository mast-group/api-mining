package codemining.js.codeutils.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import codemining.java.codeutils.binding.BindingTester;
import codemining.js.codeutils.JavascriptASTExtractorTest;
import codemining.languagetools.bindings.TokenNameBinding;

public class JavascriptApproximateVariableBindingExtractorTest {

	private static <T> void allAreContained(final Collection<T> collection,
			final Collection<T> in) {
		for (final T element : collection) {
			assertTrue(in.contains(element));
		}
	}

	File classContent;

	File classContent2;

	String methodContent;

	@Before
	public void setUp() throws IOException {
		classContent = new File(JavascriptASTExtractorTest.class
				.getClassLoader().getResource("SampleJavascript2.txt")
				.getFile());
		classContent2 = new File(JavascriptASTExtractorTest.class
				.getClassLoader().getResource("SampleJavascript3.txt")
				.getFile());

		methodContent = FileUtils.readFileToString(new File(
				JavascriptASTExtractorTest.class.getClassLoader()
						.getResource("SampleJavascript.txt").getFile()));
	}

	@Test
	public void testClassBindings() throws IOException {
		final JavascriptApproximateVariableBindingExtractor jabe = new JavascriptApproximateVariableBindingExtractor();
		final JavascriptExactVariableBindingsExtractor jbe = new JavascriptExactVariableBindingsExtractor();

		final List<TokenNameBinding> classVariableBindings = jabe
				.getNameBindings(classContent);
		final List<TokenNameBinding> classVariableBindingsExact = jbe
				.getNameBindings(classContent);

		BindingTester.checkAllBindings(classVariableBindings);
		assertEquals(classVariableBindings.size(), 3);

		final List<TokenNameBinding> classVariableBindings2 = jabe
				.getNameBindings(classContent2);
		final List<TokenNameBinding> classVariableBindings2Exact = jbe
				.getNameBindings(classContent2);

		assertEquals(classVariableBindings2.size(), 13);

		allAreContained(classVariableBindingsExact, classVariableBindings);
		allAreContained(classVariableBindings2Exact, classVariableBindings2);
	}

	@Test
	public void testMethodBinding() {
		final JavascriptApproximateVariableBindingExtractor jabe = new JavascriptApproximateVariableBindingExtractor();
		final List<TokenNameBinding> methodVariableBindings = jabe
				.getNameBindings(methodContent);
		BindingTester.checkAllBindings(methodVariableBindings);
		assertEquals(methodVariableBindings.size(), 1);

	}

}
