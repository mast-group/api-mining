/**
 * 
 */
package codemining.js.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

import com.google.common.collect.Maps;

/**
 * A utility class that retrieves the methods (as AST Nodes) of a file.
 * 
 * @author Miltos Allamanis
 * 
 */
public final class FunctionRetriever extends ASTVisitor {

	public static Map<String, FunctionDeclaration> getFunctionNodes(
			final File file) throws IOException {
		final JavascriptASTExtractor astExtractor = new JavascriptASTExtractor(
				false);
		final FunctionRetriever m = new FunctionRetriever();
		final JavaScriptUnit cu = astExtractor.getAST(file);
		cu.accept(m);
		return m.functions;
	}

	public static Map<String, FunctionDeclaration> getFunctionNodes(
			final String file) throws Exception {
		final JavascriptASTExtractor astExtractor = new JavascriptASTExtractor(
				false);
		final FunctionRetriever m = new FunctionRetriever();
		final ASTNode cu = astExtractor.getCompilationUnitAstNode(file);
		cu.accept(m);
		return m.functions;
	}

	private final Map<String, FunctionDeclaration> functions = Maps
			.newTreeMap();

	private FunctionRetriever() {

	}

	@Override
	public boolean visit(final FunctionDeclaration node) {
		functions.put(node.getName().toString(), node);
		return super.visit(node);
	}

}
