package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class PriceUnavailableException extends TradeException {
    public PriceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);    // 503
    }
}
