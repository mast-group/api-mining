/**
 * 
 */
package codemining.java.codeutils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Before;
import org.junit.Test;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ParseType;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavaAstExtractorTest {

	String classContent;
	String methodContent;

	@Before
	public void setUp() throws IOException {
		classContent = FileUtils.readFileToString(new File(
				JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleClass.txt").getFile()));

		methodContent = FileUtils.readFileToString(new File(
				JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleMethod.txt").getFile()));
	}

	/**
	 * Test method for
	 * {@link codemining.java.codeutils.JavaASTExtractor#getBestEffortAst(java.lang.String)}
	 * .
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetASTString() {
		final JavaASTExtractor ex = new JavaASTExtractor(false);
		assertTrue(classContent.length() > 0);
		final ASTNode classCU = ex.getASTNode(classContent,
				ParseType.COMPILATION_UNIT);
		assertTrue(snippetMatchesAstTokens(classContent, classCU));

		assertTrue(methodContent.length() > 0);
		final ASTNode methodCU = ex.getASTNode(methodContent,
				ParseType.METHOD);
		assertTrue(snippetMatchesAstTokens(methodContent, methodCU));
	}

	private boolean snippetMatchesAstTokens(final String snippetCode,
			final ASTNode node) {
		final JavaTokenizer tokenizer = new JavaTokenizer();
		final List<String> snippetTokens = tokenizer
				.tokenListFromCode(snippetCode.toCharArray());
		final List<String> astTokens = tokenizer.tokenListFromCode(node
				.toString().toCharArray());
		return astTokens.equals(snippetTokens);
	}
}
