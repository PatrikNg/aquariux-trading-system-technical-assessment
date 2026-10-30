package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.PriceUnavailableException;
import com.aquariux.technical.assessment.trade.exception.InvalidTradeException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import com.aquariux.technical.assessment.trade.service.WalletServiceInterface;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeServiceInterface {

    private final TradeMapper tradeMapper;
    private final CryptoPairMapper cryptoPairMapper;
    private final CryptoPriceMapper cryptoPriceMapper;
    private final WalletServiceInterface walletService;

    @Override
    @Transactional
    public TradeResponse executeTrade(TradeRequest request) {
        // Validate required fields
        if (request.getUserId() == null 
        || request.getTradeType() == null
        || request.getPairName() == null 
        || request.getQuantity() == null) {
            throw new InvalidTradeException("userId, tradeType, pairName and quantity are required.");
        }

        // Validate trade quantity
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0 
        || request.getQuantity().scale() > 8) {
            throw new InvalidTradeException("Trade quantity must be greater than zero and have at most 8 decimal places.");
        }

        // Validate crypto is supported and active
        CryptoPair pair = cryptoPairMapper.findActiveByPairName(request.getPairName());
        if (pair == null) {
            throw new InvalidTradeException("Unsupported or inactive pair: " + request.getPairName());
        }

        // Get the latest price for the crypto pair and validate it
        CryptoPrice price = cryptoPriceMapper.findLatestByPairId(pair.getId());
        if (price == null) {
            throw new PriceUnavailableException("No price data available for the requested pair.");
        }

        // Validate that the bid and ask prices are positive and that the bid price is not greater than the ask price
        if (price.getBidPrice().signum() <= 0 
        || price.getAskPrice().signum() <= 0
        || price.getBidPrice().compareTo(price.getAskPrice()) > 0) {
            throw new PriceUnavailableException("Invalid market price for " + pair.getPairName());
        }

        // Get the trade information and calculate the total amount based on the trade type (BUY or SELL)
        boolean isBuy = request.getTradeType() == TradeType.BUY;
        BigDecimal executionPrice = isBuy ? price.getAskPrice() : price.getBidPrice();
        BigDecimal quantity = request.getQuantity();
        BigDecimal totalAmount = quantity.multiply(executionPrice).setScale(8, RoundingMode.HALF_UP);

        switch (request.getTradeType()) {
            case BUY -> {
                executeBuy(request.getUserId(), pair, quantity, totalAmount);
            }
            case SELL -> {
                executeSell(request.getUserId(), pair, quantity, totalAmount);
            }
        }

        Trade trade = new Trade();
        trade.setUserId(request.getUserId());
        trade.setCryptoPairId(pair.getId());
        trade.setTradeType(request.getTradeType().name());
        trade.setQuantity(quantity);
        trade.setPrice(executionPrice); 
        trade.setTotalAmount(totalAmount);
        trade.setTradeTime(LocalDateTime.now());
        tradeMapper.insertTrade(trade);

        return TradeResponse.mapToResponse(trade, pair.getPairName(), isBuy ? price.getAskSource() : price.getBidSource());
    }

    private void executeBuy(Long userId, CryptoPair pair, BigDecimal quantity, BigDecimal totalAmount) {
        // Deduct quote currency (USDT) and add base currency (BTC/ETH)
        walletService.deductBalance(userId, pair.getQuoteSymbolId(), totalAmount);
        walletService.addBalance(userId, pair.getBaseSymbolId(), quantity);
    }

    private void executeSell(Long userId, CryptoPair pair, BigDecimal quantity, BigDecimal totalAmount) {
        // Deduct base currency (BTC/ETH) and add quote currency (USDT)
        walletService.deductBalance(userId, pair.getBaseSymbolId(), quantity);
        walletService.addBalance(userId, pair.getQuoteSymbolId(), totalAmount);
    }
}