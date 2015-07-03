package codemining.java.codedata.metrics;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.WhileStatement;

import codemining.java.codeutils.JavaASTExtractor;

/**
 * Compute McCabe's Cyclomatic Complexity.
 * 
 * @author Miltos Allamanis
 * 
 */
public class CyclomaticCalculator implements IFileMetricRetriever {

	/**
	 * Visit all "junctions" in an AST and increment complexity.
	 * 
	 */
	private static class JunctionVisitor extends ASTVisitor {
		int complexity = 0;

		@Override
		public boolean visit(final CatchClause arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final ConditionalExpression arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final DoStatement arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final EnhancedForStatement arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final ForStatement arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final IfStatement arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final MethodDeclaration arg0) {
			/*
			 * if (isConcrete(arg0)) { complexity.startMethod(); return
			 * super.visit(arg0); } return false;
			 */
			complexity++; // TODO: Not exactly true, but we'll use that
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final SwitchCase arg0) {
			complexity++;
			return super.visit(arg0);
		}

		@Override
		public boolean visit(final WhileStatement arg0) {
			complexity++;
			return super.visit(arg0);
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(CyclomaticCalculator.class.getName());

	public int getComplexity(final File file) throws IOException {
		final JavaASTExtractor ast = new JavaASTExtractor(false);
		final JunctionVisitor visitor = new JunctionVisitor();
		ast.getAST(file).accept(visitor);
		return visitor.complexity;
	}

	@Override
	public double getMetricForASTNode(final ASTNode node) {
		final JunctionVisitor visitor = new JunctionVisitor();
		node.accept(visitor);
		return visitor.complexity;
	}

	@Override
	public double getMetricForFile(final File file) throws IOException {
		return getComplexity(file);
	}
}
