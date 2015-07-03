/**
 * 
 */
package codemining.python.codeutils;

import org.python.pydev.parser.grammar30.PythonGrammar30TokenManager;
import org.python.pydev.parser.grammarcommon.ITokenManager;
import org.python.pydev.parser.jython.FastCharStream;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class Python30Tokenizer extends AbstractPythonTokenizer  {

	private static final long serialVersionUID = 6944634686739086853L;

	/**
	 * @param stream
	 * @return
	 */
	@Override
	public ITokenManager getPythonTokenizer(final FastCharStream stream) {
		final ITokenManager mng = new PythonGrammar30TokenManager(stream);
		return mng;
	}
}
