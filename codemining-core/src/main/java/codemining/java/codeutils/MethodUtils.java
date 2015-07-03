/**
 *
 */
package codemining.java.codeutils;

import java.util.List;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

/**
 * A set of utility methods for Java Methods.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public final class MethodUtils {

	/**
	 * @param node
	 * @return
	 */
	public static String getMethodType(final MethodDeclaration node) {
		final StringBuffer typeSb = new StringBuffer();
		if (node.getReturnType2() != null) {
			typeSb.append(node.getReturnType2().toString()).append("(");
		} else if (node.isConstructor()) {
			typeSb.append("constructor(");
		} else {
			typeSb.append("void(");
		}
		for (final Object svd : node.parameters()) {
			final SingleVariableDeclaration decl = (SingleVariableDeclaration) svd;
			typeSb.append(decl.getType().toString());
			typeSb.append(",");
		}
		typeSb.append(")");

		final String methodType = typeSb.toString();
		return methodType;
	}

	public static boolean hasOverrideAnnotation(final MethodDeclaration node) {
		final List modifiers = node.modifiers();
		for (final Object mod : modifiers) {
			final IExtendedModifier modifier = (IExtendedModifier) mod;
			if (modifier.isAnnotation()) {
				final Annotation annotation = (Annotation) modifier;
				if (annotation.getTypeName().toString().equals("Override")) {
					return true;
				}
			}
		}
		return false;
	}

	private MethodUtils() {
	}

}
