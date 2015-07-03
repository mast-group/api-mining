/**
 *
 */
package codemining.java.codeutils.binding;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import codemining.java.codeutils.JavaAstExtractorTest;
import codemining.languagetools.bindings.TokenNameBinding;

public class JavaMethodBindingExtractorTest {

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
	public void testClassLevelBindings() throws IOException {
		final JavaMethodInvocationBindingExtractor jame = new JavaMethodInvocationBindingExtractor();

		final List<TokenNameBinding> classMethodBindings = jame
				.getNameBindings(classContent);

		BindingTester.checkAllBindings(classMethodBindings);
		assertEquals(classMethodBindings.size(), 7);

		final List<TokenNameBinding> classMethodBindings2 = jame
				.getNameBindings(classContent2);
		BindingTester.checkAllBindings(classMethodBindings2);

		assertEquals(classMethodBindings2.size(), 6);
	}

}
