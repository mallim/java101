package completablefuture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TwentyExamplesTest {

    static Random random = new Random();

    ArrayList<String> numbers = new ArrayList<String>(Arrays.asList("0", "1", "2"));

    static ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        int count = 1;
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "custom-executor-" + count++);
        }
    });

    @Test
    @DisplayName("Example 1 - Hello World - Creating a Completed CompletableFuture")
    public void completedFutureExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message");
        assertTrue(cf.isDone());
        assertEquals("message", cf.getNow(null));
    }

    @Test
    @DisplayName("Example 2 - Running a Simple Asynchronous Stage")
    void runAsyncExample() {
        CompletableFuture cf = CompletableFuture.runAsync(() -> {
            assertTrue(Thread.currentThread().isDaemon());
            randomSleep();
        });
        assertFalse(cf.isDone());
        sleepEnough();
        assertTrue(cf.isDone());
    }

    @Test
    @DisplayName("Example 3 - Running a Simple Asynchronous Stage")
    public void thenApplyExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApply(s -> {
            assertFalse(Thread.currentThread().isDaemon());
            return s.toUpperCase();
        });
        assertEquals("MESSAGE", cf.getNow(null));
    }

    /**
     * Asynchronously Applying a Function on a Previous Stage
     */
    @DisplayName("Example 4 - Asynchronously Applying a Function on a Previous Stage")
    public void thenApplyAsyncExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            assertTrue(Thread.currentThread().isDaemon());
            randomSleep();
            return s.toUpperCase();
        });
        assertNull(cf.getNow(null));
        assertEquals("MESSAGE", cf.join());
    }

    @Test
    @DisplayName("Example 5 - Asynchronously Applying a Function on  a Previous Stage Using a Custom Executor")
    void thenApplyAsyncWithExecutorExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            assertTrue(Thread.currentThread().getName().startsWith("custom-executor-"));
            assertFalse(Thread.currentThread().isDaemon());
            randomSleep();
            return s.toUpperCase();
        }, executor);
        assertNull(cf.getNow(null));
        assertEquals("MESSAGE", cf.join());
    }

    @Test
    @DisplayName("6. Consuming the Result of the Previous Stage")
    void thenAcceptExample() {
        StringBuilder result = new StringBuilder();
        CompletableFuture.completedFuture("thenAccept message")
                .thenAccept(s -> result.append(s));
        assertTrue( result.length() > 0, "Result was empty" );
    }

    @Test
    @DisplayName("7. Asynchronously Consuming the Result of the Previous Stage")
    public void thenAcceptAsyncExample() {
        StringBuilder result = new StringBuilder();
        CompletableFuture cf = CompletableFuture.completedFuture("thenAcceptAsync message")
                .thenAcceptAsync(s -> result.append(s));
        cf.join();
        assertTrue( result.length() > 0, "Result was empty" );
    }

    /**
     * https://gist.github.com/spullara/5897605
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    @DisplayName("GIST 1 - testCancellation")
    public void testCancellation() throws ExecutionException, InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean handled = new AtomicBoolean();
        AtomicBoolean handleCalledWithValue = new AtomicBoolean();
        CompletableFuture<String> other = supplyAsync(() -> "Doomed value");
        CompletableFuture<String> future = supplyAsync(() -> {
            // sleep(1000);
            sleepEnough();
            return "Doomed value";
        }).exceptionally(t -> {
            cancelled.set(true);
            return null;
        }).thenCombine(other, (a, b) -> a + ", " + b).handle((v, t) -> {
            if (t == null) {
                handleCalledWithValue.set(true);
            }
            handled.set(true);
            return null;
        });
        sleep(100);
        future.cancel(true);
        sleep(1000);
        try {
            future.get();
            fail("Should have thrown");
        } catch (CancellationException ce) {
            log.error("future cancelled: " + future.isCancelled());
            log.error("other cancelled: " + other.isCancelled());
            log.error("exceptionally called: " + cancelled.get());
            log.error("handle called: " + handled.get());
            log.error("handle called with value: " + handleCalledWithValue.get());
        }
    }

    /**
     * https://gist.github.com/spullara/5897605
     *
     */
    @Test
    @DisplayName("GIST 2 - testExceptions")
    public void testExceptions() {

        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException());
        future.exceptionally(t -> {
            assertNull( t );
            throw new CompletionException(t);
        });

        CompletableFuture<Object> future2 = supplyAsync(() -> {
            throw new RuntimeException();
        });
        future2.exceptionally(t -> {
            assertNull( t );
            throw new CompletionException(t);
        });

        CompletableFuture<String> future3 = supplyAsync(() -> "test");
        future3.thenAccept(t -> {
            throw new RuntimeException();
        }).exceptionally(t -> {
            assertNull( t );
            throw new CompletionException(t);
        });
    }

    /**
     * https://gist.github.com/spullara/5897605
     *
     */
    @Test
    @DisplayName("GIST 3 - testCompleteExceptionally")
    public void testCompleteExceptionally() throws ExecutionException, InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean handled = new AtomicBoolean();
        AtomicBoolean handleCalledWithValue = new AtomicBoolean();
        CompletableFuture<String> other = supplyAsync(() -> "Doomed value");
        CompletableFuture<String> future = supplyAsync(() -> {
            sleepEnough();
            return "Doomed value";
        }).exceptionally(t -> {
            cancelled.set(true);
            return null;
        }).thenCombine(other, (a, b) -> a + ", " + b).handle((v, t) -> {
            if (t == null) {
                handleCalledWithValue.set(true);
            }
            handled.set(true);
            return null;
        });
        sleep(100);
        future.completeExceptionally(new CancellationException());
        sleep(1000);
        try {
            future.get();
            fail("Should have thrown");
        } catch (CancellationException ce) {
            assertTrue( future.isCancelled(), "future cancelled: " );
            assertTrue( ! other.isCancelled(), "other cancelled: " );
            assertTrue( ! cancelled.get(), "exceptionally called: " );
            assertTrue( ! handled.get(), "handle called: ");
            assertTrue( ! handleCalledWithValue.get(), "handle called with value: " );
        }
    }

    @Test
    @DisplayName("Example 10 - Applying a Function to the Result of Either of Two Completed Stages")
    public void applyToEitherExample() {
        String original = "Message";
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture(original)
                .thenApplyAsync(s -> delayedUpperCase(s));
        CompletableFuture<String> cf2 = cf1.applyToEither(
                CompletableFuture.completedFuture(original).thenApplyAsync(s -> delayedLowerCase(s)),
                s -> s + " from applyToEither");
        assertTrue(cf2.join().endsWith(" from applyToEither"));
    }

    @Test
    @DisplayName("Example 11. Consuming the Result of Either of Two Completed Stages")
    public void acceptEitherExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture cf = CompletableFuture.completedFuture(original)
                .thenApplyAsync(s -> delayedUpperCase(s))
                .acceptEither(CompletableFuture.completedFuture(original).thenApplyAsync(s -> delayedLowerCase(s)),
                        s -> result.append(s).append("acceptEither"));
        cf.join();
        assertTrue( result.toString().endsWith("acceptEither"), "Result was empty" );
    }

    @Test
    @DisplayName("12. Running a Runnable Upon Completion of Both Stages")
    void runAfterBothExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture.completedFuture(original).thenApply(String::toUpperCase).runAfterBoth(
                CompletableFuture.completedFuture(original).thenApply(String::toLowerCase),
                () -> result.append("done"));
        assertTrue( result.length() > 0, "Result was empty" );
    }

    @Test
    @DisplayName("13. Accepting the Results of Both Stages in a BiConsumer")
    public void thenAcceptBothExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture.completedFuture(original).thenApply(String::toUpperCase).thenAcceptBoth(
                CompletableFuture.completedFuture(original).thenApply(String::toLowerCase),
                (s1, s2) -> result.append(s1 + s2));
        assertEquals("MESSAGEmessage", result.toString());
    }

    @Test
    @DisplayName("14. Applying a BiFunction on Results of Both Stages")
    public void thenCombineExample() {
        String original = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(original).thenApply(s -> delayedUpperCase(s))
                .thenCombine(CompletableFuture.completedFuture(original).thenApply(s -> delayedLowerCase(s)),
                        (s1, s2) -> s1 + s2);
        assertEquals("MESSAGEmessage", cf.getNow(null));
    }

    @Test
    @DisplayName("15. Asynchronously Applying a BiFunction on Results of Both Stages")
    public void thenCombineAsyncExample() {
        String original = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(original)
                .thenApplyAsync(s -> delayedUpperCase(s))
                .thenCombine(CompletableFuture.completedFuture(original).thenApplyAsync(s -> delayedLowerCase(s)),
                        (s1, s2) -> s1 + s2);
        assertEquals("MESSAGEmessage", cf.join());
    }

    @Test
    @DisplayName("16. Composing CompletableFutures")
    public void thenComposeExample() {
        String original = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(original).thenApply(s -> delayedUpperCase(s))
                .thenCompose(upper -> CompletableFuture.completedFuture(original).thenApply(s -> delayedLowerCase(s))
                        .thenApply(s -> upper + s));
        assertEquals("MESSAGEmessage", cf.join());
    }

    @Test
    @DisplayName("17. Creating a Stage That Completes When Any of Several Stages Completes")
    public void anyOfExample() {
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");
        List<CompletableFuture> futures = messages.stream()
                .map(msg -> CompletableFuture.completedFuture(msg).thenApply(s -> delayedUpperCase(s)))
                .collect(Collectors.toList());
        CompletableFuture.anyOf(futures.toArray(new CompletableFuture[futures.size()])).whenComplete((res, th) -> {
            if(th == null) {
                assertTrue(isUpperCase((String) res));
                result.append(res);
            }
        });
        assertTrue(result.length() > 0, "Result was empty" );
    }

    @Test
    @DisplayName("18. Creating a Stage That Completes When All Stages Complete")
    public void allOfExample() {
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");
        List<CompletableFuture<String>> futures = messages.stream()
                .map(msg -> CompletableFuture.completedFuture(msg).thenApply(s -> delayedUpperCase(s)))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).whenComplete((v, th) -> {
            futures.forEach(cf -> assertTrue(isUpperCase(cf.getNow(null))));
            result.append("done");
        });
        assertTrue( result.length() > 0, "Result was empty" );
    }

    @Test
    @DisplayName("19. Creating a Stage That Completes Asynchronously When All Stages Complete")
    public void allOfAsyncExample() {
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");
        List<CompletableFuture<String>> futures = messages.stream()
                .map(msg -> CompletableFuture.completedFuture(msg).thenApplyAsync(s -> delayedUpperCase(s)))
                .collect(Collectors.toList());
        CompletableFuture allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .whenComplete((v, th) -> {
                    futures.forEach(cf -> assertTrue(isUpperCase(cf.getNow(null))));
                    result.append("done");
                });
        allOf.join();
        assertTrue( result.length() > 0, "Result was empty" );
    }

    /**
     * http://javasampleapproach.com/java/java-8/java-8-multiple-completablefutures
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    @DisplayName("JAVASAMPLE 1 - thenCompose() can chain 2 CompletableFutures by using the result which is returned from the invoking future")
    void testCompose() throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = createCF(2); // inside future
        CompletableFuture<String> combinedFuture = future.thenCompose(TwentyExamplesTest::calculateCF);

        combinedFuture.thenAccept(result -> log.debug("accept: " + result));
        // check results
        assertEquals( "2", future.get(), "Future result>> " );
        log.debug( "combinedFuture result>> {}", combinedFuture.get() );
    }

    @Test


    private static CompletableFuture<String> calculateCF(String s) {

        return CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                log.debug("> inside new Future");
                return "new Completable Future: " + s;
            }
        });
    }

    private CompletableFuture<String> createCF(int index) {
        return CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                try {
                    System.out.println("inside future: waiting for detecting index: " + index + "...");
                    System.out.println("inside future: done...");

                    return numbers.get(index);
                } catch (Throwable e) {
                    return "not detected";
                }
            }
        });
    }

    private String delayedUpperCase(String s) {
        randomSleep();
        return s.toUpperCase();
    }

    private String delayedLowerCase(String s) {
        randomSleep();
        return s.toLowerCase();
    }

    private void randomSleep() {
        try {
            sleep(random.nextInt(1000));
        } catch (InterruptedException e) {
            // ...
        }
    }

    private void sleepEnough() {
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            // ...
        }
    }

    private static boolean isUpperCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
