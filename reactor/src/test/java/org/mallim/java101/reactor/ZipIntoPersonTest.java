package org.mallim.java101.reactor;

import lombok.Value;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Based on https://musigma.blog/2016/11/21/reactor.html
 */
public class ZipIntoPersonTest {

    @Value
    private class Person {
        private final String firstName;
        private final String lastName;
    }

    Flux<Person> zipIntoPersons(Flux<String> firstNames, Flux<String> lastNames) {
        return Flux.zip(firstNames, lastNames)
                .map(pair -> new Person(pair.getT1(), pair.getT2()));
    }

    @Test
    public void zipIntoPerson() {
        Flux<String> firstNames = Flux.just("FirstName1", "FirstName2");
        Flux<String> lastNames = Flux.just("LastName1", "LastName2");

        List<Person> elements = new ArrayList<>();
        Flux.zip(firstNames, lastNames)
                .map(pair -> new Person(pair.getT1(), pair.getT2()))
                .subscribe(elements::add);

        assertThat(elements).hasSize(2);
    }

    @Test
    public void fasterStreamMono() {
        Mono<String> mono1 = Mono.just("s");
        Mono<String> mono2 = Mono.just("t");

        // get an item from whichever one can provide it faster
        StepVerifier.create(mono1.or(mono2))
                .expectNext("s")
                .expectComplete()
                .verify();
    }

}
