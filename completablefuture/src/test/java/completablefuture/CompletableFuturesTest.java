package completablefuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * https://gist.github.com/spullara/5897605
 */
public class CompletableFuturesTest {

    @Test
    @DisplayName("Gist 4 - testCompletableFutures")
    public void testCompletableFutures() throws Exception {

        AtomicBoolean executed = new AtomicBoolean(false);
        CompletableFuture<String> future = supplyAsync(() -> {
            sleep(1000);
            return "Done.";
        });
        CompletableFuture<String> future1 = supplyAsync(() -> {
            sleep(900);
            return "Done2.";
        });
        CompletableFuture<String> future2 = supplyAsync(() -> "Constant");
        CompletableFuture<String> future3 = supplyAsync(() -> {
            sleep(500);
            throw new RuntimeException("CompletableFuture4");
        });
        CompletableFuture<String> future4 = new CompletableFuture<>();
        future4.completeExceptionally(new RuntimeException("CompletableFuture5"));
        CompletableFuture<String> future5 = supplyAsync(() -> {
            executed.set(true);
            sleep(1000);
            return "Done.";
        });
        future5.cancel(true);

        CompletableFuture<String> selected = select(future, future1, future3, future4);

        try {
            assertTrue(future5.isCancelled());
            assertTrue(future5.isDone());
            future5.get();
            fail("Was not cancelled");
        } catch (CancellationException ce) {
            if (executed.get()) {
                fail("Executed though cancelled immediately");
            }
        }

        CompletableFuture<String> result10 = new CompletableFuture<>();
        try {
            onFailure(future3, e -> {
                result10.complete("Failed");
            }).get(0, TimeUnit.SECONDS);
            fail("Didn't timeout");
        } catch (TimeoutException te) {
        }

        try {
            future4.thenApply(v -> null).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        CompletableFuture<String> result3 = new CompletableFuture<>();
        future.applyToEither(future1, v -> result3.complete("Selected: " + v));
        final CompletableFuture<String> result4 = new CompletableFuture<>();
        future1.applyToEither(future, v -> result4.complete("Selected: " + v));
        assertEquals("Selected: Done2.", result3.get());
        assertEquals("Selected: Done2.", result4.get());
        assertEquals("Done2.", selected.get());

        CompletableFuture<String> map1 = future.thenCombine(future1, (value1, value2) -> value1 + ", " + value2);
        CompletableFuture<String> map2 = future1.thenCombine(future, (value1, value2) -> value1 + ", " + value2);
        assertEquals("Done., Done2.", map1.get());
        assertEquals("Done2., Done.", map2.get());

        final CompletableFuture<String> result1 = new CompletableFuture<>();
        future.acceptEither(future3, s -> result1.complete("Selected: " + s));
        assertEquals("Selected: Done.", result1.get());
        assertEquals("Failed", result10.get());

        try {
            onFailure(future3.acceptEither(future4, e -> {
            }), e -> {
                result1.complete(e.getMessage());
            }).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        try {
            future.thenCombine(future3, (value1, value2) -> value1 + ", " + value2).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        assertEquals("Flatmapped: Constant", future1.thenCompose(v -> future2).thenApply(v -> "Flatmapped: " + v).get());

        CompletableFuture<String> result11 = new CompletableFuture<>();
        try {
            onFailure(future1.thenApply(v -> future3), e -> {
                result11.complete("Failed");
            }).get();
        } catch (ExecutionException ee) {
            assertEquals("Failed", result11.get());
        }

        CompletableFuture<String> result2 = new CompletableFuture<>();
        onFailure(future3.thenCompose(v -> future1), e -> {
            result2.complete("Flat map failed: " + e);
        });
        assertEquals("Flat map failed: java.util.concurrent.CompletionException: java.lang.RuntimeException: CompletableFuture4", result2.get());

        assertEquals("Done.", future.get(1, TimeUnit.DAYS));

        try {
            future3.get();
            fail("Didn't fail");
        } catch (ExecutionException e) {
        }

        try {
            future3.thenCombine(future, (a, b) -> null).get();
            fail("Didn't fail");
        } catch (ExecutionException e) {
        }

        CompletableFuture<String> result5 = new CompletableFuture<>();
        CompletableFuture<String> result6 = new CompletableFuture<>();
        onFailure(future.thenAccept(s -> result5.complete("onSuccess: " + s)),
                e -> result5.complete("onFailure: " + e))
                .thenRun(() -> result6.complete("Ensured"));
        assertEquals("onSuccess: Done.", result5.get());
        assertEquals("Ensured", result6.get());

        CompletableFuture<String> result7 = new CompletableFuture<>();
        CompletableFuture<String> result8 = new CompletableFuture<>();
        ensure(onFailure(future3.thenAccept(s -> result7.complete("onSuccess: " + s)), e -> {
            result7.complete("onFailure: " + e);
        }), () -> result8.complete("Ensured"));
        assertEquals("onFailure: java.util.concurrent.CompletionException: java.lang.RuntimeException: CompletableFuture4", result7.get());
        assertEquals("Ensured", result8.get());

        assertEquals("Was Rescued!", future3.exceptionally(e -> "Rescued!").thenApply(v -> "Was " + v).get());
        assertEquals("Was Constant", future2.exceptionally(e -> "Rescued!").thenApply(v -> "Was " + v).get());

        assertEquals(asList("Done.", "Done2.", "Constant"), collect( asList(future, future1, future2)).get());
        assertEquals(Arrays.<String>asList(), collect(new ArrayList<>()).get());
        try {
            assertEquals(asList("Done.", "Done2.", "Constant"), collect(asList(future, future3, future2)).get());
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        CompletableFuture<String> result9 = new CompletableFuture<>();
        future.thenAccept(v -> result9.complete("onSuccess: " + v));
        assertEquals("onSuccess: Done.", result9.get());
    }

    @Test
    public void testExceptionally() {
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Object> future = new CompletableFuture<>().exceptionally(t -> {
            called.set(true);
            return null;
        });
        future.completeExceptionally(new CancellationException());
        try {
            future.get();
        } catch (Exception e) {
            assertTrue( ! called.get(), "exceptionally called: " );
        }
    }

    private static <T> CompletableFuture<T> onFailure(CompletableFuture<T> future, Consumer<Throwable> call) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        future.handle((v, t) -> {
            if (t == null) {
                completableFuture.complete(v);
            } else {
                call.accept(t);
                completableFuture.completeExceptionally(t);
            }
            return null;
        });
        return completableFuture;
    }

    @SafeVarargs
    private final CompletableFuture<String> select(CompletableFuture<String>... completableFutures) {
        CompletableFuture<String> future = new CompletableFuture<>();
        for (CompletableFuture<String> completableFuture : completableFutures) {
            completableFuture.thenAccept(future::complete);
        }
        return future;
    }

    private CompletableFuture<List<String>> collect(List<CompletableFuture<String>> completableFutures) {
        CompletableFuture<List<String>> result = new CompletableFuture<>();
        int size = completableFutures.size();
        List<String> list = new ArrayList<>();
        if (size == 0) {
            result.complete(list);
        } else {
            for (CompletableFuture<String> completableFuture : completableFutures) {
                completableFuture.handle((s, t) -> {
                    if (t == null) {
                        list.add(s);
                        if (list.size() == size) {
                            result.complete(list);
                        }
                    } else {
                        result.completeExceptionally(t);
                    }
                    return s;
                });
            }
        }
        return result;
    }

    private static <T> CompletableFuture<T> ensure(CompletableFuture<T> future, Runnable call) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        future.handle((v, t) -> {
            if (t == null) {
                call.run();
                completableFuture.complete(v);
            } else {
                call.run();
                completableFuture.completeExceptionally(t);
            }
            return null;
        });
        return completableFuture;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

}
