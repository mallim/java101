package org.mallim.java101.springasync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.CompletableFuture;

@Slf4j
@SpringBootApplication
@EnableAsync
public class Application implements CommandLineRunner {

    @Autowired
    GitHubLookupService gitHubLookupService;

    @Override
    public void run(String... args) throws Exception {
        // Start the clock
        long start = System.currentTimeMillis();

        // Kick of multiple, asynchronous lookups
        CompletableFuture<User> page1 = gitHubLookupService.findUser("PivotalSoftware");
        CompletableFuture<User> page2 = gitHubLookupService.findUser("CloudFoundry");
        CompletableFuture<User> page3 = gitHubLookupService.findUser("Spring-Projects");

        // Wait until they are all done
        //while (!(page1.isDone() && page2.isDone() && page3.isDone())) {
        //  Thread.sleep(10); //10-millisecond pause between each check
        //}

        //wait until all they are completed.
        CompletableFuture.allOf(page1,page2,page3).join();
        //I could join as well if interested.

        // Print results, including elapsed time
        log.info("Elapsed time: " + (System.currentTimeMillis() - start) +" ms");
        log.info("get 1 = {}", page1.get());
        log.info("get 2 = {}", page2.get());
        log.info("get 3 = {}", page3.get());
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
