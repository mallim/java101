package org.mallim.java101.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Article at http://www.baeldung.com/reactor-core
 * <p>
 * From https://github.com/eugenp/tutorials/blob/master/reactor-core/src/test/java/com/baeldung/reactor/ReactorIntegrationTest.java
 */
@Slf4j
public class ReactorIntegrationTest {

    @Test
    public void givenFlux_whenSubscribing_thenStream() throws InterruptedException {

        List<Integer> elements = new ArrayList<>();

        Flux.just(1, 2, 3, 4)
                .log()
                .map(i -> {
                    log.info("inside map, {} : {}", i, Thread.currentThread());
                    return i * 2;
                })
                .subscribe(elements::add);

        assertThat(elements).containsExactlyInAnyOrder(2, 4, 6, 8);
    }

    @Test
    public void givenFlux_whenZipping_thenCombine() {
        List<String> elements = new ArrayList<>();

        Flux.just(1, 2, 3, 4)
                .log()
                .map(i -> i * 2)
                .zipWith(Flux.range(0, Integer.MAX_VALUE).log(), (two, one) -> String.format("First Flux: %d, Second Flux: %d", one, two))
                .subscribe(elements::add);

        assertThat(elements).containsExactly(
                "First Flux: 0, Second Flux: 2",
                "First Flux: 1, Second Flux: 4",
                "First Flux: 2, Second Flux: 6",
                "First Flux: 3, Second Flux: 8");
    }

    @Test
    public void givenFlux_whenApplyingBackPressure_thenPushElementsInBatches() throws InterruptedException {

        List<Integer> elements = new ArrayList<>();

        Flux.just(1, 2, 3, 4)
                .log()
                .map(i -> i * 2)
                .onBackpressureBuffer()
                .subscribe(new Subscriber<Integer>() {
                    private Subscription s;
                    int onNextAmount;

                    @Override
                    public void onSubscribe(final Subscription s) {
                        this.s = s;
                        s.request(2);
                    }

                    @Override
                    public void onNext(final Integer integer) {
                        elements.add(integer);
                        onNextAmount++;
                        if (onNextAmount % 2 == 0) {
                            s.request(2);
                        }
                    }

                    @Override
                    public void onError(final Throwable t) {
                    }

                    @Override
                    public void onComplete() {
                        int ham = 2;
                    }
                });

        assertThat(elements).containsExactly(2, 4, 6, 8);
    }

    @Test
    public void givenFlux_whenInParallel_thenSubscribeInDifferentThreads() throws InterruptedException {
        List<String> threadNames = new ArrayList<>();

        Flux.just(1, 2, 3, 4)
                .log()
                .map(i -> Thread.currentThread().getName())
                .subscribeOn(Schedulers.parallel())
                .subscribe(threadNames::add);

        Thread.sleep(1000);

        assertThat(threadNames).containsExactly("parallel-1", "parallel-1", "parallel-1", "parallel-1");
    }

    @Test
    public void givenConnectableFlux_whenConnected_thenShouldStream() {

        List<Integer> elements = new ArrayList<>();

        final ConnectableFlux<Integer> publish = Flux.just(1, 2, 3, 4).publish();

        publish.subscribe(elements::add);

        assertThat(elements).isEmpty();

        publish.connect();

        assertThat(elements).containsExactly(1, 2, 3, 4);
    }
}
