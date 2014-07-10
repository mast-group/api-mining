/**
 * 
 */
package itemsetmining.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * A wrapper around Java's thread pool.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public final class ParallelThreadPool {

	private static final Logger LOGGER = Logger
			.getLogger(ParallelThreadPool.class.getName());

	private final ExecutorService threadPool;

	public static final int NUM_THREADS = Runtime.getRuntime()
			.availableProcessors();

	/**
	 * 
	 */
	public ParallelThreadPool() {
		threadPool = Executors.newFixedThreadPool(NUM_THREADS);
	}

	/**
	 * Interrupt the execution of any future tasks, returning tasks that have
	 * been interrupted.
	 */
	public List<Runnable> interrupt() {
		return threadPool.shutdownNow();
	}

	public void pushAll(final Collection<Runnable> tasks) {
		for (final Runnable task : tasks) {
			threadPool.execute(task);
		}
	}

	/**
	 * Push a task to be executed.
	 * 
	 * @param task
	 */
	public void pushTask(final Runnable task) {
		checkArgument(!threadPool.isShutdown(),
				"Cannot submit task to thread pool that has already been shutdown.");
		threadPool.execute(task);
	}

	public boolean waitForTermination() {
		threadPool.shutdown();
		try {
			return threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (final InterruptedException e) {
			LOGGER.warning("Thread Pool Interrupted "
					+ ExceptionUtils.getFullStackTrace(e));
		}
		return false;
	}

}
