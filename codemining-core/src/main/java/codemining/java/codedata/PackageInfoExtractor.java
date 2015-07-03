/**
 * 
 */
package codemining.java.codedata;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import com.google.common.collect.Lists;

/**
 * Get package information from Java source code.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class PackageInfoExtractor extends ASTVisitor {

	private String packageName;
	private final List<String> packageImports;

	private final CompilationUnit cu;

	public PackageInfoExtractor(final CompilationUnit cu) {
		this.cu = cu;
		packageImports = Lists.newArrayList();
		cu.accept(this);
	}

	public List<String> getImports() {
		return Collections.unmodifiableList(packageImports);
	}

	public String getPackageName() {
		return packageName;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		packageImports.add(node.getName().getFullyQualifiedName());
		return false;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		packageName = node.getName().toString();
		return false;
	}

}
