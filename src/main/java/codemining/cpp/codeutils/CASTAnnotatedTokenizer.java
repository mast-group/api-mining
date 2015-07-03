/**
 * 
 */
package codemining.cpp.codeutils;

import codemining.languagetools.ITokenizer;

/**
 * A C AST annotated tokenizer.
 * 
 * @author Miltos Allamanis<m.allamanis@ed.ac.uk>
 * 
 */
public class CASTAnnotatedTokenizer extends AbstractCdtASTAnnotatedTokenizer {

	private static final long serialVersionUID = 6395574519739472995L;

	/**
	 * @param extractorClass
	 */
	public CASTAnnotatedTokenizer() {
		super(CAstExtractor.class, "");
	}

	/**
	 * @param base
	 * @param extractorClass
	 */
	public CASTAnnotatedTokenizer(final ITokenizer base) {
		super(base, CAstExtractor.class, "");
	}

	public CASTAnnotatedTokenizer(final ITokenizer base,
			final String codeBasePath) {
		super(base, CAstExtractor.class, codeBasePath);
	}

	public CASTAnnotatedTokenizer(final String codeBasePath) {
		super(CAstExtractor.class, codeBasePath);
	}

}
