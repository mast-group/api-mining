/**
 * 
 */
package codemining.python.codeutils;

import org.python.pydev.parser.grammar27.PythonGrammar27TokenManager;
import org.python.pydev.parser.grammarcommon.ITokenManager;
import org.python.pydev.parser.jython.FastCharStream;

/**
 * A Python 2.7 tokenizer.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class Python27Tokenizer extends AbstractPythonTokenizer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * codemining.python.codeutils.AbstractPythonTokenizer#getPythonTokenizer
	 * (org.python.pydev.parser.jython.FastCharStream)
	 */
	@Override
	public ITokenManager getPythonTokenizer(FastCharStream stream) {
		final ITokenManager mng = new PythonGrammar27TokenManager(stream);
		return mng;
	}

}
