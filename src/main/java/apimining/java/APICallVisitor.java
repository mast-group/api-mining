package apimining.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.google.common.base.Charsets;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.Files;

import apimining.java.ASTVisitors.WildcardImportVisitor;

/**
 * Visit all API calls (method invocations and class instance creations) in
 * given AST and approximately resolve their fully qualified names.
 *
 * Optionally supply namespace files to resolve wildcard imports.
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 */
public class APICallVisitor extends JavaApproximateTypeInferencer {

	/** Locally declared classes */
	private final Set<String> decClasses;

	/** Folder with namespaces for wildcarded imports */
	private final String wildcardNameSpaceFolder;

	/** Wildcarded imports */
	private final Set<String> wildcardImports;

	/** Wildcarded static imports */
	private final Set<String> wildcardMethodImports;

	/** A map between statically imported methodNames and their fqName */
	private final Map<String, String> methodImports = new HashMap<>();

	/** A map between declared methods and their return types */
	private final Map<String, Type> methodReturnTypes = new HashMap<>();

	/** API Calls */
	private final List<Expression> apiCalls = new ArrayList<>();

	/** Unresolved class names */
	private final Set<String> unresClasses = new HashSet<>();

	/** Unresolved method names */
	private final Set<String> unresMethods = new HashSet<>();

