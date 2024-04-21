package org.sharpch.orderbook;

import java.util.*;

/**
 * Given an Order, add it to the OrderBook (order additions are expected to occur extremely frequently)
 * <p>
 * Given an order id, remove an Order from the OrderBook (order deletions are expected to occur at approximately 60% of the rate of order additions)
 * <p>
 * Given an order id and a new size, modify an existing order in the book to use the new size (size modifications do not affect time priority)
 * <p>
 * Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best price for a given side). For example, given side=B and level=2 return the second best bid price
 * <p>
 * Given a side and a level return the total size available for that level
 * <p>
 * Given a side return all the orders from that side of the book, in level and time-order
 */
public class OrderBook {

    /**
     * Use an OrderLevel to be placed in a sorted order book.
     * By using a LinkedHashMap we maintain insertion order, including replacements for the same key
     */
    private final static class OrderLevel {
        private final SequencedMap<Long, Order> ordersById = new LinkedHashMap<>();
        private long total = 0; // Keep a total for faster retrieval
        final double price;

        private OrderLevel(final double price) {
            this.price = price;
        }

        private void upsertOrder(final Order order) {
            Order priorOrder = ordersById.put(order.id(), order);
            if (priorOrder != null) {
                total -= priorOrder.size();
            }
            total += order.size();
        }

        private Order removeOrder(final long id) {
            return ordersById.remove(id);
        }

        private Order getOrder(final long id) {
            return ordersById.get(id);
        }

        private Collection<Order> getOrders() {
            return ordersById.values();
        }
    }

    private final SortedMap<Double, OrderLevel> buyOrders = new TreeMap<>(Comparator.naturalOrder());
    private final SortedMap<Double, OrderLevel> sellOrders = new TreeMap<>(Comparator.reverseOrder());
    private final Map<Long, OrderLevel> orderLevelsById;

    public OrderBook(final int initialCapacity) {
        orderLevelsById = new HashMap<>(initialCapacity, 0.8f);
    }

    /**
     * Given an Order, add it to the OrderBook
     */
    public synchronized void addOrder(final Order order) {
        char side = order.side();
        if (side == 'B') {
            addOrderToOrderBookSide(order, buyOrders, orderLevelsById);
        } else {
            addOrderToOrderBookSide(order, sellOrders, orderLevelsById);
        }
    }

    private void addOrderToOrderBookSide(final Order order, final SortedMap<Double, OrderLevel> orderBookSide, final Map<Long, OrderLevel> ordersById) {
        OrderLevel orderLevel = orderBookSide.get(order.price());
        if (orderLevel == null) {
            orderLevel = new OrderLevel(order.price());
            orderBookSide.put(order.price(), orderLevel);
        }
        orderLevel.upsertOrder(order);
        ordersById.put(order.id(), orderLevel);
    }


    /**
     * Given an order id, remove an Order from the OrderBook
     *
     * @return removed order from order book for given id, else null
     */
    public synchronized Order removeOrder(long id) {
        OrderLevel orderLevel = orderLevelsById.get(id);
        Order order = null;
        if (orderLevel != null) {
            order = orderLevel.removeOrder(id);
        }
        return order;
    }

    /**
     * Given an order id and a new size, modify an existing order in the book to use the new size
     * (size modifications do not affect time priority)
     *
     * @return original order for the given id if it exists, else null
     */
    public Order updateOrderSize(final long id, final long size) {
        OrderLevel orderLevel = orderLevelsById.get(id);
        Order order = null;

        if (orderLevel != null) {
            order = orderLevel.getOrder(id);
            Order orderUpdate = order.orderWithSize(size);
            orderLevel.upsertOrder(orderUpdate);
        }
        return order;
    }


    private SortedMap<Double, OrderLevel> getOrderSideMapForSide(final char side) {
        SortedMap<Double, OrderLevel> orderSideMap;
        if (side == 'B') {
            orderSideMap = buyOrders;
        } else if (side == 'S') {
            orderSideMap = sellOrders;
        } else {
            throw new IllegalArgumentException("No such side [" + side + "]");
        }
        return orderSideMap;
    }

    private OrderLevel getOrderLevelForLevel(final int level, final SortedMap<Double, OrderLevel> orderSideMap) {
        if (level < 1) {
            throw new IllegalArgumentException("Level [" + level + "] must be > 0");
        }
        int currentDepth = 1;
        // Iterate N times to locate the Nth level
        for (OrderLevel orderLevel : orderSideMap.values()) {
            if (currentDepth++ == level) {
                return orderLevel;
            }
        }
        return null;
    }

    /**
     * Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best price for a given side). For example, given side=B and level=2 return the second best bid price
     *
     * @return price for the given level if it exists, else Double.NaN
     * @throws IllegalArgumentException for a bad side or level < 1
     */
    public double getPriceForSideLevel(final char side, final int level) {
        OrderLevel orderLevel = getOrderLevelForLevel(level, getOrderSideMapForSide(side));
        if (orderLevel != null) {
            return orderLevel.price;
        } else {
            return Double.NaN;
        }
    }


    /**
     * Given a side and a level return the total size available for that level
     * <p>
     * Assumption: Practically long is large enough for any total or orders and not expected to overflow.
     *
     * @return -1 if no such side or level exists, else a total.
     * @throws IllegalArgumentException for invalid side or negative level
     */
    public synchronized long getTotalSizeForLevel(final char side, final int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level must not be negative [" + level + "]");
        }
        SortedMap<Double, OrderLevel> orderSideMap;
        orderSideMap = getOrderSideMapForSide(side);
        OrderLevel orderLevel = getOrderLevelForLevel(level, orderSideMap);
        if (orderLevel == null) {
            return 0;
        }
        return orderLevel.total;
    }

    /**
     * Given a side return all the orders from that side of the book, in level and time order
     *
     * @throws IllegalArgumentException for invalid side
     */
    public List<Order> getOrdersForSide(final char side) {
        List<Order> orderListForSide = new ArrayList<>(100);
        SortedMap<Double, OrderLevel> orderSideMap = getOrderSideMapForSide(side);

        orderSideMap.values().forEach((orderLevel) -> orderListForSide.addAll(orderLevel.getOrders()));

        return orderListForSide;
    }
}
