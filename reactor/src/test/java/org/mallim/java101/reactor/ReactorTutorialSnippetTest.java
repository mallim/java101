package org.mallim.java101.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Source from https://github.com/mohitsinha/tutorials/blob/master/reactor-example/src/test/java/com/example/ReactorTests.java
 */
public class ReactorTutorialSnippetTest {

    @Test
    public void empty() {
        Mono<String> emptyMono = Mono.empty();
        StepVerifier.create(emptyMono).verifyComplete();

        Flux<String> emptyFlux = Flux.empty();
        StepVerifier.create(emptyFlux).verifyComplete();
    }

    @Test
    public void initialized() {
        Mono<String> nonEmptyMono = Mono.just("Joel");
        StepVerifier.create(nonEmptyMono).expectNext("Joel").verifyComplete();

        Flux<String> nonEmptyFlux = Flux.just("John", "Mike", "Sarah");
        StepVerifier.create(nonEmptyFlux).expectNext("John", "Mike", "Sarah").verifyComplete();

        Flux<String> fluxFromIterable = Flux.fromIterable(Arrays.asList("Tom", "Hardy", "Bane"));
        StepVerifier.create(fluxFromIterable).expectNext("Tom", "Hardy", "Bane").verifyComplete();
    }

    @Test
    public void operations() {
        Mono<String> monoMap = Mono.just("James").map(s -> s.toLowerCase());
        StepVerifier.create(monoMap).expectNext("james").verifyComplete();

        Flux<String> fluxMapFilter = Flux.just("Joel", "Kyle")
                .filter(s -> s.toUpperCase().startsWith("K")) //  passed only names starting with ‘k’.
                .map(s -> s.toLowerCase()); // emitting the names in lower-case
        StepVerifier.create(fluxMapFilter).expectNext("kyle").verifyComplete();
    }

    @Test
    public void zipping() {
        Flux<String> titles = Flux.just("Mr.", "Mrs.");
        Flux<String> firstNames = Flux.just("John", "Jane");
        Flux<String> lastNames = Flux.just("Doe", "Blake");

        Flux<String> names = Flux
                .zip(titles, firstNames, lastNames) // combining them in a strict sequence
                .map(t -> t.getT1() + " " + t.getT2() + " " + t.getT3()); // concatenated them to create a Flux emitting the full names

        StepVerifier.create(names).expectNext("Mr. John Doe", "Mrs. Jane Blake").verifyComplete();

        Flux<Long> delay = Flux.interval(Duration.ofMillis(5));
        Flux<String> firstNamesWithDelay = firstNames.zipWith(delay, (s, l) -> s);

        Flux<String> namesWithDelay = Flux
                .zip(titles, firstNamesWithDelay, lastNames)
                .map(t -> t.getT1() + " " + t.getT2() + " " + t.getT3());

        StepVerifier.create(namesWithDelay).expectNext("Mr. John Doe", "Mrs. Jane Blake").verifyComplete();
    }

    /**
     * Interleaving is a concept in which data is written non-sequentially to improve performance.
     */
    @Test
    public void interleave() {
        Flux<Long> delay = Flux.interval(Duration.ofMillis(5));
        Flux<String> alphabetsWithDelay = Flux.just("A", "B").zipWith(delay, (s, l) -> s);
        Flux<String> alphabetsWithoutDelay = Flux.just("C", "D");

        Flux<String> interleavedFlux = alphabetsWithDelay
                .mergeWith(alphabetsWithoutDelay); // merges them into an interleaved sequence
        StepVerifier.create(interleavedFlux).expectNext("C", "D", "A", "B").verifyComplete();

        Flux<String> nonInterleavedFlux = alphabetsWithDelay
                .concatWith(alphabetsWithoutDelay); //  merges them into a non-interleaved sequence
        StepVerifier.create(nonInterleavedFlux).expectNext("A", "B", "C", "D").verifyComplete();
    }

    @Test
    public void block() {
        // subscribe to a Publisher indefinitely and get the values in a blocking manner
        String name = Mono.just("Jesse").block();
        assertEquals("Jesse", name);

        Iterator<String> namesIterator = Flux.just("Tom", "Peter").toIterable().iterator();
        assertEquals("Tom", namesIterator.next());
        assertEquals("Peter", namesIterator.next());
        assertFalse(namesIterator.hasNext());
    }
}
