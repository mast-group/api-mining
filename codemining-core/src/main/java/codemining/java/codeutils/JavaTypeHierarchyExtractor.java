/**
 *
 */
package codemining.java.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ClassHierarchy;
import codemining.util.data.Pair;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Use heuristics to extract the type hierarchy from a corpus.
 *
 * Uses fully qualified names.
 *
 * TODO: Still we can infer hierarchies from assignments and downcasting.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaTypeHierarchyExtractor {

	private static class HierarchyExtractor extends ASTVisitor {

		private final Map<String, String> importedNames = Maps.newTreeMap();

		private String currentPackageName;

		private final Set<Pair<String, String>> parentChildRelationships = Sets
				.newHashSet();

		Stack<String> className = new Stack<String>();

		private void addTypes(String parent, String child) {
			if (parent.contains("<")) {
				// Manually compute erasure.
				parent = parent.substring(0, parent.indexOf("<"))
						+ parent.substring(parent.indexOf(">") + 1);
			}
			if (child.contains("<")) {
				// Manually compute erasure.
				child = child.substring(0, child.indexOf("<"))
						+ child.substring(child.indexOf(">") + 1);
			}

			if (!child.contains(".") && importedNames.containsKey(child)) {
				child = importedNames.get(child);
			} else if (!child.contains(".")) {
				child = currentPackageName + "." + child;
			}
			if (!parent.contains(".") && importedNames.containsKey(parent)) {
				parent = importedNames.get(parent);
			} else if (!parent.contains(".")) {
				parent = currentPackageName + "." + parent;
			}
			final Pair<String, String> typeRelationship = Pair.create(parent,
					child);
			parentChildRelationships.add(typeRelationship);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core
		 * .dom.TypeDeclaration)
		 */
		@Override
		public void endVisit(final TypeDeclaration node) {
			className.pop();
			super.endVisit(node);
		}

		private void getTypeBindingParents(ITypeBinding binding) {
			if (binding.isParameterizedType()) {
				binding = binding.getErasure();
			}
			final String bindingName = binding.isRecovered() ? binding
					.getName() : binding.getQualifiedName();
			final ITypeBinding superclassBinding = binding.getSuperclass();
			if (superclassBinding != null) {
				addTypes(
						superclassBinding.isRecovered() ? superclassBinding.getName()
								: superclassBinding.getQualifiedName(),
						bindingName);
				getTypeBindingParents(superclassBinding);
			}

			for (ITypeBinding iface : binding.getInterfaces()) {
				if (iface.isParameterizedType()) {
					iface = iface.getErasure();
				}
				addTypes(
						iface.isRecovered() ? iface.getName()
								: iface.getQualifiedName(), bindingName);
				getTypeBindingParents(iface);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .CompilationUnit)
		 */
		@Override
		public boolean visit(final CompilationUnit node) {
			if (node.getPackage() != null) {
				currentPackageName = node.getPackage().getName()
						.getFullyQualifiedName();
			}
			for (final Object decl : node.imports()) {
				final ImportDeclaration imp = (ImportDeclaration) decl;
				if (!imp.isStatic()) {
					final String fqn = imp.getName().getFullyQualifiedName();
					importedNames.put(fqn.substring(fqn.lastIndexOf('.') + 1),
							fqn);
				}
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .ImportDeclaration)
		 */
		@Override
		public boolean visit(final ImportDeclaration node) {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .SimpleType)
		 */
		@Override
		public boolean visit(final SimpleType node) {
			if (node.resolveBinding() == null) {
				return true;
			}
			getTypeBindingParents(node.resolveBinding());
			return super.visit(node);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .TypeDeclaration)
		 */
		@Override
		public boolean visit(final TypeDeclaration node) {
			for (final Object supType : node.superInterfaceTypes()) {
				Type superType = (Type) supType;
				if (superType.isParameterizedType()) {
					superType = ((ParameterizedType) superType).getType();
				}
				final String qName = superType.resolveBinding().getErasure()
						.getQualifiedName();
				if (className.isEmpty()) {
					addTypes(qName, currentPackageName + "." + node.getName());
				} else {
					addTypes(qName, className.peek() + "." + node.getName());
				}
			}

			Type superclassType = node.getSuperclassType();
			if (superclassType != null) {
				if (superclassType.isParameterizedType()) {
					superclassType = ((ParameterizedType) superclassType)
							.getType();
				}
				addTypes(superclassType.resolveBinding().getQualifiedName(),
						currentPackageName + "." + node.getName());
			}

			if (className.isEmpty()) {
				className.push(currentPackageName + "."
						+ node.getName().getIdentifier());
				importedNames.put(node.getName().getIdentifier(),
						currentPackageName + "."
								+ node.getName().getIdentifier());
			} else {
				className.push(className.peek() + "."
						+ node.getName().getIdentifier());
				importedNames
						.put(node.getName().getIdentifier(), className.peek()
								+ "." + node.getName().getIdentifier());
			}
			return true;
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length != 1) {
			System.err.println("Usage <codeFolder>");
			System.exit(-1);
		}
		final File directory = new File(args[0]);

		final Collection<File> allFiles = FileUtils
				.listFiles(directory, JavaTokenizer.javaCodeFileFilter,
						DirectoryFileFilter.DIRECTORY);

		final JavaTypeHierarchyExtractor jthe = new JavaTypeHierarchyExtractor();
		jthe.addFilesToCorpus(allFiles);

		System.out.println(jthe);
	}

	private static final Logger LOGGER = Logger
			.getLogger(JavaTypeHierarchyExtractor.class.getName());

	private final ClassHierarchy hierarchy = new ClassHierarchy();

	public void addFilesToCorpus(final Collection<File> files) {
		files.parallelStream()
				.map(f -> getParentTypeRelationshipsFrom(f))
				.flatMap(rel -> rel.stream())
				.sequential()
				.forEach(
						rel -> hierarchy.addParentToType(rel.second, rel.first));

	}

	public ClassHierarchy getHierarchy() {
		return hierarchy;
	}

	private Collection<Pair<String, String>> getParentTypeRelationshipsFrom(
			final File file) {
		final JavaASTExtractor ex = new JavaASTExtractor(true);
		try {
			final CompilationUnit ast = ex.getAST(file);
			final HierarchyExtractor hEx = new HierarchyExtractor();
			ast.accept(hEx);
			return hEx.parentChildRelationships;
		} catch (final IOException e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		}
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		return hierarchy.toString();
	}

}
