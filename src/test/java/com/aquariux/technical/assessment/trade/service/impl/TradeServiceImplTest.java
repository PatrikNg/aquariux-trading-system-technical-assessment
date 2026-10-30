package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.exception.PriceUnavailableException;
import com.aquariux.technical.assessment.trade.exception.InvalidTradeException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.service.WalletServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long BTC_ID = 1L;
    private static final Long USDT_ID = 3L;
    private static final Long BTCUSDT_PAIR_ID = 1L;

    @Mock
    private TradeMapper tradeMapper;

    @Mock
    private CryptoPairMapper cryptoPairMapper;

    @Mock
    private CryptoPriceMapper cryptoPriceMapper;

    @Mock
    private WalletServiceInterface walletService;

    @InjectMocks
    private TradeServiceImpl tradeService;

    private CryptoPair btcUsdt;
    private CryptoPrice btcPrice;

    @BeforeEach
    void setUp() {
        btcUsdt = new CryptoPair();
        btcUsdt.setId(BTCUSDT_PAIR_ID);
        btcUsdt.setBaseSymbolId(BTC_ID);
        btcUsdt.setQuoteSymbolId(USDT_ID);
        btcUsdt.setPairName("BTCUSDT");
        btcUsdt.setActive(true);

        btcPrice = new CryptoPrice();
        btcPrice.setCryptoPairId(BTCUSDT_PAIR_ID);
        btcPrice.setBidPrice(new BigDecimal("45000.00"));
        btcPrice.setAskPrice(new BigDecimal("45100.00"));
        btcPrice.setBidSource("BINANCE");
        btcPrice.setAskSource("HUOBI");
    }

    private TradeRequest request(TradeType type, String pairName, String quantity) {
        TradeRequest request = new TradeRequest();
        request.setUserId(USER_ID);
        request.setTradeType(type);
        request.setPairName(pairName);
        request.setQuantity(quantity == null ? null : new BigDecimal(quantity));
        return request;
    }

    @Test
    void executeTrade_Buy_ShouldPayUsdtAtAskPriceAndReceiveBtc() {
        // Given
        when(cryptoPairMapper.findActiveByPairName("BTCUSDT")).thenReturn(btcUsdt);
        when(cryptoPriceMapper.findLatestByPairId(BTCUSDT_PAIR_ID)).thenReturn(btcPrice);
        doAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            trade.setId(100L);
            return null;
        }).when(tradeMapper).insertTrade(any(Trade.class));

        // When
        TradeResponse response = tradeService.executeTrade(request(TradeType.BUY, "BTCUSDT", "0.5"));

        // Then: 0.5 x 45100 (ask) = 22550 USDT
        BigDecimal expectedTotal = new BigDecimal("22550.00000000");
        verify(walletService).deductBalance(USER_ID, USDT_ID, expectedTotal);
        verify(walletService).addBalance(USER_ID, BTC_ID, new BigDecimal("0.5"));

        assertThat(response.getTradeId()).isEqualTo(100L);
        assertThat(response.getPairName()).isEqualTo("BTCUSDT");
        assertThat(response.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(response.getPrice()).isEqualByComparingTo("45100.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(response.getPriceSource()).isEqualTo("HUOBI");
        assertThat(response.getTradeTime()).isNotNull();
    }

    @Test
    void executeTrade_Sell_ShouldGiveBtcAndReceiveUsdtAtBidPrice() {
        // Given
        when(cryptoPairMapper.findActiveByPairName("BTCUSDT")).thenReturn(btcUsdt);
        when(cryptoPriceMapper.findLatestByPairId(BTCUSDT_PAIR_ID)).thenReturn(btcPrice);

        // When
        TradeResponse response = tradeService.executeTrade(request(TradeType.SELL, "BTCUSDT", "1"));

        // Then: 1 x 45000 (bid) = 45000 USDT
        BigDecimal expectedTotal = new BigDecimal("45000.00000000");
        verify(walletService).deductBalance(USER_ID, BTC_ID, new BigDecimal("1"));
        verify(walletService).addBalance(USER_ID, USDT_ID, expectedTotal);

        assertThat(response.getTradeType()).isEqualTo(TradeType.SELL);
        assertThat(response.getPrice()).isEqualByComparingTo("45000.00");
        assertThat(response.getPriceSource()).isEqualTo("BINANCE");
    }

    @Test
    void executeTrade_ShouldSaveTradeRecordWithCorrectFields() {
        // Given
        when(cryptoPairMapper.findActiveByPairName("BTCUSDT")).thenReturn(btcUsdt);
        when(cryptoPriceMapper.findLatestByPairId(BTCUSDT_PAIR_ID)).thenReturn(btcPrice);
        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);

        // When
        tradeService.executeTrade(request(TradeType.SELL, "BTCUSDT", "0.25"));

        // Then
        verify(tradeMapper).insertTrade(captor.capture());
        Trade saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getCryptoPairId()).isEqualTo(BTCUSDT_PAIR_ID);
        assertThat(saved.getTradeType()).isEqualTo("SELL");
        assertThat(saved.getQuantity()).isEqualByComparingTo("0.25");
        assertThat(saved.getPrice()).isEqualByComparingTo("45000.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("11250.00");
        assertThat(saved.getTradeTime()).isNotNull();
    }

    @Test
    void executeTrade_ShouldRoundTotalAmountToEightDecimals() {
        // Given
        btcPrice.setAskPrice(new BigDecimal("45123.456789"));
        when(cryptoPairMapper.findActiveByPairName("BTCUSDT")).thenReturn(btcUsdt);
        when(cryptoPriceMapper.findLatestByPairId(BTCUSDT_PAIR_ID)).thenReturn(btcPrice);

        // When: 0.00012345 x 45123.456789 = 5.57049074060205
        TradeResponse response = tradeService.executeTrade(request(TradeType.BUY, "BTCUSDT", "0.00012345"));

        // Then
        assertThat(response.getTotalAmount().scale()).isEqualTo(8);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("5.57049074");
    }

    @Test
    void executeTrade_WhenInsufficientBalance_ShouldThrowAndNotSaveTrade() {
        // Given
        when(cryptoPairMapper.findActiveByPairName("BTCUSDT")).thenReturn(btcUsdt);
        when(cryptoPriceMapper.findLatestByPairId(BTCUSDT_PAIR_ID)).thenReturn(btcPrice);
        doThrow(new InsufficientBalanceException("Insufficient balance for user " + USER_ID))
                .when(walletService).deductBalance(USER_ID, BTC_ID, new BigDecimal("100"));

        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.SELL, "BTCUSDT", "100")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletService, never()).addBalance(any(), any(), any());
        verify(tradeMapper, never()).insertTrade(any());
    }

    @Test
    void executeTrade_WhenPairNotSupported_ShouldThrowInvalidTradeException() {
        // Given
        when(cryptoPairMapper.findActiveByPairName("DOGEUSDT")).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "DOGEUSDT", "1")))
                .isInstanceOf(InvalidTradeException.class)
                .hasMessageContaining("DOGEUSDT");

        verifyNoInteractions(cryptoPriceMapper, walletService, tradeMapper);
    }

    @Test
    void executeTrade_WhenNoPriceAvailable_ShouldThrowAndNotTouchWallet() {
        // Given
        when(cryptoPairMapper.findActiveByPairName("BTCUSDT")).thenReturn(btcUsdt);
        when(cryptoPriceMapper.findLatestByPairId(BTCUSDT_PAIR_ID)).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "BTCUSDT", "1")))
                .isInstanceOf(PriceUnavailableException.class);

        verifyNoInteractions(walletService, tradeMapper);
    }

    @Test
    void executeTrade_WhenQuantityIsZero_ShouldThrowAndDoNothing() {
        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "BTCUSDT", "0")))
                .isInstanceOf(InvalidTradeException.class);

        verifyNoInteractions(cryptoPairMapper, cryptoPriceMapper, walletService, tradeMapper);
    }

    @Test
    void executeTrade_WhenQuantityIsNull_ShouldThrowAndDoNothing() {
        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "BTCUSDT", null)))
                .isInstanceOf(InvalidTradeException.class);

        verifyNoInteractions(cryptoPairMapper, cryptoPriceMapper, walletService, tradeMapper);
    }

    @Test
    void executeTrade_WhenQuantityIsNegative_ShouldThrowAndDoNothing() {
        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.SELL, "BTCUSDT", "-1")))
                .isInstanceOf(InvalidTradeException.class);

        verifyNoInteractions(cryptoPairMapper, cryptoPriceMapper, walletService, tradeMapper);
    }

    @Test
    void executeTrade_WhenQuantityIsMoreThen8Digits_ShouldThrowAndDoNothing() {
        // When / Then
        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.SELL, "BTCUSDT", "1.123456789")))
                .isInstanceOf(InvalidTradeException.class);

        verifyNoInteractions(cryptoPairMapper, cryptoPriceMapper, walletService, tradeMapper);
    }
}