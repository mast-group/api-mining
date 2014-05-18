package itemsetmining.main;

import java.util.Collection;
import java.util.HashSet;

/**
 * This class represents an itemset-tree node.
 * 
 * Copyright (c) 2008-2012 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 * 
 * SPMF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SPMF. If not, see <http://www.gnu.org/licenses/>.
 */
public class ItemsetTreeNode {

	// the itemset
	int[] itemset;
	// the support
	int support;
	// the list of children
	Collection<ItemsetTreeNode> children = new HashSet<ItemsetTreeNode>();

	/**
	 * The constructor
	 * 
	 * @param itemset
	 *            the itemset to be stored in this node.
	 * @param support
	 *            the support associated to this node.
	 */
	public ItemsetTreeNode(int[] itemset, int support) {
		this.itemset = itemset;
		this.support = support;
	}

	/**
	 * Return a string representation of this node
	 * 
	 * @param buffer
	 *            a stringbuffer for appending a string representation
	 * @param space
	 *            the indentation that should be used on each line
	 * @return the updated buffer as a string.
	 */
	public String toString(StringBuffer buffer, String space) {
		buffer.append(space);
		if (itemset == null) {
			buffer.append("{}");
		} else {
			buffer.append("[");
			for (Integer item : itemset) {
				buffer.append(item);
				buffer.append(" ");
			}
			buffer.append("]");
		}
		buffer.append("   sup=");
		buffer.append(support);
		buffer.append("\n");

		for (ItemsetTreeNode node : children) {
			node.toString(buffer, space + "  ");
		}
		return buffer.toString();
	}

	public String toString() {
		return toString(new StringBuffer(), "  ");
	}

}
