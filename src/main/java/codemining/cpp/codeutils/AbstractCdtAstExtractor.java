package codemining.cpp.codeutils;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;

import com.google.common.collect.Maps;

/**
 * Inteface for all classes that are able to retrieve a CDT-compatible AST.
 * Macros and inclusions are not resolved, unless in the same file.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public abstract class AbstractCdtAstExtractor {

	/**
	 * Return an AST for the following CDT-compatible code;
	 * 
	 * @param code
	 * @return
	 * @throws CoreException
	 */
	public final IASTTranslationUnit getAST(final char[] code,
			final String baseIncludePath) throws CoreException {
		final FileContent fc = FileContent.create(baseIncludePath, code);
		final Map<String, String> macroDefinitions = Maps.newHashMap();
		final String[] includeSearchPaths = new String[0];
		final IScannerInfo si = new ScannerInfo(macroDefinitions,
				includeSearchPaths);
		final IncludeFileContentProvider ifcp = IncludeFileContentProvider
				.getEmptyFilesProvider();
		final IIndex idx = null;
		final int options = ILanguage.OPTION_IS_SOURCE_UNIT;
		final IParserLogService log = new DefaultLogService();
		return getAstForLanguage(fc, si, ifcp, idx, options, log);
	}

	/**
	 * To be overrided for each language.
	 * 
	 * @param fc
	 * @param si
	 * @param ifcp
	 * @param idx
	 * @param options
	 * @param log
	 * @return
	 * @throws CoreException
	 */
	protected abstract IASTTranslationUnit getAstForLanguage(FileContent fc,
			IScannerInfo si, IncludeFileContentProvider ifcp, IIndex idx,
			int options, IParserLogService log) throws CoreException;

}