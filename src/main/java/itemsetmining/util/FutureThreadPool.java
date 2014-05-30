/**
 * 
 */
package itemsetmining.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.google.common.collect.Lists;

/**
 * A wrapper around Java's thread pool using Future. Uses Callable so tasks can
 * return values.
 * 
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 * 
 */
public final class FutureThreadPool<T> {

	private static final Logger LOGGER = Logger
			.getLogger(FutureThreadPool.class.getName());

	private final ExecutorService threadPool;
	private List<Future<T>> futures;

	public static final int NUM_THREADS = (int) Runtime.getRuntime()
			.availableProcessors();

	/**
	 * 
	 */
	public FutureThreadPool() {
		threadPool = Executors.newFixedThreadPool(NUM_THREADS);
	}

	/**
	 * @param nThreads
	 *            number of parallel threads to use.
	 */
	public FutureThreadPool(final int nThreads) {
		threadPool = Executors.newFixedThreadPool(nThreads);
	}

	public List<T> getCompletedTasks() {
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (final InterruptedException e) {
			LOGGER.warning("Thread Pool Interrupted "
					+ ExceptionUtils.getFullStackTrace(e));
		}
		final List<T> outputs = Lists.newArrayList();
		for (final Future<T> future : futures) {
			try {
				outputs.add(future.get());
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}
		return outputs;
	}

	/**
	 * Interrupt the execution of any future tasks, returning tasks that have
	 * been interrupted.
	 */
	public List<Runnable> interrupt() {
		return threadPool.shutdownNow();
	}

	public void pushAll(final Collection<Callable<T>> tasks) {
		futures = new ArrayList<Future<T>>();
		for (final Callable<T> task : tasks) {
			futures.add(threadPool.submit(task));
		}
	}

	/**
	 * Push a task to be executed.
	 * 
	 * @param task
	 */
	public void pushTask(final Callable<T> task) {
		checkArgument(!threadPool.isShutdown(),
				"Cannot submit task to thread pool that has already been shutdown.");
		if (futures == null) {
			futures = new ArrayList<Future<T>>();
		}
		futures.add(threadPool.submit(task));
	}

}
