/**
 * 
 */
package codemining.cpp.codeutils;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.core.runtime.CoreException;

/**
 * A C++ AST Extractor.
 * 
 * For more look here
 * http://www.inf.unibz.it/~gsucci/publications/full%20text/full
 * %20text/OSS12.pdf
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class CppASTExtractor extends AbstractCdtAstExtractor {

	@Override
	protected IASTTranslationUnit getAstForLanguage(final FileContent fc,
			final IScannerInfo si, final IncludeFileContentProvider ifcp,
			final IIndex idx, final int options, final IParserLogService log)
			throws CoreException {
		return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp,
				idx, options, log);
	}
}
