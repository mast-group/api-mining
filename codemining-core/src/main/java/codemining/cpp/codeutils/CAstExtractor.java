/**
 * 
 */
package codemining.cpp.codeutils;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.core.runtime.CoreException;

/**
 * A C AST extractor.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class CAstExtractor extends AbstractCdtAstExtractor {

	@Override
	protected IASTTranslationUnit getAstForLanguage(final FileContent fc,
			final IScannerInfo si, final IncludeFileContentProvider ifcp,
			final IIndex idx, final int options, final IParserLogService log)
			throws CoreException {
		return GCCLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp,
				idx, options, log);
	}

}
