package org.sharpch.orderbook;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class OrderBookBenchmark {

    @State(Scope.Benchmark)
    public static class SetupStateAdds {
        OrderBook orderBook;
        Order[] orders;

        @Param({"100", "1000", "10000"})
        int orderCount;

        @Setup(Level.Trial)
        public void setup() {
            orderBook = new OrderBook(orderCount);
            orders = new Order[orderCount];
            for (int i = 0; i < orderCount; i++) {
                orders[i] = new Order(i + 1, 'B', i * 10.0, 10);
            }
        }
    }

    @Benchmark
    public void addOrderBenchmark(SetupStateAdds state, Blackhole blackhole) {
        for (Order order : state.orders) {
            state.orderBook.addOrder(order);
            blackhole.consume(order);
        }
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}