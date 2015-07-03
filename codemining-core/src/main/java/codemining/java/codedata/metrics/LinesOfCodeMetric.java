/**
 * 
 */
package codemining.java.codedata.metrics;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Find how many lines of code there are in the given file.
 * 
 * Note that if you give a file it returns the length including the contents,
 * while giving an AST Node ignores them.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class LinesOfCodeMetric implements IFileMetricRetriever {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.ed.inf.codedataextractors.IFileMetricRetriever#getMetricForASTNode
	 * (org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	public double getMetricForASTNode(final ASTNode node) {
		return node.toString().split(System.getProperty("line.separator")).length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.ed.inf.codedataextractors.IFileMetricRetriever#getMetricForFile
	 * (java.io.File)
	 */
	@Override
	public double getMetricForFile(File file) throws IOException {
		final String fileContents = FileUtils.readFileToString(file);
		// This returns the real lines, while the other returns without the
		// comments.
		return fileContents.split(System.getProperty("line.separator")).length;
	}

}
