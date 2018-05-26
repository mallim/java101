package org.mallim.java101.reactor;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

@Slf4j
public class Consumer<T> extends BaseSubscriber {

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        log.debug("Consumer: hookOnSubscribe.");
        request(1);
    }

    @Override
    protected void hookOnNext(Object value) {
        log.debug("Consumer: hookOnNext " + value);
        request(1);
    }
}
