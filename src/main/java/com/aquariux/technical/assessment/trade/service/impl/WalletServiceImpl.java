package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import com.aquariux.technical.assessment.trade.dto.response.WalletBalanceResponse;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.aquariux.technical.assessment.trade.service.WalletServiceInterface;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletServiceInterface {

    private final UserWalletMapper userWalletMapper;

    public List<WalletBalanceResponse> getUserWalletBalances(Long userId) {
        List<UserWalletDto> wallets = userWalletMapper.findByUserId(userId);
        
        return wallets.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private WalletBalanceResponse mapToResponse(UserWalletDto wallet) {
        WalletBalanceResponse response = new WalletBalanceResponse();
        response.setSymbol(wallet.getSymbol());
        response.setName(wallet.getName());
        response.setBalance(wallet.getBalance());
        return response;
    }

    @Override
    public void deductBalance(Long userId, Long symbolId, BigDecimal amount) {
        if (userWalletMapper.deductBalance(userId, symbolId, amount) == 0) {
            throw new InsufficientBalanceException("Insufficient balance for user " + userId);
        }
    }

    @Override 
    public void addBalance(Long userId, Long symbolId, BigDecimal amount) {
        if (userWalletMapper.addBalance(userId, symbolId, amount) == 0) {
            // first time acquiring this coin
            try {
                userWalletMapper.insertWallet(userId, symbolId, amount);
            } catch (DuplicateKeyException e) {
                // another trade created the wallet first
                userWalletMapper.addBalance(userId, symbolId, amount);
            }
        }
    }
}