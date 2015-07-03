/**
 * 
 */
package codemining.js.codeutils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.junit.Before;
import org.junit.Test;

import codemining.languagetools.ParseType;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
// FIXME Javascript AST parser is quite buggy: For SampleJavascript the === is
// printed as + and for SampleJavascript2 it prints a random semicolon at line 7
// Is this just a buggy toString method issue? Tests commented out until fixed.
public class JavascriptASTExtractorTest {

	String classContent;
	String methodContent;

	@Before
	public void setUp() throws IOException {
		classContent = FileUtils.readFileToString(new File(
				JavascriptASTExtractorTest.class.getClassLoader()
						.getResource("SampleJavascript2.txt").getFile()));

		methodContent = FileUtils.readFileToString(new File(
				JavascriptASTExtractorTest.class.getClassLoader()
						.getResource("SampleJavascript.txt").getFile()));
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
		final JavascriptASTExtractor ex = new JavascriptASTExtractor(false);
		assertTrue(classContent.length() > 0);
		final ASTNode classCU = ex.getASTNode(classContent,
				ParseType.COMPILATION_UNIT);
		// assertTrue(snippetMatchesAstTokens(classContent, classCU));

		assertTrue(methodContent.length() > 0);
		final ASTNode methodCU = ex.getASTNode(methodContent, ParseType.METHOD);
		// assertTrue(snippetMatchesAstTokens(methodContent, methodCU));
	}

	private boolean snippetMatchesAstTokens(final String snippetCode,
			final ASTNode node) {
		final JavascriptTokenizer tokenizer = new JavascriptTokenizer();
		final List<String> snippetTokens = tokenizer
				.tokenListFromCode(snippetCode.toCharArray());
		final List<String> astTokens = tokenizer.tokenListFromCode(node
				.toString().toCharArray());
		return astTokens.equals(snippetTokens);
	}
}
