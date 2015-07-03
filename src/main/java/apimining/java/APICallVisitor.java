package apimining.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Visit all API calls (as defined in MAPO) in given AST and approximately
 * resolve their fully qualified names (c.f. JavaApproximateTypeInferencer).
 *
 * @see {@link JavaApproximateTypeInferencer}
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 */
public class APICallVisitor extends JavaApproximateTypeInferencer {

	/** Root node of current AST */
	CompilationUnit rootNode;

	/** Wildcarded imports */
	private final Set<String> wildcardImports = new HashSet<>();

	/** Wildcarded static imports */
	private final Set<String> wildcardMethodImports = new HashSet<>();

	/** A map between statically imported methodNames and their fqName */
	private final Map<String, String> methodImports = new HashMap<>();

	/** A map between declared methods and their return types */
	private final Map<String, Type> methodReturnTypes = new HashMap<>();

	/** Method invocations */
	private final List<MethodInvocation> invMethods = new ArrayList<>();

	/** Locally declared classes */
	private Set<String> decClasses;

	/** Unresolved class names */
	private final Set<String> unresClasses = new HashSet<>();

	@Override
	protected final String getFullyQualifiedNameFor(final String name) {
		final String className = name.replace(".", "$"); // Nested classes
		if (importedNames.containsKey(className))
			return importedNames.get(className);
		for (final String wildcardImport : wildcardImports) {
			try {
				return Class.forName(wildcardImport + "." + className).getName();
			} catch (final ClassNotFoundException e) {
			}
		}
		try {
			return Class.forName("java.lang." + className).getName();
		} catch (final ClassNotFoundException e) {
		}
		if (decClasses.contains(className))
			return "LOCAL." + className;
		// No wildcard imports, thus it's in the current package
		if (wildcardImports.isEmpty())
			return "LOCAL." + className;
		unresClasses.add(className);
		return "UNRESOLVED." + className;
	}

	@Override
	public boolean visit(final ImportDeclaration node) {
		final String qName = node.getName().getFullyQualifiedName();
		final String imprt = node.toString().trim();
		if (!node.isStatic()) {
			if (imprt.endsWith(".*;"))
				wildcardImports.add(qName);
			else
				importedNames.put(qName.substring(qName.lastIndexOf('.') + 1), qName);
		} else {
			if (imprt.endsWith(".*;")) {
				wildcardMethodImports.add(qName);
			} else {
				final String name = qName.substring(qName.lastIndexOf('.') + 1);
				if (Character.isLowerCase(name.charAt(0)))
					methodImports.put(name, qName.substring(0, qName.lastIndexOf('.')));
			}
		}
		return false;
	}

	@Override
	public boolean visit(final MethodDeclaration node) {
		final String name = node.getName().toString();
		final Type returnType = node.getReturnType2();
		methodReturnTypes.put(name, returnType);
		return super.visit(node);
	}

	@Override
	public boolean visit(final MethodInvocation node) {
		invMethods.add(node);
		return super.visit(node);
	}

	@Override
	public void process(final CompilationUnit unit) {
		if (rootNode != null)
			throw new RuntimeException("Visitor already used.");
		decClasses = ASTVisitors.getDeclaredClasses(unit);
		rootNode = unit;
		rootNode.accept(this);
	}

	private String getFQMethodClass(final Expression expression,
			final Table<ASTNode, String, String> variableScopeNameTypes, final ASTNode parent) {
		if (expression == null && parent != null) {
			if (parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
				final String name = ((MethodInvocation) parent).getName().toString();
				return getNameOfType(methodReturnTypes.get(name)); // method1().method2()
			}
		}
		if (expression.getNodeType() == ASTNode.QUALIFIED_NAME) {
			final QualifiedName qName = (QualifiedName) expression;
			return qName.getFullyQualifiedName();
		}
		if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			final String name = ((SimpleName) expression).toString();
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, expression);
			if (variableScopeNameTypes.contains(scope, name))
				return variableScopeNameTypes.get(scope, name);
			return getFullyQualifiedNameFor(name);
		}
		if (expression.getNodeType() == ASTNode.TYPE_LITERAL)
			return getNameOfType(((TypeLiteral) expression).getType());
		if (expression.getNodeType() == ASTNode.STRING_LITERAL)
			return "java.lang.String";
		if (expression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
			return getNameOfType(((ClassInstanceCreation) expression).getType());
		if (expression.getNodeType() == ASTNode.CAST_EXPRESSION)
			return getNameOfType(((CastExpression) expression).getType());
		if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			final MethodInvocation inv = (MethodInvocation) expression;
			if (inv.getName().toString().equals("forName")) // Handle
															// Class.forName()
				return ((StringLiteral) inv.arguments().get(0)).getLiteralValue();
			else // Handle method1().method2()
				return getFQMethodClass(inv.getExpression(), variableScopeNameTypes, expression);
		}
		if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			final ParenthesizedExpression par = (ParenthesizedExpression) expression;
			return getFQMethodClass(par.getExpression(), variableScopeNameTypes, null);
		}
		if (expression.getNodeType() == ASTNode.FIELD_ACCESS) {
			final String name = ((FieldAccess) expression).getName().toString();
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, expression);
			return variableScopeNameTypes.get(scope, name);
		}
		if (expression.getNodeType() == ASTNode.ARRAY_ACCESS) {
			final ArrayAccess array = (ArrayAccess) expression;
			return getFQMethodClass(array.getArray(), variableScopeNameTypes, null);
		}
		throw new RuntimeException("Unhandled Expression ASTNode. Please implement.");
	}

	public ArrayList<String> getAPINames() {
		final ArrayList<String> fqMethodNames = new ArrayList<>();

		if (!wildcardImports.isEmpty())
			System.out.println("***** WARNING: AST contains wildcard imports, these may not resolve correctly.");

		if (!wildcardMethodImports.isEmpty())
			System.out.println("***** WARNING: AST contains static wildcard imports, these may not resolve correctly.");

		// Get scopes for variables
		final Table<ASTNode, String, String> variableScopeNameTypes = HashBasedTable.create();
		for (final ASTNode parent : variableNames.keySet()) {
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, parent);
			for (final Entry<String, Integer> entry : variableNames.get(parent).entrySet())
				variableScopeNameTypes.put(scope, entry.getKey(), variableTypes.get(entry.getValue()));
		}

		for (final MethodInvocation node : invMethods) {
			final String name = node.getName().toString();
			String fqName;
			if (node.getExpression() == null) {
				if (methodImports.containsKey(name))
					fqName = methodImports.get(name);
				else if (wildcardMethodImports.isEmpty()) // local method
					fqName = "LOCAL";
				else
					fqName = "UNRESOLVED";
			} else {
				fqName = getFQMethodClass(node.getExpression(), variableScopeNameTypes, null);
			}
			fqMethodNames.add(fqName + "." + name);
		}

		for (final String className : unresClasses)
			System.out.println("+++++ INFO: Unable to resolve class " + className);

		return fqMethodNames;
	}

}