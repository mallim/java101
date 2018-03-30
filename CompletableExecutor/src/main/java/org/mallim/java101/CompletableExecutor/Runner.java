package org.mallim.java101.CompletableExecutor;

import lombok.extern.slf4j.Slf4j;
import org.mallim.java101.CompletableExecutor.service.AsyncService;
import org.springframework.boot.CommandLineRunner;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Slf4j
public class Runner implements CommandLineRunner {

    private AsyncService asyncService;

    public Runner(AsyncService asyncService) {
        this.asyncService = asyncService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Going to start run...");
        IntStream.rangeClosed(1, 10)
                .mapToObj(__ -> asyncService.asyncTimeoutGreeting().exceptionally(Throwable::getMessage))
                .forEach(this::printResult);
        log.info("Running completed.");
    }

    private void printResult(CompletableFuture<String> future) {
        future.thenRun(() -> log.info(future.join()));
    }
}
