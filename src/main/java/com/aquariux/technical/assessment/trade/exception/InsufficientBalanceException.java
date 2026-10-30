package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends TradeException {
    public InsufficientBalanceException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);   // 422
    }
}