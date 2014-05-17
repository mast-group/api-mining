package itemsetmining.main;

import java.util.List;

import com.google.common.collect.Lists;

/** a transaction is an ordered list of items */
public class Transaction {

	private final List<Integer> items;

	public Transaction() {
		items = Lists.newArrayList();
	}

	public Transaction(final List<Integer> transaction) {
		items = Lists.newArrayList(transaction);
	}

	public void addItem(final Integer item) {
		items.add(item);
	}

	public List<Integer> getItems() {
		return items;
	}

}