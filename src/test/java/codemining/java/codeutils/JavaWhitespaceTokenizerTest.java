/**
 * 
 */
package codemining.java.codeutils;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import codemining.java.tokenizers.JavaWhitespaceTokenizer;

import com.google.common.collect.Lists;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavaWhitespaceTokenizerTest {

	private String code;
	private List<String> correctTokens;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		code = FileUtils.readFileToString(new File(
				JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleClass2.txt").getFile()));

		correctTokens = Lists.newArrayList(FileUtils.readFileToString(
				new File(JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleClass2WhitespaceTokens.txt")
						.getFile())).split("\n"));
	}

	@Test
	public void test() {
		final JavaWhitespaceTokenizer tokenizer = new JavaWhitespaceTokenizer();
		final List<String> tokens = tokenizer.tokenListFromCode(code
				.toCharArray());
		for (int i = 0; i < correctTokens.size(); i++) {
			assertEquals("Does not match at position " + i, tokens.get(i),
					correctTokens.get(i));
		}
	}
}
