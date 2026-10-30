# Assumptions & Design Decisions

## Trade semantics
- A trade is on one pair (BTCUSDT / ETHUSDT). `quantity` is in the base asset (BTC/ETH).
- BUY pays USDT at the latest best **ask**; SELL receives USDT at the latest best **bid**.
- `totalAmount = quantity × price`, rounded HALF_UP to 8 decimals (matches DECIMAL(20,8)).
- Quantity must be > 0 with at most 8 decimal places.
- Cross trades (e.g. BTC → ETH) are not supported; they require two trades via USDT.

## Wallets
- One row per (user, symbol). A wallet row is created on first acquisition.
- Balance deduction is a single conditional UPDATE (`balance >= amount`), so concurrent
  trades cannot overdraw a wallet.
- The whole trade (debit, credit, trade record) runs in one transaction.

## Prices
- The latest row in `crypto_prices` for the pair is used. Prices are not rejected for age,
  because the seed data is historical and exchange APIs may be unreachable when reviewing.

## Bugs found by me
- `@EnableScheduling` was missing, so prices were never refreshed.

## Bugs found by Claude
- `PriceUpdateScheduler` stored prices under the opposite pair and swapped bid/ask.
- Seed data for 2025-01-16 had swapped pair IDs and reversed bid/ask; corrected in
  `V5__Fix_corrupted_price_data.sql` (V2 left untouched to keep Flyway checksums valid).