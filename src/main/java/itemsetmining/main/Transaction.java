package itemsetmining.main;

import java.util.List;

import com.google.common.collect.Lists;

/** a transaction is an ordered list of items */
public class Transaction {

	private final List<Integer> items = Lists.newArrayList();

	public Transaction(final int... items) {
		add(items);
	}

	public void add(final int item) {
		items.add(item);
	}

	public void add(final int... items) {
		for (final int item : items)
			this.items.add(item);
	}

	public List<Integer> getItems() {
		return items;
	}

	@Override
	public String toString() {
		return items.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Transaction other = (Transaction) obj;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		return true;
	}

}