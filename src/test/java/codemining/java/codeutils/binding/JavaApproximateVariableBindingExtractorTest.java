package codemining.java.codeutils.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import codemining.java.codeutils.JavaAstExtractorTest;
import codemining.languagetools.bindings.TokenNameBinding;

public class JavaApproximateVariableBindingExtractorTest {

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
		classContent = new File(JavaAstExtractorTest.class.getClassLoader()
				.getResource("SampleClass.txt").getFile());
		classContent2 = new File(JavaAstExtractorTest.class.getClassLoader()
				.getResource("SampleClass2.txt").getFile());

		methodContent = FileUtils.readFileToString(new File(
				JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleMethod.txt").getFile()));
	}

	@Test
	public void testClassBindings() throws IOException {
		final JavaApproximateVariableBindingExtractor jabe = new JavaApproximateVariableBindingExtractor();
		final JavaExactVariableBindingsExtractor jbe = new JavaExactVariableBindingsExtractor();

		final List<TokenNameBinding> classVariableBindings = jabe
				.getNameBindings(classContent);
		final List<TokenNameBinding> classVariableBindingsExact = jbe
				.getNameBindings(classContent);

		BindingTester
				.checkAllBindings(classVariableBindings);
		assertEquals(classVariableBindings.size(), 5);

		final List<TokenNameBinding> classVariableBindings2 = jabe
				.getNameBindings(classContent2);
		final List<TokenNameBinding> classVariableBindings2Exact = jbe
				.getNameBindings(classContent2);

		assertEquals(classVariableBindings2.size(), 9);

		allAreContained(classVariableBindingsExact, classVariableBindings);
		allAreContained(classVariableBindings2Exact, classVariableBindings2);
	}

	@Test
	public void testMethodBinding() {
		final JavaApproximateVariableBindingExtractor jabe = new JavaApproximateVariableBindingExtractor();
		final List<TokenNameBinding> methodVariableBindings = jabe
				.getNameBindings(methodContent);
		BindingTester
				.checkAllBindings(methodVariableBindings);
		assertEquals(methodVariableBindings.size(), 3);

	}

}
