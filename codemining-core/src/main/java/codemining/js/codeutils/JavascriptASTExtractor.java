/**
 * 
 */
package codemining.js.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

import codemining.languagetools.ParseType;

/**
 * A utility class to retrieve an Eclipse AST.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavascriptASTExtractor {

	private static final class TopFunctionRetriever extends ASTVisitor {
		public FunctionDeclaration topDcl;

		@Override
		public boolean visit(final FunctionDeclaration node) {
			topDcl = node;
			return false;
		}
	}

	/**
	 * Remembers if the given Extractor will calculate the bindings.
	 */
	private final boolean useBindings;

	private final boolean useJavadocs;

	/**
	 * Constructor.
	 * 
	 * @param useBindings
	 *            calculate bindings on the extracted AST.
	 */
	public JavascriptASTExtractor(final boolean useBindings) {
		this.useBindings = useBindings;
		useJavadocs = false;
	}

	public JavascriptASTExtractor(final boolean useBindings,
			final boolean useJavadocs) {
		this.useBindings = useBindings;
		this.useJavadocs = useJavadocs;
	}

	/**
	 * Get the AST of a file. It is assumed that a JavaScriptUnit will be
	 * returned. An heuristic is used to set the path variables.
	 * 
	 * @param file
	 * @return the compilation unit of the file
	 * @throws IOException
	 */
	public final JavaScriptUnit getAST(final File file) throws IOException {
		final String sourceFile = FileUtils.readFileToString(file);
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final Map<String, String> options = new Hashtable<String, String>();
		options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM,
				JavaScriptCore.VERSION_1_7);
		options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_7);
		if (useJavadocs) {
			options.put(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT,
					JavaScriptCore.ENABLED);
		}
		parser.setCompilerOptions(options);
		parser.setSource(sourceFile.toCharArray()); // set source
		parser.setResolveBindings(useBindings);
		parser.setBindingsRecovery(useBindings);

		parser.setStatementsRecovery(true);

		parser.setUnitName(file.getAbsolutePath());

		// FIXME Need file's project loaded into Eclipse to get bindings
		// which is only possible automatically if this were an Eclipse plugin
		// cf. https://bugs.eclipse.org/bugs/show_bug.cgi?id=206391
		// final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// final IProject project = root.getProject(projectName);
		// parser.setProject(JavaScriptCore.create(project));

		final JavaScriptUnit compilationUnit = (JavaScriptUnit) parser
				.createAST(null);

		return compilationUnit;
	}

	/**
	 * Get a compilation unit of the given file content.
	 * 
	 * @param fileContent
	 * @param parseType
	 * @return the compilation unit
	 */
	public final ASTNode getAST(final String fileContent,
			final ParseType parseType) {
		return (ASTNode) getASTNode(fileContent, parseType);
	}

	/**
	 * Return an ASTNode given the content
	 * 
	 * @param content
	 * @return
	 */
	public final ASTNode getASTNode(final char[] content,
			final ParseType parseType) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		final int astKind;
		switch (parseType) {
		case CLASS_BODY:
		case METHOD:
			astKind = ASTParser.K_CLASS_BODY_DECLARATIONS;
			break;
		case COMPILATION_UNIT:
			astKind = ASTParser.K_COMPILATION_UNIT;
			break;
		case EXPRESSION:
			astKind = ASTParser.K_EXPRESSION;
			break;
		case STATEMENTS:
			astKind = ASTParser.K_STATEMENTS;
			break;
		default:
			astKind = ASTParser.K_COMPILATION_UNIT;
		}
		parser.setKind(astKind);

		final Map<String, String> options = new Hashtable<String, String>();
		options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM,
				JavaScriptCore.VERSION_1_7);
		options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_7);
		if (useJavadocs) {
			options.put(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT,
					JavaScriptCore.ENABLED);
		}
		parser.setCompilerOptions(options);
		parser.setSource(content); // set source
		parser.setResolveBindings(useBindings);
		parser.setBindingsRecovery(useBindings);

		parser.setStatementsRecovery(true);

		if (parseType != ParseType.METHOD) {
			return parser.createAST(null);
		} else {
			final ASTNode cu = parser.createAST(null);
			return getFirstFunctionDeclaration(cu);
		}
	}

	/**
	 * Get the AST of a string. Path variables cannot be set.
	 * 
	 * @param file
	 * @param parseType
	 * @return an AST node for the given file content
	 * @throws IOException
	 */
	public final ASTNode getASTNode(final String fileContent,
			final ParseType parseType) {
		return getASTNode(fileContent.toCharArray(), parseType);
	}

	/**
	 * Get the AST and assume the type of the node is a compilation unit.
	 * 
	 * @throws Exception
	 */
	public final ASTNode getCompilationUnitAstNode(final char[] content)
			throws Exception {
		return getASTNode(content, ParseType.COMPILATION_UNIT);
	}

	/**
	 * Get the AST of a string. Path variables cannot be set.
	 * 
	 * @param file
	 * @return an AST node for the given file content
	 * @throws Exception
	 * @throws IOException
	 */
	public final ASTNode getCompilationUnitAstNode(final String fileContent)
			throws Exception {
		return getCompilationUnitAstNode(fileContent.toCharArray());
	}

	private final FunctionDeclaration getFirstFunctionDeclaration(
			final ASTNode node) {
		final TopFunctionRetriever visitor = new TopFunctionRetriever();
		node.accept(visitor);
		return visitor.topDcl;
	}

}
