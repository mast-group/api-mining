/**
 * 
 */
package codemining.java.codedata;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import codemining.java.codeutils.JavaASTExtractor;

import com.google.common.collect.Maps;

/**
 * A utility class that retrieves the methods (as AST Nodes) of a file.
 * 
 * @author Miltos Allamanis
 * 
 */
public final class MethodRetriever extends ASTVisitor {

	public static Map<String, MethodDeclaration> getMethodNodes(final File file)
			throws IOException {
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final MethodRetriever m = new MethodRetriever();
		final CompilationUnit cu = astExtractor.getAST(file);
		cu.accept(m);
		return m.methods;
	}

	public static Map<String, MethodDeclaration> getMethodNodes(
			final String file) throws Exception {
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final MethodRetriever m = new MethodRetriever();
		final ASTNode cu = astExtractor.getBestEffortAstNode(file);
		cu.accept(m);
		return m.methods;
	}

	private final Map<String, MethodDeclaration> methods = Maps.newTreeMap();

	private MethodRetriever() {

	}

	@Override
	public boolean visit(final MethodDeclaration node) {
		methods.put(node.getName().toString(), node);
		return super.visit(node);
	}

}
