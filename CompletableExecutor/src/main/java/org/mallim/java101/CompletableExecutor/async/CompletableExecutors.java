package org.mallim.java101.CompletableExecutor.async;

import java.util.concurrent.ExecutorService;

/**
 * Inspired by:
 * http://binkley.blogspot.fr/2014/12/completablefuture-and-executorservice.html
 */
public final class CompletableExecutors {

    public static CompletableExecutorService completable(ExecutorService delegate) {
        return new DelegatingCompletableExecutorService(delegate);
    }

}
