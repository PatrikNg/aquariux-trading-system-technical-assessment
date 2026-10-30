-- Jan 16 seed rows were stored under the wrong pair and with bid/ask reversed
-- (same defect as PriceUpdateScheduler). Correct them instead of deleting,
-- so price history is preserved.
UPDATE crypto_prices
SET crypto_pair_id = CASE crypto_pair_id WHEN 1 THEN 2 ELSE 1 END,
    bid_price  = ask_price,
    ask_price  = bid_price,
    bid_source = ask_source,
    ask_source = bid_source
WHERE created_at >= '2025-01-16 00:00:00' AND created_at < '2025-01-17 00:00:00';