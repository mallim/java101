package org.mallim.java101.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Adopted from https://www.e4developer.com/2018/04/11/getting-reactive-with-spring-boot-2-0-and-reactor/
 */
@Slf4j
public class FluxSinkTest {

    public class EventListener {

        int count = 0;
        FluxSink<String> sink;

        void generate() {
            while (count < 10) {
                sink.next("event " + count);
                count++;
            }
            count++;
        }

        public void register(FluxSink<String> sink) {
            this.sink = sink;
        }
    }

    @Test
    public void generateEventListener() {
        Flux<String> dynamicFlux = Flux.create(sink -> {
            EventListener eventListener = new EventListener();
            eventListener.register(sink);
            eventListener.generate();
        });

        List<String> elements = new ArrayList<>();
        dynamicFlux
                .subscribe(vo -> log.info(vo));

        dynamicFlux.subscribe(elements::add);

        assertThat(elements).hasSize(10);
        assertThat(elements).element(0).isEqualTo("event 0");
        assertThat(elements).last().isEqualTo("event 9");
    }
}
