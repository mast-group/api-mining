package itemsetmining.main;

import java.util.HashSet;

public class Rule {
	private HashSet<Integer> itemset1; // antecedent
	private HashSet<Integer> itemset2; // consequent
	private double probability;

	/**
	 * Constructor
	 * 
	 * @param itemset1
	 *            the antecedent of the rule (an itemset)
	 * @param itemset2
	 *            the consequent of the rule (an itemset)
	 * @param probablity
	 *            probability of the rule (integer)
	 */
	public Rule(HashSet<Integer> itemset1, HashSet<Integer> itemset2,
			double probability) {
		this.itemset1 = itemset1;
		this.itemset2 = itemset2;
		this.probability = probability;
	}

	/**
	 * Return a String representation of this rule
	 * 
	 * @return a String
	 */
	public String toString() {
		return itemset1.toString() + " ==> " + itemset2.toString() + " prob: "
				+ probability;
	}

	/**
	 * Get the left itemset of this rule (antecedent).
	 * 
	 * @return an itemset.
	 */
	public HashSet<Integer> getItemset1() {
		return itemset1;
	}

	/**
	 * Get the right itemset of this rule (consequent).
	 * 
	 * @return an itemset.
	 */
	public HashSet<Integer> getItemset2() {
		return itemset2;
	}

	/**
	 * Get the probability of this rule.
	 * 
	 * @return probability.
	 */
	public double getProbability() {
		return probability;
	}

}
