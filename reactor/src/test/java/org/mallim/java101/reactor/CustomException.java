package org.mallim.java101.reactor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomException extends RuntimeException {
    private static final long serialVersionUID = -5970845585469454688L;

    public CustomException(String type) {
        log.error("{} : throw CustomException!", type);
    }
}
