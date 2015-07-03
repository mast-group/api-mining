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

public class JavaTypeBindingExtractorTest {

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
		final JavaTypeDeclarationBindingExtractor jame = new JavaTypeDeclarationBindingExtractor();

		final List<TokenNameBinding> classTypeindings = jame
				.getNameBindings(classContent);

		BindingTester.checkAllBindings(classTypeindings);
		assertEquals(classTypeindings.size(), 1);

		final List<TokenNameBinding> classTypeBindings2 = jame
				.getNameBindings(classContent2);
		BindingTester.checkAllBindings(classTypeBindings2);

		assertEquals(classTypeBindings2.size(), 1);
	}

}
