package com.aquariux.technical.assessment.trade.service;

import com.aquariux.technical.assessment.trade.dto.response.WalletBalanceResponse;

import java.util.List;
import java.math.BigDecimal;

public interface WalletServiceInterface {
    List<WalletBalanceResponse> getUserWalletBalances(Long userId);
    void deductBalance(Long userId, Long symbolId, BigDecimal amount);
    void addBalance(Long userId, Long symbolId, BigDecimal amount);
}