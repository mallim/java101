package org.mallim.java101.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Article at https://www.infoq.com/articles/reactor-by-example
 */
@Slf4j
public class ReactorSnippetsTest {

    private static List<String> words = Arrays.asList(
            "the",
            "quick",
            "brown",
            "fox",
            "jumped",
            "over",
            "the",
            "lazy",
            "dog"
    );

    @Test
    public void simpleCreation() {

        Flux<String> fewWords = Flux.just("Hello", "World").log();

        List<String> elements = new ArrayList<>();
        Flux.fromIterable(words).log().subscribe(elements::add);

        StepVerifier
                .create(fewWords)
                .expectNext("Hello")
                .expectNext("World")
                .expectComplete()
                .verify();

        assertThat(elements).containsExactlyElementsOf(words);
    }

    @Test
    public void findingMissingLetter() {
        List<String> elements = new ArrayList<>();
        Flux<String> manyLetters = Flux
                .fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split("")))
                .distinct()
                .sort()
                .zipWith(Flux.range(1, Integer.MAX_VALUE),
                        (string, count) -> String.format("%2d. %s", count, string));

        manyLetters.subscribe(vo -> log.info(vo));
        manyLetters.subscribe(elements::add);

        assertThat(elements).hasSize(25);
        assertThat(elements).first().isEqualTo(" 1. a");
    }

    @Test
    public void restoringMissingLetter() {
        Mono<String> missing = Mono.just("s");
        Flux<String> allLetters = Flux
                .fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split("")))
                .concatWith(missing) // Add in the missing letter s
                .distinct()
                .sort()
                .zipWith(Flux.range(1, Integer.MAX_VALUE),
                        (string, count) -> String.format("%2d. %s", count, string));

        allLetters.subscribe(vo -> log.info(vo));

        List<String> elements = new ArrayList<>();
        allLetters.subscribe(elements::add);

        assertThat(elements).hasSize(26);
    }

    /**
     * This snippet prints "Hello", but fails to print the delayed "world" because the test terminates too early.
     */
    @Test
    public void shortCircuit() {
        Flux<String> helloPauseWorld =
                Mono.just("Hello")
                        .concatWith(Mono.just("world")
                                .delaySubscription(Duration.ofMillis(500)));

        helloPauseWorld.subscribe(vo -> log.info(vo));
    }

    /**
     * Solve that issue is by using one of the operators that revert back to the non-reactive world.
     * Specifically, toIterable and toStream will both produce a blocking instance.
     */
    @Test
    public void blocks() {
        Flux<String> helloPauseWorld =
                Mono.just("Hello")
                        .concatWith(Mono.just("world")
                                .delaySubscription(Duration.ofMillis(500)));

        helloPauseWorld.toStream()
                .forEach(vo -> log.info(vo));
    }

    @Test
    public void firstEmitting() {

        Mono<String> a = Mono.just("oops I'm late")
                .delaySubscription(Duration.ofMillis(450));

        Flux<String> b = Flux.just("let's get", "the party", "started")
                .delayElements(Duration.ofMillis(400)); //  short 400ms pause between each section

        /**
         * since the first value from the Flux comes in before the Mono's value, it is the Flux that ends up being played
         */
        Flux.first(a, b)
                .toIterable()
                .forEach(vo -> log.info(vo));
    }
}

