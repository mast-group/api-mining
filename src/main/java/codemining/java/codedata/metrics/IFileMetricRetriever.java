/**
 * 
 */
package codemining.java.codedata.metrics;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * An interface for all the classes that can return a metric
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public interface IFileMetricRetriever {
	double getMetricForASTNode(final ASTNode node);

	double getMetricForFile(final File file) throws IOException;
}
