package com.aquariux.technical.assessment.trade.dto.request;

import com.aquariux.technical.assessment.trade.enums.TradeType;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TradeRequest {
    private Long userId;
    private TradeType tradeType;
    private String pairName; // Storing the cryptocurrency pair to be traded
    private BigDecimal quantity; // crypto quantity to be traded
}