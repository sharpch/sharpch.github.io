

# Part A
## Requirements

A limit order book stores customer orders on a price time priority basis.
The highest bid and lowest offer are considered "best" with all other orders stacked in price levels behind. In this test, the best order is considered to be at level 1.

- Given an Order, add it to the OrderBook (order additions are expected to occur extremely frequently)
- Given an order id, remove an Order from the OrderBook (order deletions are expected to occur at approximately 60% of the rate of order additions)
- Given an order id and a new size, modify an existing order in the book to use the new size (size modifications do not affect time priority)
- Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best price for a given side). For example, given side=B and level=2 return the second best bid price
- Given a side and a level return the total size available for that level
- Given a side return all the orders from that side of the book, in level- and time-order

## Design
### Order
- Use of double may not accurately represent every price that could be entered into the order book, a form of scaled decimal is recommended
- An enum could better represent side to avoid checking for invalid characters
- Used record class for sake of brevity to represent order
 
### OrderBook
I chose a sorted heap structure with a sequence representing each level. My initial consideration was a PriorityQueue but does not guarantee iteration ordering

Style wise I default to using final for method parameters, just a safety measure to force avoidance of reassignment side effects within the implementation

# Part B

Please suggest (but do not implement) modifications or additions to the Order and/or OrderBook classes to make them better suited to support real-life, latency-sensitive trading operations.

Reduce object creation to attempt to achieve steady state memory.
Some different approaches
- Object Pooling
- Collapse data structures into a new single collection class
- Use primitive arrays for storage
- Avoid all autoboxing

Allow order sizes to be directly mutated, avoiding object creation and 

# JMH Benchmark
Only a basic test run for adds, would need full public interface coverage.
Could set up performance assertions in CI build

| Benchmark                            | (orderCount) | Mode | Cnt | Score    | Error   | Units |
|--------------------------------------|--------------|------|-----|----------|---------|-------|
| OrderBookBenchmark.addOrderBenchmark | 100          | avgt | 25  | 1.846    | ± 0.077 | us/op |
| OrderBookBenchmark.addOrderBenchmark | 1000         | avgt | 25  | 32.089   | ± 1.853 | us/op |
| OrderBookBenchmark.addOrderBenchmark | 10000        | avgt | 25  | 548.494  | ± 7.422 | us/op |