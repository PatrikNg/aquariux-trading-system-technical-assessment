package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class InvalidTradeException extends TradeException {
    public InvalidTradeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);            // 400
    }
}
