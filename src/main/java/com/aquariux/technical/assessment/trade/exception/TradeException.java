package com.aquariux.technical.assessment.trade.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class TradeException extends RuntimeException {

    private final HttpStatus status;

    protected TradeException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}