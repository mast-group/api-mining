/**
 *
 */
package codemining.java.codeutils;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ClassHierarchy;
import codemining.languagetools.ClassHierarchy.Type;

import com.google.common.base.Optional;

/**
 * Collect information about classes and their implementing methods.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class ProjectTypeInformation {

	private final File projectDirectory;
	private final MethodsInClass methodInformation = new MethodsInClass();
	private ClassHierarchy hierarchy = null;

	public ProjectTypeInformation(final File projectDirectory) {
		this.projectDirectory = projectDirectory;
	}

	public void collect() {
		final Collection<File> allFiles = FileUtils
				.listFiles(projectDirectory, JavaTokenizer.javaCodeFileFilter,
						DirectoryFileFilter.DIRECTORY);
		methodInformation.scan(allFiles);
		final JavaTypeHierarchyExtractor hierarchyExtractor = new JavaTypeHierarchyExtractor();
		hierarchyExtractor.addFilesToCorpus(allFiles);
		hierarchy = hierarchyExtractor.getHierarchy();
	}

	public boolean isMethodOverride(final String fullyQualifiedNameOfClass,
			final MethodDeclaration method) {
		final String methodSignature = method.getName().getIdentifier() + ":"
				+ MethodUtils.getMethodType(method);
		if (!methodInformation.getMethodsForClass(fullyQualifiedNameOfClass)
				.contains(methodSignature)) {
			return false;
		}
		final Optional<Type> type = hierarchy
				.getTypeForName(fullyQualifiedNameOfClass);
		if (!type.isPresent()) {
			return false;
		}
		for (final Type implementor : type.get().getImplementingTypesClosure()) {
			if (methodInformation.getMethodsForClass(
					implementor.fullQualifiedName).contains(methodSignature)) {
				return true;
			}
		}
		return false;
	}

}