	@Override
	protected final String getFullyQualifiedNameFor(final String name) {
		final String className = name.replace(".", "$"); // Nested classes
		if (importedNames.containsKey(className))
			return importedNames.get(className);
		for (final String wildcardImport : wildcardImports) { // java.util.* etc
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
		if (!node.isStatic()) {
			importedNames.put(qName.substring(qName.lastIndexOf('.') + 1), qName);
		} else {
			final String name = qName.substring(qName.lastIndexOf('.') + 1);
			if (Character.isLowerCase(name.charAt(0))) // ignore constants
				methodImports.put(name, qName.substring(0, qName.lastIndexOf('.')));
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
		apiCalls.add(node);
		return super.visit(node);
	}

	@Override
	public boolean visit(final SuperMethodInvocation node) {
		apiCalls.add(node);
		return super.visit(node);
	}

	@Override
	public boolean visit(final ClassInstanceCreation node) {
		apiCalls.add(node);
		return super.visit(node);
	}

	public APICallVisitor(final CompilationUnit unit) {
		this(unit, null);
	}

	/**
	 * @param wildcardNameSpaceFolder
	 *            folder with namespaces for wildcarded imports in class/method
	 *            subfolders
	 */
	public APICallVisitor(final CompilationUnit unit, final String wildcardNameSpaceFolder) {
		super(unit); // set root node

		this.wildcardNameSpaceFolder = wildcardNameSpaceFolder;

		decClasses = ASTVisitors.getDeclaredClasses(unit);

		final WildcardImportVisitor wiv = new WildcardImportVisitor(".*");
		wiv.process(unit);
		this.wildcardImports = wiv.wildcardImports;
		this.wildcardMethodImports = wiv.wildcardMethodImports;
	}

	@Override
	public void process() {

		for (final String wimport : wildcardImports) {
			try {
				final List<String> qNames = Files.readLines(new File(wildcardNameSpaceFolder + "class/" + wimport),
						Charsets.UTF_8);
				for (final String qName : qNames)
					importedNames.put(qName.substring(qName.lastIndexOf('.') + 1), qName);
				System.out.println("+++++ INFO: Successfully resolved import " + wimport);
			} catch (final IOException e) {
				if (!wimport.matches("java.*")) // handled in getFQNameFor()
					System.out.println("***** WARNING: Wildcard import '" + wimport
							+ "' not resolved. Please supply class namespace file.");
			}
		}

		for (final String wimport : wildcardMethodImports) {
			try {
				final List<String> qNames = Files.readLines(new File(wildcardNameSpaceFolder + "method/" + wimport),
						Charsets.UTF_8);
				for (final String qName : qNames) {
					final String name = qName.substring(qName.lastIndexOf('.') + 1);
					if (Character.isLowerCase(name.charAt(0)))
						methodImports.put(name, qName.substring(0, qName.lastIndexOf('.')));
					System.out.println("+++++ INFO: Successfully resolved static import " + wimport);
				}
			} catch (final IOException e) {
				System.out.println("***** WARNING: Static wildcard import '" + wimport
						+ "' not resolved. Please supply method namespace file.");
			}
		}

		rootNode.accept(this);
	}

	private String getFQMethodClassFor(final Expression expression,
			final Table<ASTNode, String, String> variableScopeNameTypes, final ASTNode parent) {
		if (expression == null && parent.getNodeType() == ASTNode.METHOD_INVOCATION) { // method1().method2()
			final String name = ((MethodInvocation) parent).getName().toString();
			if (methodReturnTypes.containsKey(name))
				return getNameOfType(methodReturnTypes.get(name));
			return "UNRESOLVED";
		}
		if (expression.getNodeType() == ASTNode.THIS_EXPRESSION && parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
			final String name = ((MethodInvocation) parent).getName().toString(); // this.method1().method2()
			if (methodReturnTypes.containsKey(name))
				return getNameOfType(methodReturnTypes.get(name));
			return "UNRESOLVED";
		}
		if (expression.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) // super().method1().method2()
			return "UNRESOLVED";
		if (expression.getNodeType() == ASTNode.QUALIFIED_NAME)
			return ((QualifiedName) expression).getFullyQualifiedName();
		if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			final String name = ((SimpleName) expression).toString();
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, expression);
			if (variableScopeNameTypes.contains(scope, name)) // variable name
				return variableScopeNameTypes.get(scope, name);
			return getFullyQualifiedNameFor(name); // class name
		}
		if (expression.getNodeType() == ASTNode.FIELD_ACCESS) {
			final String name = ((FieldAccess) expression).getName().toString();
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, expression);
			if (variableScopeNameTypes.contains(scope, name))
				return variableScopeNameTypes.get(scope, name);
			return "UNRESOLVED";
		}
		if (expression.getNodeType() == ASTNode.SUPER_FIELD_ACCESS)
			return "UNRESOLVED";
		if (expression.getNodeType() == ASTNode.STRING_LITERAL)
			return "java.lang.String";
		if (expression.getNodeType() == ASTNode.CHARACTER_LITERAL)
			return "java.lang.Character";
		if (expression.getNodeType() == ASTNode.NUMBER_LITERAL)
			return "java.lang.Number"; // don't handle subclasses
		if (expression.getNodeType() == ASTNode.TYPE_LITERAL)
			return getNameOfType(((TypeLiteral) expression).getType());
		if (expression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
			return getNameOfType(((ClassInstanceCreation) expression).getType());
		if (expression.getNodeType() == ASTNode.CAST_EXPRESSION)
			return getNameOfType(((CastExpression) expression).getType());
		if (expression.getNodeType() == ASTNode.ARRAY_CREATION)
			return getNameOfType(((ArrayCreation) expression).getType());
		if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			final MethodInvocation inv = (MethodInvocation) expression;
			if (inv.getName().toString().equals("forName")) { // Class.forName()
				final Expression reqClass = (Expression) inv.arguments().get(0);
				return getFQMethodClassFor(reqClass, variableScopeNameTypes, expression);
			} else // Handle nested method calls
				return getFQMethodClassFor(inv.getExpression(), variableScopeNameTypes, expression);
		}
		if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			final ParenthesizedExpression par = (ParenthesizedExpression) expression;
			return getFQMethodClassFor(par.getExpression(), variableScopeNameTypes, null);
		}
		if (expression.getNodeType() == ASTNode.ARRAY_ACCESS) {
			final ArrayAccess array = (ArrayAccess) expression;
			return getFQMethodClassFor(array.getArray(), variableScopeNameTypes, null).replace("[]", "");
		}
		if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			final PrefixExpression prefix = (PrefixExpression) expression;
			return getFQMethodClassFor(prefix.getOperand(), variableScopeNameTypes, null);
		}
		if (expression.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
			final PostfixExpression postfix = (PostfixExpression) expression;
			return getFQMethodClassFor(postfix.getOperand(), variableScopeNameTypes, null);
		}
		if (expression.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			final InfixExpression infix = (InfixExpression) expression;
			if (infix.getOperator() == InfixExpression.Operator.PLUS
					&& (infix.getLeftOperand().toString().contains("toString()")
							|| infix.getRightOperand().toString().contains("toString()")))
				return "java.lang.String"; // toString() String concat.
			final String fqLeftName = getFQMethodClassFor(infix.getLeftOperand(), variableScopeNameTypes, null);
			final String fqRightName = getFQMethodClassFor(infix.getRightOperand(), variableScopeNameTypes, null);
			if (fqLeftName.equals(fqRightName))
				return fqLeftName; // general case
			if (infix.getOperator() == InfixExpression.Operator.PLUS
					&& (fqLeftName.equals("java.lang.String") || fqRightName.equals("java.lang.String")
							|| fqLeftName.equals("java.lang.Character") || fqRightName.equals("java.lang.Character")))
				return "java.lang.String"; // implicit String concat.
		}
		if (expression.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
			final ConditionalExpression cond = (ConditionalExpression) expression;
			final String fqThenName = getFQMethodClassFor(cond.getThenExpression(), variableScopeNameTypes, null);
			final String fqElseName = getFQMethodClassFor(cond.getElseExpression(), variableScopeNameTypes, null);
			if (fqThenName.equals("java.lang.String") || fqElseName.equals("java.lang.String"))
				return "java.lang.String";
			if (fqThenName.equals(fqElseName))
				return fqThenName; // boolExp ? thenExp : elseExp
		}
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			final Assignment ass = (Assignment) expression;
			return getFQMethodClassFor(ass.getLeftHandSide(), variableScopeNameTypes, null);
		}
		throw new RuntimeException("Unhandled Expression ASTNode. Please implement.");
	}

	public String getFullyQualifiedMethodNameFor(final Table<ASTNode, String, String> variableScopeNameTypes,
			final Expression exp) {
		String fqName;
		String name;
		if (exp.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			name = "<init>";
			fqName = getNameOfType(((ClassInstanceCreation) exp).getType());
		} else if (exp.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
			final SuperMethodInvocation node = (SuperMethodInvocation) exp;
			name = node.getName().toString();
			if (node.getQualifier() != null && node.getQualifier().isQualifiedName())
				fqName = ((QualifiedName) node.getQualifier()).getFullyQualifiedName();
			else
				fqName = "LOCAL";
		} else { // MethodInvocation
			final MethodInvocation node = (MethodInvocation) exp;
			name = node.getName().toString();
			if (node.getExpression() == null) {
				if (methodImports.containsKey(name))
					fqName = methodImports.get(name);
				else if (wildcardMethodImports.isEmpty())
					fqName = "LOCAL"; // local method
				else
					fqName = "UNRESOLVED";
			} else if (node.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				fqName = "LOCAL";
			} else {
				fqName = getFQMethodClassFor(node.getExpression(), variableScopeNameTypes, null);
			}
		}
		if (fqName.equals("UNRESOLVED"))
			unresMethods.add(name);
		return fqName + "." + name;
	}

	/** Box primitive types, use java.lang.Number for numbers */
	private String box(final String string) {
		if (string.matches("byte|short|int|long|float|double"))
			return "java.lang.Number";
		if (string.matches("boolean"))
			return "java.lang.Boolean";
		if (string.matches("char"))
			return "java.lang.Character";
		return string;
	}

	/** Get sequence of fully qualified names of API calls */
	public ArrayList<String> getAPINames() {
		final ArrayList<String> fqMethodNames = new ArrayList<>();

		// Get scopes for variables
		final Table<ASTNode, String, String> variableScopeNameTypes = HashBasedTable.create();
		for (final ASTNode parent : variableNames.keySet()) {
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, parent);
			for (final Entry<String, Integer> entry : variableNames.get(parent).entrySet())
				variableScopeNameTypes.put(scope, entry.getKey(), box(variableTypes.get(entry.getValue())));
		}

		for (final Expression exp : apiCalls)
			fqMethodNames.add(getFullyQualifiedMethodNameFor(variableScopeNameTypes, exp));

		for (final String className : unresClasses)
			System.out.println("+++++ INFO: Unable to resolve class " + className);

		for (final String name : unresMethods)
			System.out.println("+++++ INFO: Unable to resolve method " + name);

		return fqMethodNames;
	}

}