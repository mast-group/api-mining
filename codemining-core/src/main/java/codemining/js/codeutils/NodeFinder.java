/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package codemining.js.codeutils;

import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BlockComment;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.CharacterLiteral;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.FunctionRefParameter;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.InferredType;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.LineComment;
import org.eclipse.wst.jsdt.core.dom.ListExpression;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteralField;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.RegularExpressionLiteral;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TextElement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.TypeLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;

/**
 * For a given range, finds the covered node and the covering node. Ported to
 * JavaScript by Jaroslav Fowkes.
 * 
 * @since 3.5
 */
public final class NodeFinder {
	/**
	 * This class defines the actual visitor that finds the node.
	 */
	private static class NodeFinderVisitor extends ASTVisitor {
		private final int fStart;
		private final int fEnd;
		private ASTNode fCoveringNode;
		private ASTNode fCoveredNode;

		NodeFinderVisitor(final int offset, final int length) {
			super(true); // include Javadoc tags
			this.fStart = offset;
			this.fEnd = offset + length;
		}

		public boolean visitNode(final ASTNode node) {
			final int nodeStart = node.getStartPosition();
			final int nodeEnd = nodeStart + node.getLength();
			if (nodeEnd < this.fStart || this.fEnd < nodeStart) {
				return false;
			}
			if (nodeStart <= this.fStart && this.fEnd <= nodeEnd) {
				this.fCoveringNode = node;
			}
			if (this.fStart <= nodeStart && nodeEnd <= this.fEnd) {
				if (this.fCoveringNode == node) { // nodeStart == fStart &&
													// nodeEnd == fEnd
					this.fCoveredNode = node;
					return true; // look further for node with same length as
									// parent
				} else if (this.fCoveredNode == null) { // no better found
					this.fCoveredNode = node;
				}
				return false;
			}
			return true;
		}

		/**
		 * Returns the covered node. If more than one nodes are covered by the
		 * selection, the returned node is first covered node found in a
		 * top-down traversal of the AST
		 * 
		 * @return ASTNode
		 */
		public ASTNode getCoveredNode() {
			return this.fCoveredNode;
		}

		/**
		 * Returns the covering node. If more than one nodes are covering the
		 * selection, the returned node is last covering node found in a
		 * top-down traversal of the AST
		 * 
		 * @return ASTNode
		 */
		public ASTNode getCoveringNode() {
			return this.fCoveringNode;
		}

		/*
		 * Call visitNode on each type-specific visitor. This is ugly but
		 * unavoidable as the Javascript ASTVisitor doesn't implement boolean
		 * preVisit2(ASTNode node) and visiting every ASTNode with preVisit() is
		 * far too slow.
		 */

		@Override
		public boolean visit(final AnonymousClassDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ArrayAccess node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ArrayCreation node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ArrayInitializer node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ArrayType node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final Assignment node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final Block node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final BlockComment node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final BooleanLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final BreakStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final CatchClause node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final CharacterLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final RegularExpressionLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ClassInstanceCreation node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final JavaScriptUnit node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ConditionalExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ConstructorInvocation node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ContinueStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final DoStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final EmptyStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final EnhancedForStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ExpressionStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FieldAccess node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FieldDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ForStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ForInStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final IfStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ImportDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final InferredType node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final InfixExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final InstanceofExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final Initializer node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final JSdoc node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final LabeledStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final LineComment node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ListExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final MemberRef node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FunctionRef node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FunctionRefParameter node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FunctionDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FunctionInvocation node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final Modifier node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final NullLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final UndefinedLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final NumberLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final PackageDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ParenthesizedExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final PostfixExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final PrefixExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final PrimitiveType node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final QualifiedName node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final QualifiedType node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ReturnStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SimpleName node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SimpleType node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SingleVariableDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final StringLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SuperConstructorInvocation node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SuperFieldAccess node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SuperMethodInvocation node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SwitchCase node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final SwitchStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final TagElement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final TextElement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ThisExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ThrowStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final TryStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final TypeDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final TypeDeclarationStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final TypeLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final VariableDeclarationExpression node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final VariableDeclarationStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final VariableDeclarationFragment node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final WhileStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final WithStatement node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ObjectLiteral node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final ObjectLiteralField node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(final FunctionExpression node) {
			return visitNode(node);
		}

	}

