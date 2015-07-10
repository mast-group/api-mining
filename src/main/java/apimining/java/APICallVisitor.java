package apimining.java;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EnumDeclaration;
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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;

import com.google.common.base.Charsets;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Table;
import com.google.common.io.Files;

import apimining.java.ASTVisitors.MethodClassDeclarationVisitor;
import apimining.java.ASTVisitors.WildcardImportVisitor;

/**
 * Visit all API calls (method invocations, and class instance creations) in
 * given AST and approximately resolve their fully qualified names.
 *
 * Optionally supply namespace files to resolve wildcard imports.
 *
 * <p>
 * <b>Caveats:</b> Resolving nested method calls has severe limitations.
 * Currently local methods are not in-lined (ala MAPO) Currently there is no way
 * of knowing if wildcard imports have been fully resolved (i.e. if the supplied
 * namespace is complete), therefore package local classes/methods are not
 * resolved when wildcard imports are present. Superclass methods (and their
 * return types) are not resolved. Super method/constructor invocations are not
 * considered, neither are class cast expressions.
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 */
public class APICallVisitor extends JavaApproximateTypeInferencer {

	/** fqNames for declared classes */
	private final Set<String> decClasses;

	/** Map between declared method fqNames and their return types */
	private final Map<String, Type> methodReturnTypes;

	/** Folder with namespaces for wildcarded imports */
	private final String wildcardNameSpaceFolder;

	/** Wildcarded imports */
	private final Set<String> wildcardImports;

	/** Wildcarded static imports */
	private final Set<String> wildcardMethodImports;

	/** Map between statically imported method names and their fqName */
	private final Map<String, String> methodImports = new HashMap<>();

	/** Multimap between declared methods and API Calls */
	private final LinkedListMultimap<MethodDeclaration, Expression> apiCalls = LinkedListMultimap.create();

	/** Declared method stack */
	private final Stack<MethodDeclaration> methodStack = new Stack<MethodDeclaration>();

	/** Map between declared methods and their fqName */
	private final Map<MethodDeclaration, String> methodNames = new HashMap<>();

	/** Class scope name */
	private final StringBuilder scopeName = new StringBuilder();

	/** Unresolved class names */
	private final Set<String> unresClasses = new HashSet<>();

