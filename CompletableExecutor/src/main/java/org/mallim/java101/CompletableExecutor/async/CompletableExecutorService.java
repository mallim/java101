package org.mallim.java101.CompletableExecutor.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * DelegatingCompletableExecutorService {@code ExecutorService} to covariantly return {@code
 * CompletableFuture} in place of {@code Future}.
 */
public interface CompletableExecutorService extends ExecutorService {
    /**
     * @return a completable future representing pending completion of the
     * task, never missing
     */
    @Override
    <T> CompletableFuture<T> submit(Callable<T> task);

    /**
     * @return a completable future representing pending completion of the
     * task, never missing
     */
    @Override
    <T> CompletableFuture<T> submit(Runnable task, T result);

    /**
     * @return a completable future representing pending completion of the
     * task, never missing
     */
    @Override
    CompletableFuture<?> submit(Runnable task);
}
