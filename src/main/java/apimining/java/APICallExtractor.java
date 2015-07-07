package apimining.java;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

public class APICallExtractor {

	private static final String libFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/java_libraries/";
	private static final String namespaceFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/API/namespaces/";

	public static void main(final String[] args) {

		final File[] projects = new File(libFolder).listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
		Arrays.sort(projects);

		// For each java file in project
		for (final File project : projects) {

			System.out.println("===== Processing " + project.getName());

			// Get all java files in source folder
			final List<File> files = (List<File>) FileUtils.listFiles(project, new String[] { "java" }, true);
			Collections.sort(files);

			int count = 0;
			for (final File file : files) {
				// if (!file.getName().contains("ScanIteratorTest"))
				// continue;

				System.out.println("\nFile: " + file);

				// Ignore empty files
				if (file.length() == 0)
					continue;

				if (count % 50 == 0)
					System.out.println("At file " + count + " of " + files.size());
				count++;

				final APICallVisitor acv = new APICallVisitor(ASTVisitors.getAST(file), namespaceFolder);
				acv.process();

				for (final String fqName : new HashSet<>(acv.getAPINames()))
					if (!fqName.contains("java.") && !fqName.contains("LOCAL."))
						System.out.println(fqName);

			}

			System.out.println("Press any key to continue...");
			try {
				System.in.read();
			} catch (final Exception e) {
			}

		}

	}

}