	/** Unresolved method names */
	private final Set<String> unresMethods = new HashSet<>();

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
		methodStack.push(node);
		final String name = node.getName().toString();
		methodNames.put(node, currentPackage + scopeName + "." + name);
		return super.visit(node);
	}

	@Override
	public void endVisit(final MethodDeclaration node) {
		final MethodDeclaration curNode = methodStack.pop();
		assert curNode == node; // node was top of stack
	}

	@Override
	public boolean visit(final MethodInvocation node) {
		if (methodStack.size() > 0) // ignore class-level calls
			apiCalls.put(methodStack.peek(), node);
		return super.visit(node);
	}

	@Override
	public boolean visit(final ClassInstanceCreation node) {
		if (methodStack.size() > 0) // ignore class-level calls
			apiCalls.put(methodStack.peek(), node);
		return super.visit(node);
	}

	@Override
	public boolean visit(final TypeDeclaration node) {
		scopeName.append("." + node.getName().toString());
		return super.visit(node);
	}

	@Override
	public void endVisit(final TypeDeclaration node) {
		scopeName.delete(scopeName.lastIndexOf("."), scopeName.length());
	}

	@Override
	public boolean visit(final EnumDeclaration node) {
		scopeName.append("." + node.getName().toString());
		return super.visit(node);
	}

	@Override
	public void endVisit(final EnumDeclaration node) {
		scopeName.delete(scopeName.lastIndexOf("."), scopeName.length());
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

		final MethodClassDeclarationVisitor mcdv = new MethodClassDeclarationVisitor();
		mcdv.process(unit);
		decClasses = mcdv.decClasses;
		methodReturnTypes = mcdv.methodReturnTypes;

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

	@Override
	protected final String getFullyQualifiedNameFor(final String className) {
		if (importedNames.containsKey(className))
			return importedNames.get(className);
		for (final String wildcardImport : wildcardImports) { // java.util.* etc
			try {
				return Class.forName(wildcardImport + "." + className.replace(".", "$")).getName();
			} catch (final ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
			}
		}
		try {
			return Class.forName("java.lang." + className.replace(".", "$")).getName();
		} catch (final ClassNotFoundException e) {
		}
		for (final String fqClassName : decClasses) { // Local classes
			if (fqClassName.endsWith(className))
				return fqClassName;
		}
		// No wildcard imports, thus it's in the current package
		if (wildcardImports.isEmpty())
			return currentPackage + "." + className;
		unresClasses.add(className);
		return "UNRESOLVED." + className;
	}

	private String getFQMethodClassFor(final Expression expression, final String parentClassName,
			final Table<ASTNode, String, String> variableScopeNameTypes) {
		if (expression.getNodeType() == ASTNode.QUALIFIED_NAME)
			return ((QualifiedName) expression).getFullyQualifiedName();
		if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			final String name = ((SimpleName) expression).toString();
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, expression);
			if (variableScopeNameTypes.contains(scope, name)) // variable
																// name
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
				return getFQMethodClassFor(reqClass, parentClassName, variableScopeNameTypes);
			} else { // Handle nested method calls
				if (inv.getExpression() == null) { // method1().method2()
					final String name = ((MethodInvocation) expression).getName().toString();
					if (methodReturnTypes.containsKey(parentClassName + "." + name))
						return getNameOfType(methodReturnTypes.get(parentClassName + "." + name));
					return "UNRESOLVED";
				}
				if (inv.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
					final String name = ((MethodInvocation) expression).getName().toString(); // this.method1().method2()
					if (methodReturnTypes.containsKey(parentClassName + "." + name))
						return getNameOfType(methodReturnTypes.get(parentClassName + "." + name));
					return "UNRESOLVED";
				}
				if (inv.getExpression().getNodeType() == ASTNode.SIMPLE_NAME) // variable.method1().method2()
					return "UNRESOLVED"; // TODO could do better here?
				if (inv.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) // method1().method2().method3()
					return "UNRESOLVED"; // TODO could do better here?
				return "UNRESOLVED"; // TODO could do better in some of these?
			}
		}
		if (expression.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) // super().method1().method2()
			return "UNRESOLVED";
		if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			final ParenthesizedExpression par = (ParenthesizedExpression) expression;
			return getFQMethodClassFor(par.getExpression(), parentClassName, variableScopeNameTypes);
		}
		if (expression.getNodeType() == ASTNode.ARRAY_ACCESS) {
			final ArrayAccess array = (ArrayAccess) expression;
			return getFQMethodClassFor(array.getArray(), parentClassName, variableScopeNameTypes).replace("[]", "");
		}
		if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			final PrefixExpression prefix = (PrefixExpression) expression;
			return getFQMethodClassFor(prefix.getOperand(), parentClassName, variableScopeNameTypes);
		}
		if (expression.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
			final PostfixExpression postfix = (PostfixExpression) expression;
			return getFQMethodClassFor(postfix.getOperand(), parentClassName, variableScopeNameTypes);
		}
		if (expression.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			final InfixExpression infix = (InfixExpression) expression;
			if (infix.getOperator() == InfixExpression.Operator.PLUS
					&& (infix.getLeftOperand().toString().contains("toString()")
							|| infix.getRightOperand().toString().contains("toString()")))
				return "java.lang.String"; // toString() String concat.
			final String fqLeftName = getFQMethodClassFor(infix.getLeftOperand(), parentClassName,
					variableScopeNameTypes);
			final String fqRightName = getFQMethodClassFor(infix.getRightOperand(), parentClassName,
					variableScopeNameTypes);
			if (fqLeftName.equals(fqRightName))
				return fqLeftName; // general case
			if (infix.getOperator() == InfixExpression.Operator.PLUS
					&& (fqLeftName.equals("java.lang.String") || fqRightName.equals("java.lang.String")
							|| fqLeftName.equals("java.lang.Character") || fqRightName.equals("java.lang.Character")))
				return "java.lang.String"; // implicit String concat.
			return "UNRESOLVED";
		}
		if (expression.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
			final ConditionalExpression cond = (ConditionalExpression) expression;
			final String fqThenName = getFQMethodClassFor(cond.getThenExpression(), parentClassName,
					variableScopeNameTypes);
			final String fqElseName = getFQMethodClassFor(cond.getElseExpression(), parentClassName,
					variableScopeNameTypes);
			if (fqThenName.equals("java.lang.String") || fqElseName.equals("java.lang.String"))
				return "java.lang.String";
			if (fqThenName.equals(fqElseName))
				return fqThenName; // boolExp ? thenExp : elseExp
			return "UNRESOLVED";
		}
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			final Assignment ass = (Assignment) expression;
			return getFQMethodClassFor(ass.getLeftHandSide(), parentClassName, variableScopeNameTypes);
		}
		throw new RuntimeException("Unhandled Expression ASTNode. Please implement.");
	}

	public String getFullyQualifiedMethodNameFor(final Expression exp, final String parentClassName,
			final Table<ASTNode, String, String> variableScopeNameTypes) {
		String fqName;
		String name;
		if (exp.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			name = "<init>";
			fqName = getNameOfType(((ClassInstanceCreation) exp).getType());
		} else { // MethodInvocation
			final MethodInvocation node = (MethodInvocation) exp;
			name = node.getName().toString();
			if (node.getExpression() == null) {
				if (methodImports.containsKey(name))
					fqName = methodImports.get(name);
				else if (methodReturnTypes.containsKey(parentClassName + "." + name))
					fqName = parentClassName; // local
				else if (wildcardMethodImports.isEmpty())
					fqName = currentPackage; // package local method
				else
					fqName = "UNRESOLVED";
			} else if (node.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				if (methodReturnTypes.containsKey(parentClassName + "." + name))
					fqName = parentClassName; // local
				else // superclass method
					fqName = "SUPER";
			} else {
				fqName = getFQMethodClassFor(node.getExpression(), parentClassName, variableScopeNameTypes);
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

	/**
	 * Get multimap of fq method names to fully qualified names of API calls
	 *
	 * @param namespace
	 *            API namespace (e.g. org.apache.hadoop), "" for all namespaces
	 */
	public LinkedListMultimap<String, String> getAPINames(final String namespace) {
		final LinkedListMultimap<String, String> fqAPICalls = LinkedListMultimap.create();

		// Get scopes for variables
		final Table<ASTNode, String, String> variableScopeNameTypes = HashBasedTable.create();
		for (final ASTNode parent : variableNames.keySet()) {
			final ASTNode scope = ASTVisitors.getCoveringBlock(rootNode, parent);
			for (final Entry<String, Integer> entry : variableNames.get(parent).entrySet())
				variableScopeNameTypes.put(scope, entry.getKey(), box(variableTypes.get(entry.getValue())));
		}

		for (final MethodDeclaration method : apiCalls.keySet()) {
			for (final Expression exp : apiCalls.get(method)) {
				final String fqCallingMethod = methodNames.get(method);
				final String parentClassName = fqCallingMethod.substring(0, fqCallingMethod.lastIndexOf("."));
				final String fqMethodCall = getFullyQualifiedMethodNameFor(exp, parentClassName,
						variableScopeNameTypes);
				if (fqMethodCall.startsWith(namespace))
					fqAPICalls.put(fqCallingMethod, fqMethodCall);
			}
		}

		for (final String className : unresClasses)
			System.out.println("+++++ INFO: Unable to resolve class " + className);

		for (final String name : unresMethods)
			System.out.println("+++++ INFO: Unable to resolve method " + name);

		return fqAPICalls;
	}

}