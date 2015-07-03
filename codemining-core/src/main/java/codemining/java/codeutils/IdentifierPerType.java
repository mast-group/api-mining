/**
 * 
 */
package codemining.java.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * Utility class for returning the identifiers of a file per type.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class IdentifierPerType {

	private static class MethodIdentifierExtractor extends ASTVisitor {
		final ASTNode cu;

		private final Map<String, RangeSet<Integer>> identifiers = Maps
				.newTreeMap();

		public MethodIdentifierExtractor(final char[] code) throws Exception {
			cu = (new JavaASTExtractor(false)).getBestEffortAstNode(new String(
					code));
			cu.accept(this);
		}

		public MethodIdentifierExtractor(final File f) throws IOException {
			cu = (new JavaASTExtractor(false)).getAST(f);
			cu.accept(this);
		}

		@Override
		public boolean visit(final MethodDeclaration node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final MethodInvocation node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

	}

	private static class TypeIdentifierExtractor extends ASTVisitor {
		final ASTNode cu;

		private final Map<String, RangeSet<Integer>> identifiers = Maps
				.newTreeMap();

		public TypeIdentifierExtractor(final char[] code) throws Exception {
			cu = (new JavaASTExtractor(false)).getBestEffortAstNode(new String(
					code));
			cu.accept(this);
		}

		public TypeIdentifierExtractor(final File f) throws IOException {
			cu = (new JavaASTExtractor(false)).getAST(f);
			cu.accept(this);
		}

		@Override
		public boolean visit(final ArrayType node) {
			addToMap(identifiers, node, node.toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final MethodDeclaration node) {
			final Collection<ASTNode> ex = node.thrownExceptions();
			for (final ASTNode n : ex) {
				addToMap(identifiers, n, n.toString());
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(final PrimitiveType node) {
			addToMap(identifiers, node, node.toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final SimpleType node) {
			addToMap(identifiers, node, node.toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final TypeDeclaration node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final TypeDeclarationStatement node) {
			addToMap(identifiers, node, node.getDeclaration().getName()
					.toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final TypeLiteral node) {
			addToMap(identifiers, node, node.getType().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final TypeParameter node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

	}

	private static class VariableIdentifierExtractor extends ASTVisitor {
		final ASTNode cu;

		private final Map<String, RangeSet<Integer>> identifiers = Maps
				.newTreeMap();

		public VariableIdentifierExtractor(final char[] code) throws Exception {
			cu = (new JavaASTExtractor(false)).getBestEffortAstNode(new String(
					code));
			cu.accept(this);
		}

		public VariableIdentifierExtractor(final File f) throws IOException {
			cu = (new JavaASTExtractor(false)).getAST(f);
			cu.accept(this);
		}

		@Override
		public boolean visit(final EnumConstantDeclaration node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final FieldAccess node) {
			final String fieldName = node.getName().toString();
			addToMap(identifiers, node, fieldName);
			return super.visit(node);
		}

		@Override
		public boolean visit(final ImportDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(final PackageDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(final QualifiedName node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final SingleVariableDeclaration node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final SuperFieldAccess node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

		@Override
		public boolean visit(final VariableDeclarationFragment node) {
			addToMap(identifiers, node, node.getName().toString());
			return super.visit(node);
		}

	}

	public static final void addToMap(
			final Map<String, RangeSet<Integer>> identifiers,
			final ASTNode node, final String identifier) {
		final int startPosition = node.getStartPosition();
		final Range<Integer> nodeRange = Range.closedOpen(startPosition,
				startPosition + node.getLength());

		RangeSet<Integer> idRanges = identifiers.get(identifier);
		if (idRanges == null) {
			idRanges = TreeRangeSet.create();
			identifiers.put(identifier, idRanges);
		}

		idRanges.add(nodeRange);
	}

	public static Set<String> getMethodIdentifiers(final char[] code)
			throws Exception {
		return getMethodIdentifiersRanges(code).keySet();
	}

	public static Set<String> getMethodIdentifiers(final File f)
			throws IOException {
		return getMethodIdentifiersRanges(f).keySet();
	}

	public static Map<String, RangeSet<Integer>> getMethodIdentifiersRanges(
			final char[] code) throws Exception {
		final MethodIdentifierExtractor ex = new MethodIdentifierExtractor(code);
		return ex.identifiers;
	}

	public static Map<String, RangeSet<Integer>> getMethodIdentifiersRanges(
			final File f) throws IOException {
		final MethodIdentifierExtractor ex = new MethodIdentifierExtractor(f);
		return ex.identifiers;
	}

	public static Set<String> getTypeIdentifiers(final char[] code)
			throws Exception {
		return getTypeIdentifiersRanges(code).keySet();
	}

	public static Set<String> getTypeIdentifiers(final File f)
			throws IOException {
		return getTypeIdentifiersRanges(f).keySet();
	}

	public static Map<String, RangeSet<Integer>> getTypeIdentifiersRanges(
			final char[] code) throws Exception {
		final TypeIdentifierExtractor ex = new TypeIdentifierExtractor(code);
		return ex.identifiers;
	}

	public static Map<String, RangeSet<Integer>> getTypeIdentifiersRanges(
			final File f) throws IOException {
		final TypeIdentifierExtractor ex = new TypeIdentifierExtractor(f);
		return ex.identifiers;
	}

	public static Set<String> getVariableIdentifiers(final char[] code)
			throws Exception {
		return getVariableIdentifiersRanges(code).keySet();
	}

	public static Set<String> getVariableIdentifiers(final File f)
			throws IOException {
		return getVariableIdentifiersRanges(f).keySet();
	}

	public static Map<String, RangeSet<Integer>> getVariableIdentifiersRanges(
			final char[] code) throws Exception {
		final VariableIdentifierExtractor ex = new VariableIdentifierExtractor(
				code);
		return ex.identifiers;
	}

	public static Map<String, RangeSet<Integer>> getVariableIdentifiersRanges(
			final File f) throws IOException {
		final VariableIdentifierExtractor ex = new VariableIdentifierExtractor(
				f);
		return ex.identifiers;
	}

	/**
	 * Private constructor for utility class
	 */
	private IdentifierPerType() {

	}

}
