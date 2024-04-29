package org.sharpch.orderbook;

/**
 * For brevity just use an immutable record.
 */
public record Order(long id, char side, double price, long size) {
    /**
     * Copy ctor with different size
     */
    public Order orderWithSize(final long size) {
        return new Order(id, side, price, size);
    }
}
