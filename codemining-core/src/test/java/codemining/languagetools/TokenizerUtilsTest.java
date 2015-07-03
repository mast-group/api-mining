/**
 * 
 */
package codemining.languagetools;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import codemining.java.codeutils.JavaAstExtractorTest;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class TokenizerUtilsTest {

	private String classContent;

	@Before
	public void setUp() throws IOException {
		classContent = FileUtils.readFileToString(new File(
				JavaAstExtractorTest.class.getClassLoader()
						.getResource("SampleClass.txt").getFile()));
	}

	@Test
	public void testColumn() {
		assertEquals(TokenizerUtils.getColumnOfPosition(classContent, 970), 29);
		assertEquals(TokenizerUtils.getColumnOfPosition(classContent, 980), 13);
		assertEquals(TokenizerUtils.getColumnOfPosition(classContent, 1565), 17);
	}

}
