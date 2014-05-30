package itemsetmining.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.Rules;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import codemining.util.StatsUtil;
import codemining.util.parallel.FutureThreadPool;

import com.google.common.base.Charsets;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ItemsetMining {

	// private static final String DATASET = "contextPasquier99.txt";
	// private static final String DATASET = "chess.txt";
	private static final String DATASET = "caviar.txt";
	// private static final String DATASET = "freerider.txt";
	// private static final String DATASET = "crossSupp.txt";
	// private static final String DATASET = "unlifted.txt";
	// private static final String DATASET = "overlap.txt";
	private static final double FPGROWTH_SUPPORT = 0.25; // relative support
	private static final double FPGROWTH_MIN_CONF = 0;
	private static final double FPGROWTH_MIN_LIFT = 0;

	private static final int STOP_AFTER_MAX_WALKS = 3;
	private static final int MAX_RANDOM_WALKS = 100;
	private static final int MAX_STRUCTURE_ITERATIONS = 100;

	private static final int OPTIMIZE_PARAMS_EVERY = 10;
	private static final double OPTIMIZE_TOL = 1e-10;

	private static final Random rand = new Random(); // For primal-dual
	private static LinearProgramSolver solver; // For exact ILP

	public static void main(final String[] args) throws IOException {

		// Read in transaction database
		final URL url = ItemsetMining.class.getClassLoader().getResource(
				DATASET);
		final String input = java.net.URLDecoder.decode(url.getPath(), "UTF-8");
		final File inputFile = new File(input);
		System.out.println("======= Transaction Database =======\n"
				+ Files.toString(inputFile, Charsets.UTF_8));
		// TODO don't read all transactions into memory
		final List<Transaction> transactions = readTransactions(inputFile);

		// Determine most frequent singletons
		final Multiset<Integer> singletons = scanDatabaseToDetermineFrequencyOfSingleItems(inputFile);

		// Apply the algorithm to build the itemset tree
		final ItemsetTree tree = new ItemsetTree();
		tree.buildTree(inputFile, singletons);
		tree.printStatistics();
		System.out.println("THIS IS THE TREE:");
		tree.printTree();

		// Run inference to find interesting itemsets
		System.out.println("============= ITEMSET INFERENCE =============");
		final HashMap<Itemset, Double> itemsets = structuralEM(transactions,
				singletons.elementSet(), tree);
		System.out.println("\n======= Transaction Database =======\n"
				+ Files.toString(inputFile, Charsets.UTF_8));
		System.out
				.println("\n============= INTERESTING ITEMSETS =============");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			System.out.printf("%s\tprob: %1.5f %n", entry.getKey(),
					entry.getValue());
		}

		// Generate Association rules from the interesting itemsets
		final List<Rule> rules = generateAssociationRules(itemsets);
		System.out.println("\n============= ASSOCIATION RULES =============");
		for (final Rule rule : rules) {
			System.out.println(rule.toString());
		}
		System.out.println("\n");

		// Compare with the FPGROWTH algorithm
		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(input, null,
				FPGROWTH_SUPPORT);
		algo.printStats();
		patterns.printItemsets(algo.getDatabaseSize());

		// Generate association rules from FPGROWTH itemsets
		final AlgoAgrawalFaster94 algo2 = new AlgoAgrawalFaster94();
		final Rules rules2 = algo2.runAlgorithm(patterns, null,
				algo.getDatabaseSize(), FPGROWTH_MIN_CONF, FPGROWTH_MIN_LIFT);
		rules2.printRulesWithLift(algo.getDatabaseSize());

	}

	/** Learn itemsets model using structural EM */
	public static HashMap<Itemset, Double> structuralEM(
			final List<Transaction> transactions,
			final Set<Integer> singletons, final ItemsetTree tree) {

		// Intialize with equiprobable singleton sets
		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();
		for (final int singleton : singletons) {
			itemsets.put(new Itemset(singleton), 0.1);
		}
		System.out.println(" Initial itemsets: " + itemsets);
		double averageCost = Double.POSITIVE_INFINITY;

		// Structural EM
		int maxWalkCount = 0;
		for (int iteration = 1; iteration <= MAX_STRUCTURE_ITERATIONS; iteration++) {

			// Learn structure
			System.out.println("\n+++++ Structural Optimization Step "
					+ iteration);
			final boolean maxedOut = learnStructureStep(averageCost, itemsets,
					transactions, tree);
			if (maxedOut)
				maxWalkCount++;
			else
				maxWalkCount = 0;

			// Optimize parameters of new structure
			if (iteration % OPTIMIZE_PARAMS_EVERY == 0)
				averageCost = expectationMaximizationStep(itemsets,
						transactions);

			// Break if structure step has failed STOP_AFTER_MAX_WALKS times
			if (maxWalkCount == STOP_AFTER_MAX_WALKS) {
				expectationMaximizationStep(itemsets, transactions);
				break;
			}

		}

		return itemsets;
	}

	/**
	 * Find optimal parameters for given set of itemsets and store in itemsets
	 * 
	 * @return average cost per transaction
	 *         <p>
	 *         NB. zero probability itemsets are dropped
	 */
	public static double expectationMaximizationStep(
			final HashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions) {

		double averageCost = 0;
		HashMap<Itemset, Double> prevItemsets = itemsets;
		final double n = transactions.size();

		double norm = 1;
		while (norm > OPTIMIZE_TOL) {

			// Set up storage
			final HashMap<Itemset, Double> newItemsets = Maps.newHashMap();
			final Multiset<Itemset> allCoverings = ConcurrentHashMultiset
					.create();

			// Parallel E-step and M-step combined
			final FutureThreadPool<Double> ftp = new FutureThreadPool<Double>();
			for (final Transaction transaction : transactions) {

				final HashMap<Itemset, Double> parItemsets = prevItemsets;
				ftp.pushTask(new Callable<Double>() {
					@Override
					public Double call() {
						final Set<Itemset> covering = Sets.newHashSet();
						final double cost = inferGreedy(covering, parItemsets,
								transaction);
						allCoverings.addAll(covering);
						return cost;
					}
				});

			}
			final List<Double> costs = ftp.getCompletedTasks();
			averageCost = StatsUtil.sum(costs) / n;

			// Normalise probabilities
			for (final Itemset set : allCoverings.elementSet()) {
				newItemsets.put(set, allCoverings.count(set) / n);
			}

			// If set has stabilised calculate norm(p_prev - p_new)
			if (prevItemsets.size() == newItemsets.size()) {
				norm = 0;
				for (final Itemset set : prevItemsets.keySet()) {
					norm += Math.pow(
							prevItemsets.get(set) - newItemsets.get(set), 2);
				}
				norm = Math.sqrt(norm);
			}

			prevItemsets = newItemsets;
		}

		itemsets.clear();
		itemsets.putAll(prevItemsets);
		System.out.println("\n***** Parameter Optimization Step");
		System.out.println(" Parameter Optimal Itemsets: " + itemsets);
		System.out.println(" Average cost: " + averageCost);
		return averageCost;
	}

	// TODO keep a set of previous suggestions for efficiency?
	public static boolean learnStructureStep(final double averageCost,
			final HashMap<Itemset, Double> itemsets,
			final List<Transaction> transactions, final ItemsetTree tree) {

		// Try and find better itemset to add
		final double n = transactions.size();
		System.out.print(" Structural candidate itemsets: ");

		int iteration;
		for (iteration = 0; iteration < MAX_RANDOM_WALKS; iteration++) {

			// Candidate itemset
			final Itemset set = tree.randomWalk();
			System.out.print(set + ", ");

			// Skip empty candidates and candidates already present
			if (!set.isEmpty() && !itemsets.keySet().contains(set)) {

				System.out.print("\n potential candidate: " + set);
				// Estimate itemset probability (M-step assuming always
				// included)
				double p = 0;
				for (final Transaction transaction : transactions) {
					// TODO does this assumption make sense
					if (transaction.getItems().containsAll(set.getItems())) {
						p++;
					}
				}
				p = p / n;

				// Add itemset
				itemsets.put(set, p);

				// Find cost in parallel
				final FutureThreadPool<Double> ftp = new FutureThreadPool<Double>();
				for (final Transaction transaction : transactions) {

					ftp.pushTask(new Callable<Double>() {
						@Override
						public Double call() {
							final Set<Itemset> covering = Sets.newHashSet();
							return inferGreedy(covering, itemsets, transaction);
						}
					});
				}

				final List<Double> costs = ftp.getCompletedTasks();
				final double curCost = StatsUtil.sum(costs) / n;
				System.out.print(", candidate cost: " + curCost);

				if (curCost < averageCost) { // found better set of itemsets
					System.out.print("\n Candidate Accepted.");
					break;
				} // otherwise keep trying
				itemsets.remove(set);
				System.out.print("\n Structural candidate itemsets: ");
			}

		}
		System.out.println("\n Structure Optimal Itemsets: " + itemsets);

		if (iteration == MAX_RANDOM_WALKS)
			return true;
		return false;
	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering.
	 * <p>
	 * This is an O(log(n))-approximation algorithm where n is the number of
	 * elements in the transaction.
	 */
	public static double inferGreedy(final Set<Itemset> covering,
			final HashMap<Itemset, Double> itemsets,
			final Transaction transaction) {

		// TODO priority queue implementation?
		double totalCost = 0;
		final Set<Integer> coveredItems = Sets.newHashSet();
		final List<Integer> transactionItems = transaction.getItems();

		while (!coveredItems.containsAll(transactionItems)) {

			double minCostPerItem = Double.POSITIVE_INFINITY;
			Itemset bestSet = null;
			double bestCost = -1;

			for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {

				int notCovered = 0;
				for (final Integer item : entry.getKey().getItems()) {
					if (!coveredItems.contains(item)) {
						notCovered++;
					}
				}

				final double cost = -Math.log(entry.getValue());
				final double costPerItem = cost / notCovered;

				if (costPerItem < minCostPerItem
						&& transactionItems.containsAll(entry.getKey()
								.getItems())) { // Don't over-cover
					minCostPerItem = costPerItem;
					bestSet = entry.getKey();
					bestCost = cost;
				}

			}

			if (bestSet != null) {
				covering.add(bestSet);
				coveredItems.addAll(bestSet.getItems());
				totalCost += bestCost;
			} else { // Allow incomplete coverings
				if (totalCost == 0) // no covering is bad
					totalCost = Double.POSITIVE_INFINITY;
				break;
			}

		}

		return totalCost;
	}

	/**
	 * Infer ML parameters to explain transaction using Primal-Dual
	 * approximation and store in covering.
	 * <p>
	 * This is an O(mn) run-time f-approximation algorithm, where m is the no.
	 * elements to cover, n is the number of sets and f is the frequency of the
	 * most frequent element in the sets.
	 */
	public static double inferPrimalDual(final Set<Itemset> covering,
			final HashMap<Itemset, Double> itemsets,
			final Transaction transaction) {

		double totalCost = 0;
		final List<Integer> notCoveredItems = Lists.newArrayList(transaction
				.getItems());

		// Calculate costs
		final HashMap<Itemset, Double> costs = Maps.newHashMap();
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			costs.put(entry.getKey(), -Math.log(entry.getValue()));
		}

		while (!notCoveredItems.isEmpty()) {

			double minCost = Double.POSITIVE_INFINITY;
			Itemset bestSet = null;

			// Pick random element
			final int index = rand.nextInt(notCoveredItems.size());
			final Integer element = notCoveredItems.get(index);

			// Increase dual of element as much as possible
			for (final Entry<Itemset, Double> entry : costs.entrySet()) {

				if (entry.getKey().getItems().contains(element)) {

					final double cost = entry.getValue();
					if (cost < minCost
							&& transaction.getItems().containsAll(
									entry.getKey().getItems())) { // don't
																	// over-cover
						minCost = cost;
						bestSet = entry.getKey();
					}

				}
			}

			if (bestSet != null) {
				covering.add(bestSet);
				notCoveredItems.removeAll(bestSet.getItems());
				totalCost += minCost;
			} else { // Allow incomplete coverings
				if (totalCost == 0) // no covering is bad
					totalCost = Double.POSITIVE_INFINITY;
				break;
			}

			// Make dual of element binding
			for (final Itemset set : costs.keySet()) {
				if (set.getItems().contains(element)) {
					final double cost = costs.get(set);
					costs.put(set, cost - minCost);
				}
			}

		}

		return totalCost;
	}

	/**
	 * Infer ML parameters to explain transaction exactly using ILP and store in
	 * covering.
	 * <p>
	 * This is an NP-hard problem.
	 */
	// TODO use Guava's filter to filter out sets
	public static double inferILP(final Set<Itemset> covering,
			final LinkedHashMap<Itemset, Double> itemsets,
			final Transaction transaction) {

		// Load solver if necessary
		if (solver == null)
			solver = SolverFactory.getSolver("CPLEX");

		// Filter out sets containing items not in transaction
		final LinkedHashMap<Itemset, Double> filteredItemsets = Maps
				.newLinkedHashMap(itemsets);
		for (final Map.Entry<Itemset, Double> entry : itemsets.entrySet()) {
			if (transaction.getItems().containsAll(entry.getKey().getItems()))
				filteredItemsets.put(entry.getKey(), entry.getValue());
		}

		final int probSize = filteredItemsets.size();

		// Set up cost vector
		int i = 0;
		final double[] costs = new double[probSize];
		for (final double p : filteredItemsets.values()) {
			costs[i] = -Math.log(p);
			i++;
		}

		// Set objective sum(c_s*z_s)
		final LinearProgram lp = new LinearProgram(costs);

		// Add covering constraint
		for (final Integer item : transaction.getItems()) {

			i = 0;
			final double[] cover = new double[probSize];
			for (final Itemset set : filteredItemsets.keySet()) {

				// at least one set covers item
				if (set.getItems().contains(item)) {
					cover[i] = 1;
				}
				i++;
			}
			lp.addConstraint(new LinearBiggerThanEqualsConstraint(cover, 1.,
					"cover"));

		}

		// Set all variables to binary
		for (int j = 0; j < probSize; j++) {
			lp.setBinary(j);
		}

		System.out.println(lp.convertToCPLEX());

		// Solve
		lp.setMinProblem(true);
		final double[] sol = solver.solve(lp);

		// Add chosen sets to covering
		i = 0;
		double totalCost = 0;
		for (final Itemset set : filteredItemsets.keySet()) {
			if (doubleToBoolean(sol[i])) {
				covering.add(set);
				totalCost += costs[i] * sol[i];
			}
			i++;
		}

		// no covering is bad
		if (totalCost == 0)
			totalCost = Double.POSITIVE_INFINITY;

		return totalCost;
	}

	/** Round double to boolean */
	private static boolean doubleToBoolean(final double d) {
		if ((int) Math.round(d) == 1)
			return true;
		return false;
	}

	// TODO don't read all transactions into memory
	public static List<Transaction> readTransactions(final File inputFile)
			throws IOException {

		final List<Transaction> transactions = Lists.newArrayList();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#'
					|| line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the transaction into items
			final String[] lineSplited = line.split(" ");
			// create a structure for storing the transaction
			final Transaction transaction = new Transaction();
			// for each item in the transaction
			for (int i = 0; i < lineSplited.length; i++) {
				// convert the item to integer and add it to the structure
				transaction.add(Integer.parseInt(lineSplited[i]));

			}
			transactions.add(transaction);

		}
		// close the input file
		LineIterator.closeQuietly(it);

		return transactions;
	}

	/**
	 * This method scans the input database to calculate the support of single
	 * items.
	 * 
	 * @param inputFile
	 *            the input file
	 * @return a multiset for storing the support of each item
	 */
	private static Multiset<Integer> scanDatabaseToDetermineFrequencyOfSingleItems(
			final File inputFile) throws IOException {

		final Multiset<Integer> singletons = HashMultiset.create();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#'
					|| line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the line into items
			final String[] lineSplited = line.split(" ");
			// for each item
			for (final String itemString : lineSplited) {
				// increase the support count of the item
				singletons.add(Integer.parseInt(itemString));
			}
		}
		// close the input file
		LineIterator.closeQuietly(it);

		return singletons;
	}

	private static List<Rule> generateAssociationRules(
			final HashMap<Itemset, Double> itemsets) {

		final List<Rule> rules = Lists.newArrayList();

		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			final HashSet<Integer> setForRecursion = Sets.newHashSet(entry
					.getKey().getItems());
			recursiveGenRules(rules, setForRecursion, new HashSet<Integer>(),
					entry.getValue());
		}

		return rules;
	}

	private static void recursiveGenRules(final List<Rule> rules,
			final HashSet<Integer> antecedent,
			final HashSet<Integer> consequent, final double prob) {

		// Stop if no more rules to generate
		if (antecedent.isEmpty())
			return;

		// Add rule
		if (!antecedent.isEmpty() && !consequent.isEmpty())
			rules.add(new Rule(antecedent, consequent, prob));

		// Recursively generate more rules
		for (final Integer element : antecedent) {
			final HashSet<Integer> newAntecedent = Sets.newHashSet(antecedent);
			newAntecedent.remove(element);
			final HashSet<Integer> newConsequent = Sets.newHashSet(consequent);
			newConsequent.add(element);
			recursiveGenRules(rules, newAntecedent, newConsequent, prob);
		}

	}

}