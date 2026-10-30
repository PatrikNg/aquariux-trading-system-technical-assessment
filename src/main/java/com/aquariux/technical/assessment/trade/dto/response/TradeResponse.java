package com.aquariux.technical.assessment.trade.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;

import lombok.Data;

@Data
public class TradeResponse {

    private Long tradeId;
    private Long userId;
    private String pairName;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;          // execution price (ask for BUY, bid for SELL)
    private BigDecimal totalAmount;    // quantity × price, in quote currency (USDT)
    private String priceSource;        // BINANCE / HUOBI
    private LocalDateTime tradeTime;

    public static TradeResponse mapToResponse(Trade trade, String pairName, String priceSource) {
        TradeResponse response = new TradeResponse();
        response.setTradeId(trade.getId());
        response.setUserId(trade.getUserId());
        response.setPairName(pairName);
        response.setTradeType(TradeType.valueOf(trade.getTradeType()));
        response.setQuantity(trade.getQuantity());
        response.setPrice(trade.getPrice());
        response.setTotalAmount(trade.getTotalAmount());
        response.setPriceSource(priceSource);
        response.setTradeTime(trade.getTradeTime());
        return response;
    }
}