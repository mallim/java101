package org.mallim.java101.CompletableExecutor;

import org.mallim.java101.CompletableExecutor.service.AsyncService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    @Bean
    public AsyncService asyncService() {
        return new AsyncService();
    }

    @Bean
    public Runner runner(AsyncService asyncService) {
        return new Runner(asyncService);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
