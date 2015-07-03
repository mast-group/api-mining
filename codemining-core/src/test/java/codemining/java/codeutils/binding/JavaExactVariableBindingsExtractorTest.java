package codemining.java.codeutils.binding;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import codemining.java.codeutils.JavaAstExtractorTest;
import codemining.languagetools.bindings.TokenNameBinding;

public class JavaExactVariableBindingsExtractorTest {

	File classContent;

	File classContent2;

	@Before
	public void setUp() throws IOException {
		classContent = new File(JavaAstExtractorTest.class.getClassLoader()
				.getResource("SampleClass.txt").getFile());
		classContent2 = new File(JavaAstExtractorTest.class.getClassLoader()
				.getResource("SampleClass2.txt").getFile());
	}

	@Test
	public void testClassBindings() throws IOException {
		final JavaExactVariableBindingsExtractor jbe = new JavaExactVariableBindingsExtractor();
		final List<TokenNameBinding> classVariableBindings = jbe
				.getNameBindings(classContent);
		BindingTester.checkAllBindings(classVariableBindings);
		assertEquals(classVariableBindings.size(), 5);

		final List<TokenNameBinding> classVariableBindings2 = jbe
				.getNameBindings(classContent2);

		assertEquals(classVariableBindings2.size(), 9);
	}
}
