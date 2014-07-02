package itemsetmining.itemset;

import java.util.BitSet;
import java.util.Iterator;

public class BitSetIterator implements Iterator<Integer> {

	private final BitSet bitset;
	private int i;

	public BitSetIterator(final BitSet bitset) {
		this.bitset = bitset;
		i = bitset.nextSetBit(0);
	}

	@Override
	public boolean hasNext() {
		if (i >= 0)
			return true;
		return false;
	}

	@Override
	public Integer next() {
		final int item = i;
		i = bitset.nextSetBit(i + 1);
		return item;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
