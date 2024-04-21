package org.sharpch.orderbook;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook orderBook;

    /**
     * Custom matcher to test equivalence of order lists based upon their order ids
     */
    private static class OrderListIdMatcher extends TypeSafeMatcher<List<Order>> {
        private final List<Order> expected;

        public OrderListIdMatcher(final List<Order> expected) {
            this.expected = expected;
        }

        @Override
        protected boolean matchesSafely(final List<Order> actual) {
            if (actual.size() != expected.size()) {
                return false;
            }

            Iterator<Order> actualOrders = actual.iterator();
            Iterator<Order> expectedOrders = expected.iterator();
            while (actualOrders.hasNext() && expectedOrders.hasNext()) {
                // Access elements from both lists using their iterators
                Order actualOrder = actualOrders.next();
                Order expectedOrder = expectedOrders.next();
                if (actualOrder.id() != expectedOrder.id()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("orders in the same order as ").appendValue(expected);
        }
    }

    @BeforeEach
    public void setUp() {
        orderBook = new OrderBook(100); // Initial capacity for HashMaps
    }

    private void populateOrderBook(final int size) {
        for (long id = 1; id <= size; id++) {
            // ensure 2 orders at each level with different sizes
            // prices will step up in twos 1, 3, 7 ...
            double price = id;
            if (id % 2 == 0) {
                price = price - 1.0;
            }
            Order buyOrder = new Order(id, 'B', price, id);
            orderBook.addOrder(buyOrder);
            Order sellOrder = new Order(id + size, 'S', price + size, id + size);
            orderBook.addOrder(sellOrder);
        }
        System.out.println(orderBook.getOrdersForSide('B'));
        System.out.println(orderBook.getOrdersForSide('S'));
    }

    private static boolean isSorted(final List<Order> orders, final Comparator<Order> orderComparator) {
        if (orders.isEmpty() || orders.size() == 1) {
            return true;
        }

        Iterator<Order> iter = orders.iterator();
        Order current, previous = iter.next();
        while (iter.hasNext()) {
            current = iter.next();
            if (orderComparator.compare(previous, current) > 0) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    private static class OrderPriceComparator implements Comparator<Order> {
        // Whilst just used for testing could move to Order
        @Override
        public int compare(final Order firstOrder, final Order secondOrder) {
            return Double.compare(firstOrder.price(), secondOrder.price());
        }
    }

    @Test
    void testOrderBookIsSorted() {
        // Create random orders with different sides, 10 different prices and 10 different sizes
        for (long id = 1; id < 10000; id++) {
            Order randomOrder = new Order(id,
                    ThreadLocalRandom.current().nextBoolean() ? 'B' : 'S',
                    ThreadLocalRandom.current().nextLong(1, 10),
                    ThreadLocalRandom.current().nextLong(1, 10));
            orderBook.addOrder(randomOrder);
        }
        List<Order> buys = orderBook.getOrdersForSide('B');
        assertTrue(isSorted(buys, new OrderPriceComparator()));
        List<Order> sells = orderBook.getOrdersForSide('S');
        assertTrue(isSorted(sells, new OrderPriceComparator().reversed()));
    }

    @Test
    void testAddOrder() {
        Order buyOrder = new Order(2, 'B', 60.6, 600);
        orderBook.addOrder(buyOrder);
        assertEquals(1, orderBook.getOrdersForSide('B').size());

        Order sellOrder = new Order(1, 'S', 50.01, 100);
        orderBook.addOrder(sellOrder);
        assertEquals(1, orderBook.getOrdersForSide('S').size());
    }

    @Test
    void testRemoveOrder() {
        populateOrderBook(4);

        long buyLevelTotal1 = orderBook.getTotalSizeForLevel('B', 1);
        Order buyOrder = orderBook.removeOrder(1);
        long buyLevelTotal2 = orderBook.getTotalSizeForLevel('B', 1);

        assertEquals(1, buyOrder.id());
        assertEquals(buyLevelTotal1 - buyOrder.size(), buyLevelTotal2);
        assertEquals(3, orderBook.getOrdersForSide('B').size());

        long sellLevelTotal1 = orderBook.getTotalSizeForLevel('S', 2);
        Order sellOrder = orderBook.removeOrder(6);
        long sellLevelTotal2 = orderBook.getTotalSizeForLevel('S', 2);

        assertEquals(6, sellOrder.id());
        assertEquals(sellLevelTotal1 - sellOrder.size(), sellLevelTotal2);
        assertEquals(3, orderBook.getOrdersForSide('S').size());

        Order noOrder = orderBook.removeOrder(21);
        assertNull(noOrder);
    }

    @Test
    void testUpdateSize() {
        populateOrderBook(10);
        List<Order> buys = orderBook.getOrdersForSide('B');
        List<Order> sells = orderBook.getOrdersForSide('S');

        Order oldBuyOrder = orderBook.updateOrderSize(5, 25);
        assertEquals(5, oldBuyOrder.size());
        assertThat(orderBook.getOrdersForSide('B'), new OrderListIdMatcher(buys));
        assertThat(orderBook.getOrdersForSide('S'), new OrderListIdMatcher(sells));

        Order oldSellOrder = orderBook.updateOrderSize(15, 55);
        assertEquals(15, oldSellOrder.size());
        assertThat(orderBook.getOrdersForSide('B'), new OrderListIdMatcher(buys));
        assertThat(orderBook.getOrdersForSide('S'), new OrderListIdMatcher(sells));

        assertNull(orderBook.updateOrderSize(21, 25));
    }

    @Test
    void testGetPriceForSideLevel() {
        // Args
        assertThrows(IllegalArgumentException.class, () -> orderBook.getPriceForSideLevel('X', 1));
        assertThrows(IllegalArgumentException.class, () -> orderBook.getPriceForSideLevel('B', 0));

        // Can't find level
        assertTrue(Double.isNaN(orderBook.getPriceForSideLevel('B', 1)));

        populateOrderBook(4);
        assertEquals(1.0, orderBook.getPriceForSideLevel('B', 1));
        assertEquals(3.0, orderBook.getPriceForSideLevel('B', 2));
        assertEquals(7.0, orderBook.getPriceForSideLevel('S', 1));
        assertEquals(5.0, orderBook.getPriceForSideLevel('S', 2));
    }

    @Test
    void testGetTotalSizeForLevel() {
        assertThrows(IllegalArgumentException.class, () -> orderBook.getTotalSizeForLevel('X', 1));
        assertThrows(IllegalArgumentException.class, () -> orderBook.getTotalSizeForLevel('B', 0));

        assertEquals(0, orderBook.getTotalSizeForLevel('B', 1));
        assertEquals(0, orderBook.getTotalSizeForLevel('S', 1));

        Order sellOrder1 = new Order(1, 'S', 50.0, 100);
        Order sellOrder2 = new Order(2, 'S', 50.0, 150);
        Order sellOrder3 = new Order(3, 'S', 40.0, 20);

        Order buyOrder1 = new Order(4, 'B', 60.0, 200);
        Order buyOrder2 = new Order(5, 'B', 60.0, 300);
        Order buyOrder3 = new Order(6, 'B', 50.0, 10);

        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(sellOrder2);
        orderBook.addOrder(sellOrder3);

        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(buyOrder2);

        orderBook.addOrder(buyOrder3);

        assertEquals(250, orderBook.getTotalSizeForLevel('S', 1));
        assertEquals(20, orderBook.getTotalSizeForLevel('S', 2));
        assertEquals(0, orderBook.getTotalSizeForLevel('S', 3));

        assertEquals(10, orderBook.getTotalSizeForLevel('B', 1));
        assertEquals(500, orderBook.getTotalSizeForLevel('B', 2));
        assertEquals(0, orderBook.getTotalSizeForLevel('B', 3));
    }

    @Test
    void testGetOrdersForSells() {
        // We expect sell orders to have their highest price at the top of the order book
        Order sellOrder1 = new Order(1, 'S', 50.0, 100);
        Order sellOrder2 = new Order(2, 'S', 50.0, 150);
        Order sellOrder3 = new Order(3, 'S', 60.0, 200);

        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(sellOrder2); // Same level but ordered after
        orderBook.addOrder(sellOrder3); // Should be at head

        List<Order> sellOrders = orderBook.getOrdersForSide('S');
        assertEquals(3, sellOrders.size());
        assertEquals(sellOrder3, sellOrders.get(0));
        assertEquals(sellOrder1, sellOrders.get(1));
        assertEquals(sellOrder2, sellOrders.get(2));
    }

    @Test
    void testGetOrdersForBuys() {
        assertThrows(IllegalArgumentException.class, () -> orderBook.getOrdersForSide('X'));

        // We expect sell orders to have their highest price levels at the top of the order book
        Order sellOrder1 = new Order(1, 'B', 50.0, 100);
        Order sellOrder2 = new Order(2, 'B', 50.0, 150);
        Order sellOrder3 = new Order(3, 'B', 40.0, 200);

        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(sellOrder2); // Same level but ordered after
        orderBook.addOrder(sellOrder3); // Should be at head

        List<Order> sellOrders = orderBook.getOrdersForSide('B');
        assertEquals(3, sellOrders.size());
        assertEquals(sellOrder3, sellOrders.get(0));
        assertEquals(sellOrder1, sellOrders.get(1));
        assertEquals(sellOrder2, sellOrders.get(2));

        System.out.println(sellOrders);
    }
}