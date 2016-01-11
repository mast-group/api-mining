package sequencemining.main;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.junit.Test;

import sequencemining.sequence.Sequence;
import sequencemining.transaction.TransactionList;

public class SupportCountingTest {

	@Test
	public void testSupportCounting() throws IOException {

		final File input = getTestFile("TOY.txt"); // database
		final TransactionList transactions = SequenceMining.readTransactions(input);
		assertEquals(1, EMStep.getSupportOfSequence(transactions, new Sequence(7, 3)));
	}

	public File getTestFile(final String filename) throws UnsupportedEncodingException {
		final URL url = this.getClass().getClassLoader().getResource(filename);
		return new File(java.net.URLDecoder.decode(url.getPath(), "UTF-8"));
	}

}
