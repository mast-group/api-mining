/**
 *
 */
package apimining.java;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WildcardType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Added minor extension to handle <?> - Jaroslav Fowkes
 *
 * Perform approximate type inference, assigning the type to all local fields
 * and variables. This approximation does not resolve inherited field types and
 * fields of the form this.name
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 */
public class JavaApproximateTypeInferencer extends ASTVisitor {
	private int nextDeclarId = 0;

	/** Root node of current AST */
	protected final CompilationUnit rootNode;

	protected String currentPackage = "";

	/**
	 * A hash map between classNames and their respective packages
	 */
	protected final Map<String, String> importedNames = Maps.newTreeMap();

	/**
	 * Map the names that are defined in each AST node, with their respective
	 * ids.
	 */
	protected final Map<ASTNode, Map<String, Integer>> variableNames = Maps.newIdentityHashMap();

	/**
	 * Map of variables (represented with their ids) to all token positions
	 * where the variable is referenced.
	 */
	Map<Integer, List<ASTNode>> variableBindings = Maps.newTreeMap();

	/**
	 * Contains the types of the variables at each scope.
	 */
	Map<Integer, String> variableTypes = Maps.newTreeMap();

	/**
	 * Add the binding to the current scope.
	 *
	 * @param scopeBindings
	 * @param name
	 */
	private void addBinding(final ASTNode node, final String name, final Type type) {
		final int bindingId = nextDeclarId;
		nextDeclarId++;
		variableNames.get(node).put(name, bindingId);
		variableNames.get(node.getParent()).put(name, bindingId);
		variableBindings.put(bindingId, Lists.<ASTNode> newArrayList());
		final String nameOfType = getNameOfType(type);
		variableTypes.put(bindingId, nameOfType);
	}

	/**
	 * Add the binding data for the given name at the given scope and position.
	 */
	private void addBindingData(final String name, final ASTNode nameNode, final Map<String, Integer> scopeBindings) {
		// Get varId or abort
		final Integer variableId = scopeBindings.get(name);
		if (variableId == null || !variableBindings.containsKey(variableId)) {
			return;
		}
		variableBindings.get(variableId).add(nameNode);
	}

	protected String getFullyQualifiedNameFor(final String className) {
		if (importedNames.containsKey(className)) {
			return importedNames.get(className);
		} else {
			try {
				return Class.forName("java.lang." + className).getName();
			} catch (final ClassNotFoundException e) {
				// Non a java lang class, thus it's in current package
			}
		}
		return currentPackage + "." + className;
	}

	/**
	 * @param type
	 * @return
	 */
	protected String getNameOfType(final Type type) {
		final String nameOfType;
		if (type.isPrimitiveType()) {
			nameOfType = type.toString();
		} else if (type.isParameterizedType()) {
			nameOfType = getParametrizedType((ParameterizedType) type);
		} else if (type.isArrayType()) {
			final ArrayType array = (ArrayType) type;
			nameOfType = getNameOfType(array.getElementType()) + "[]";
		} else if (type.isUnionType()) {
			final UnionType uType = (UnionType) type;
			final StringBuffer sb = new StringBuffer();
			for (final Object unionedType : uType.types()) {
				sb.append(getNameOfType(((Type) unionedType)));
				sb.append(" | ");
			}
			sb.delete(sb.length() - 3, sb.length());
			nameOfType = sb.toString();
		} else if (type.isWildcardType()) {
			final WildcardType wType = (WildcardType) type;
			if (wType.getBound() == null)
				return "?";
			nameOfType = (wType.isUpperBound() ? "? extends " : "? super ") + getNameOfType(wType.getBound());
		} else {
			nameOfType = getFullyQualifiedNameFor(type.toString());
		}
		return nameOfType;
	}

