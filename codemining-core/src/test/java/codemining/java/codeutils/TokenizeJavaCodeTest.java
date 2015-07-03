package codemining.java.codeutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.junit.Test;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;

public class TokenizeJavaCodeTest {

	private static final char[] CODE_SAMPLE1 = "int x=2;".toCharArray();

	private static final String[] TOKENS_SAMPLE1 = { ITokenizer.SENTENCE_START,
			"int", "x", "=", "2", ";", ITokenizer.SENTENCE_END };
	private static final int[] TOKEN_POS_SAMPLE1 = { -1, 0, 4, 5, 6, 7,
			Integer.MAX_VALUE };

	private static final char[] CODE_SAMPLE2 = "if (y>0) {\n a += 2;\n}"
			.toCharArray();

	private static final String[] TOKENS_SAMPLE2 = { ITokenizer.SENTENCE_START,
			"if", "(", "y", ">", "0", ")", "{", "a", "+=", "2", ";", "}",
			ITokenizer.SENTENCE_END };

	public static final char[] CODE_SAMPLE3 = "int x=2; // this is a test\n"
			.toCharArray();

	@Test
	public void testSample1() {
		ITokenizer tokenizer = new JavaTokenizer();
		testSample1(tokenizer);
	}

	/**
	 * @param tokenizer
	 */
	protected void testSample1(ITokenizer tokenizer) {
		final List<String> tok = tokenizer.tokenListFromCode(CODE_SAMPLE1);
		for (int i = 0; i < TOKENS_SAMPLE1.length; i++) {
			assertEquals(tok.get(i), TOKENS_SAMPLE1[i]);
		}
		assertEquals(tok.size(), TOKENS_SAMPLE1.length);
	}

	/**
	 * @param tokenizer
	 */
	protected void testSample1Position(ITokenizer tokenizer) {
		final Map<Integer, String> toks = tokenizer
				.tokenListWithPos(CODE_SAMPLE1);
		for (int i = 0; i < TOKEN_POS_SAMPLE1.length; i++) {
			assertTrue(toks.containsKey(TOKEN_POS_SAMPLE1[i]));
			assertEquals(toks.get(TOKEN_POS_SAMPLE1[i]), TOKENS_SAMPLE1[i]);
		}
		assertEquals(toks.size(), TOKENS_SAMPLE1.length);
	}

	@Test
	public void testSample1postion() {
		ITokenizer tokenizer = new JavaTokenizer();
		testSample1Position(tokenizer);
	}

	@Test
	public void testSample2() {
		ITokenizer tokenizer = new JavaTokenizer();
		testSample2(tokenizer);
	}

	/**
	 * @param tokenizer
	 */
	protected void testSample2(ITokenizer tokenizer) {
		final List<String> tok = tokenizer.tokenListFromCode(CODE_SAMPLE2);

		for (int i = 0; i < TOKENS_SAMPLE2.length; i++) {
			assertEquals(tok.get(i), TOKENS_SAMPLE2[i]);
		}
	}

	@Test
	public void testSample3() {
		ITokenizer tokenizer = new JavaTokenizer();
		testSample3(tokenizer);
	}

	/**
	 * @param tokenizer
	 */
	protected void testSample3(ITokenizer tokenizer) {
		final List<String> tok = tokenizer.tokenListFromCode(CODE_SAMPLE3);
		for (int i = 0; i < TOKENS_SAMPLE1.length; i++) {
			assertEquals(tok.get(i), TOKENS_SAMPLE1[i]);
		}
		assertEquals(tok.size(), TOKENS_SAMPLE1.length);
	}

	@Test
	public void testTokenTypes() {
		ITokenizer tokenizer = new JavaTokenizer();
		assertEquals(
				tokenizer.getTokenFromString("hello"),
				new ITokenizer.FullToken("hello", tokenizer.getIdentifierType()));
		assertEquals(
				tokenizer.getTokenFromString("{"),
				new ITokenizer.FullToken("{", Integer
						.toString(ITerminalSymbols.TokenNameLBRACE)));

	}
}