	/**
	 * Maps a selection to a given ASTNode, where the selection is defined using
	 * a start and a length. The result node is determined as follows:
	 * <ul>
	 * <li>first the visitor tries to find a node with the exact
	 * <code>start</code> and <code>length</code></li>
	 * <li>if no such node exists then the node that encloses the range defined
	 * by <code>start</code> and <code>length</code> is returned.</li>
	 * <li>if the length is zero then also nodes are considered where the node's
	 * start or end position matches <code>start</code>.</li>
	 * <li>otherwise <code>null</code> is returned.</li>
	 * </ul>
	 * 
	 * @param root
	 *            the root node from which the search starts
	 * @param start
	 *            the given start
	 * @param length
	 *            the given length
	 * 
	 * @return the found node
	 */
	public static ASTNode perform(final ASTNode root, final int start,
			final int length) {
		final NodeFinder finder = new NodeFinder(root, start, length);
		final ASTNode result = finder.getCoveredNode();
		if (result == null || result.getStartPosition() != start
				|| result.getLength() != length) {
			return finder.getCoveringNode();
		}
		return result;
	}

	/**
	 * Maps a selection to a given ASTNode, where the selection is defined using
	 * a source range. It calls
	 * <code>perform(root, range.getOffset(), range.getLength())</code>.
	 * 
	 * @return the result node
	 * @see #perform(ASTNode, int, int)
	 */
	public static ASTNode perform(final ASTNode root, final ISourceRange range) {
		return perform(root, range.getOffset(), range.getLength());
	}

	/**
	 * Maps a selection to a given ASTNode, where the selection is given by a
	 * start and a length. The result node is determined as follows:
	 * <ul>
	 * <li>first the visitor tries to find a node that is covered by
	 * <code>start</code> and <code>length</code> where either
	 * <code>start</code> and <code>length</code> exactly matches the node or
	 * where the text covered before and after the node only consists of white
	 * spaces or comments.</li>
	 * <li>if no such node exists then the node that encloses the range defined
	 * by <code>start</code> and <code>length</code> is returned.</li>
	 * <li>if the length is zero then also nodes are considered where the node's
	 * start or end position matches <code>start</code>.</li>
	 * <li>otherwise <code>null</code> is returned.</li>
	 * </ul>
	 * 
	 * @param root
	 *            the root node from which the search starts
	 * @param start
	 *            the given start
	 * @param length
	 *            the given length
	 * @param source
	 *            the source of the compilation unit
	 * 
	 * @return the result node
	 * @throws JavaScriptModelException
	 *             if an error occurs in the JavaScript model
	 */
	public static ASTNode perform(final ASTNode root, final int start,
			final int length, final ITypeRoot source)
			throws JavaScriptModelException {
		final NodeFinder finder = new NodeFinder(root, start, length);
		final ASTNode result = finder.getCoveredNode();
		if (result == null)
			return null;
		final int nodeStart = result.getStartPosition();
		if (start <= nodeStart
				&& ((nodeStart + result.getLength()) <= (start + length))) {
			final IBuffer buffer = source.getBuffer();
			if (buffer != null) {
				final IScanner scanner = ToolFactory.createScanner(false,
						false, false, false);
				try {
					scanner.setSource(buffer.getText(start, length)
							.toCharArray());
					int token = scanner.getNextToken();
					if (token != ITerminalSymbols.TokenNameEOF) {
						final int tStart = scanner
								.getCurrentTokenStartPosition();
						if (tStart == result.getStartPosition() - start) {
							scanner.resetTo(tStart + result.getLength(),
									length - 1);
							token = scanner.getNextToken();
							if (token == ITerminalSymbols.TokenNameEOF)
								return result;
						}
					}
				} catch (final InvalidInputException e) {
					// ignore
				} catch (final IndexOutOfBoundsException e) {
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=305001
					return null;
				}
			}
		}
		return finder.getCoveringNode();
	}

	private final ASTNode fCoveringNode;
	private final ASTNode fCoveredNode;

	/**
	 * Instantiate a new node finder using the given root node, the given start
	 * and the given length.
	 * 
	 * @param root
	 *            the given root node
	 * @param start
	 *            the given start
	 * @param length
	 *            the given length
	 */
	public NodeFinder(final ASTNode root, final int start, final int length) {
		final NodeFinderVisitor nodeFinderVisitor = new NodeFinderVisitor(
				start, length);
		root.accept(nodeFinderVisitor);
		this.fCoveredNode = nodeFinderVisitor.getCoveredNode();
		this.fCoveringNode = nodeFinderVisitor.getCoveringNode();
	}

	/**
	 * Returns the covered node. If more than one nodes are covered by the
	 * selection, the returned node is first covered node found in a top-down
	 * traversal of the AST.
	 * 
	 * @return the covered node
	 */
	public ASTNode getCoveredNode() {
		return this.fCoveredNode;
	}

	/**
	 * Returns the covering node. If more than one nodes are covering the
	 * selection, the returned node is last covering node found in a top-down
	 * traversal of the AST.
	 * 
	 * @return the covering node
	 */
	public ASTNode getCoveringNode() {
		return this.fCoveringNode;
	}
}