	/**
	 * @param type
	 * @return
	 */
	private String getParametrizedType(final ParameterizedType type) {
		final StringBuffer sb = new StringBuffer(getFullyQualifiedNameFor(type.getType().toString()));
		sb.append("<");
		for (final Object typeArg : type.typeArguments()) {
			final Type arg = (Type) typeArg;
			final String argString = getNameOfType(arg);
			sb.append(argString);
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(">");
		return sb.toString();
	}

	@Override
	public void preVisit(final ASTNode node) {
		final ASTNode parent = node.getParent();
		if (parent != null && variableNames.containsKey(parent)) {
			// inherit all variables in parent scope
			final Map<String, Integer> bindingsCopy = Maps.newTreeMap();
			for (final Entry<String, Integer> binding : variableNames.get(parent).entrySet()) {
				bindingsCopy.put(binding.getKey(), binding.getValue());
			}

			variableNames.put(node, bindingsCopy);
		} else {
			// Start from scratch
			variableNames.put(node, Maps.<String, Integer> newTreeMap());
		}
		super.preVisit(node);
	}

	/**
	 * Looks for field declarations (i.e. class member variables).
	 */
	@Override
	public boolean visit(final FieldDeclaration node) {
		for (final Object fragment : node.fragments()) {
			final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
			addBinding(node, frag.getName().getIdentifier(), node.getType());
		}
		return true;
	}

	@Override
	public boolean visit(final ImportDeclaration node) {
		if (!node.isStatic()) {
			final String qName = node.getName().getFullyQualifiedName();
			importedNames.put(qName.substring(qName.lastIndexOf('.') + 1), qName);
		}
		return false;
	}

	@Override
	public boolean visit(final PackageDeclaration node) {
		currentPackage = node.getName().getFullyQualifiedName();
		return false;
	}

	/**
	 * Visits {@link SimpleName} AST nodes. Resolves the binding of the simple
	 * name and looks for it in the {@link #variableScope} map. If the binding
	 * is found, this is a reference to a variable.
	 *
	 * @param node
	 *            the node to visit
	 */
	@Override
	public boolean visit(final SimpleName node) {
		if (node.getParent().getNodeType() == ASTNode.METHOD_INVOCATION) {
			final MethodInvocation invocation = (MethodInvocation) node.getParent();
			if (invocation.getName() == node) {
				return true;
			}
		}
		addBindingData(node.getIdentifier(), node, variableNames.get(node));
		return true;
	}

	/**
	 * Looks for Method Parameters.
	 */
	@Override
	public boolean visit(final SingleVariableDeclaration node) {
		addBinding(node, node.getName().getIdentifier(), node.getType());
		return true;
	}

	/**
	 * Looks for variables declared in for loops.
	 */
	@Override
	public boolean visit(final VariableDeclarationExpression node) {
		for (final Object fragment : node.fragments()) {
			final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
			addBinding(node, frag.getName().getIdentifier(), node.getType());
		}
		return true;
	}

	/**
	 * Looks for local variable declarations. For every declaration of a
	 * variable, the parent {@link Block} denoting the variable's scope is
	 * stored in {@link #variableScope} map.
	 *
	 * @param node
	 *            the node to visit
	 */
	@Override
	public boolean visit(final VariableDeclarationStatement node) {
		for (final Object fragment : node.fragments()) {
			final VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
			addBinding(node, frag.getName().getIdentifier(), node.getType());
		}
		return true;
	}

	/**
	 * Return a naive variableName to variableType map.
	 *
	 * @return
	 */
	public Map<String, String> getVariableTypes() {
		final Map<String, String> variableNameTypes = Maps.newTreeMap();
		for (final Entry<Integer, List<ASTNode>> variableBindings : variableBindings.entrySet()) {
			final String varType = checkNotNull(variableTypes.get(variableBindings.getKey()));
			for (final ASTNode node : variableBindings.getValue()) {
				variableNameTypes.put(node.toString(), varType);
			}
		}
		return variableNameTypes;
	}

	public Map<Integer, String> getVariableTypesAtPosition() {
		final Map<Integer, String> variableTypes = Maps.newTreeMap();

		for (final Entry<Integer, List<ASTNode>> variableBinding : variableBindings.entrySet()) {
			final String varType = checkNotNull(variableTypes.get(variableBinding.getKey()));
			for (final ASTNode node : variableBinding.getValue()) {
				variableTypes.put(node.getStartPosition(), varType);
			}
		}

		return variableTypes;
	}

	public JavaApproximateTypeInferencer(final CompilationUnit node) {
		rootNode = node;
	}

	public void process() {
		rootNode.accept(this);
	}

}
