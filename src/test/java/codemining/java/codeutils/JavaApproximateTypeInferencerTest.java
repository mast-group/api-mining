package codemining.java.codeutils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import codemining.languagetools.ParseType;

public class JavaApproximateTypeInferencerTest {

	String classContent;

	@Before
	public void setUp() throws IOException {
		classContent = FileUtils.readFileToString(new File(
				JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleClass3.txt").getFile()));
	}

	@Test
	public void test() {
		JavaASTExtractor ex = new JavaASTExtractor(false);
		JavaApproximateTypeInferencer jati = new JavaApproximateTypeInferencer(
				ex.getAST(classContent, ParseType.COMPILATION_UNIT));
		jati.infer();
		final Map<String, String> vars = jati.getVariableTypes();
		assertEquals(vars.get("anInstance"), "my.pack.SomeName");
		assertEquals(vars.get("arrayOfInt"), "int[]");
		assertEquals(vars.get("aNumber"), "long");
		assertEquals(vars.get("singleObject"), "your.pack.Blah");
		assertEquals(vars.get("arrayOfObjects"), "your.pack.Blah[]");
		assertEquals(vars.get("listOfInt"), "java.util.List<java.lang.Integer>");
		assertEquals(
				vars.get("complexParamType"),
				"java.util.Map<your.pack.Blah,java.util.Map<my.pack.SomeNameInPkg,java.util.List<java.lang.Double>>>");
		assertEquals(vars.get("paraType"),
				"your.pack2.ParamType<your.pack.Blah>");
		assertEquals(vars.get("lowerBoundPa"),
				"your.pack2.ParamType<? extends your.pack.Blah>");
		assertEquals(vars.get("upperBoundPa"),
				"your.pack2.ParamType<? super your.pack.Blah>");
		assertEquals(vars.get("upperBoundPa2"),
				"your.pack2.ParamType<? super java.util.List<? super your.pack.Blah>>");
		assertEquals(vars.get("e"),
				"java.io.IOException | java.lang.ArithmeticException");

	}
}
