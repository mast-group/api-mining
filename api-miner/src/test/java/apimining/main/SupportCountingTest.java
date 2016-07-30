package apimining.main;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashSet;

import org.junit.Test;

import apimining.main.EMStep;
import apimining.main.SequenceMining;
import apimining.sequence.Sequence;
import apimining.transaction.TransactionList;

public class SupportCountingTest {

	@Test
	public void testSupportCounting() throws IOException {

		final File input = getTestFile("TOY.txt"); // database
		final TransactionList transactions = SequenceMining.readTransactions(input);
		final Sequence seq = new Sequence(7, 3);
		final HashSet<Sequence> seqs = new HashSet<>();
		seqs.add(seq);
		final long supp = EMStep.getSupportsOfSequences(transactions, seqs).get(seq);
		assertEquals(1, supp);
	}

	public File getTestFile(final String filename) throws UnsupportedEncodingException {
		final URL url = this.getClass().getClassLoader().getResource(filename);
		return new File(java.net.URLDecoder.decode(url.getPath(), "UTF-8"));
	}

}
