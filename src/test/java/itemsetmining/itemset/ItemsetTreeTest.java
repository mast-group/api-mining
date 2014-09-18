package itemsetmining.itemset;

import static org.junit.Assert.assertEquals;
import itemsetmining.main.ItemsetMining;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.junit.Test;

import com.google.common.collect.Multiset;

public class ItemsetTreeTest {

	@Test
	public void testItemsetTree() throws IOException {

		final File input = getTestFile("contextItemsetTree.txt"); // database
		// Get frequency of single items
		final Multiset<Integer> singletons = ItemsetMining
				.scanDatabaseToDetermineFrequencyOfSingleItems(input);

		// Applying the algorithm to build the itemset tree
		final ItemsetTree itemsetTree = new ItemsetTree(singletons);
		// method to construct the tree from a set of transactions in a file
		itemsetTree.buildTree(input);
		// print the tree in the console
		System.out.println("THIS IS THE TREE:");
		itemsetTree.printTree();

		// After the tree is built, we can query the tree.

		// Example query: what is the support of an itemset (e.g. {1 2 3})
		System.out
				.println("EXAMPLE QUERIES: FIND THE SUPPORT OF SOME ITEMSETS:");
		final int supp123 = itemsetTree
				.getSupportOfItemset(new Itemset(1, 2, 3));
		System.out.println("the support of 1 2 3 is : " + supp123);
		assertEquals(1, supp123);
		final int supp2 = itemsetTree.getSupportOfItemset(new Itemset(2));
		System.out.println("the support of 2 is : " + supp2);
		assertEquals(5, supp2);
		final int supp24 = itemsetTree.getSupportOfItemset(new Itemset(2, 4));
		System.out.println("the support of 2 4 is : " + supp24);
		assertEquals(3, supp24);
		final int supp12 = itemsetTree.getSupportOfItemset(new Itemset(1, 2));
		System.out.println("the support of 1 2 is : " + supp12);
		assertEquals(2, supp12);

	}

	public File getTestFile(final String filename)
			throws UnsupportedEncodingException {
		final URL url = this.getClass().getClassLoader().getResource(filename);
		return new File(java.net.URLDecoder.decode(url.getPath(), "UTF-8"));
	}

}
