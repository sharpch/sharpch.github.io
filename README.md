# Part A

## Requirements

*A limit order book stores customer orders on a price time priority basis. The highest bid and lowest offer are
considered "best" with all other orders stacked in price levels behind. In this test, the best order is considered to be
at level 1.*

- Given an Order, add it to the OrderBook (order additions are expected to occur extremely frequently)
- Given an order id, remove an Order from the OrderBook (order deletions are expected to occur at approximately 60% of
  the rate of order additions)
- Given an order id and a new size, modify an existing order in the book to use the new size (size modifications do not
  affect time priority)
- Given a side and a level (an integer value >0) return the price for that level (where level 1 represents the best
  price for a given side). For example, given side=B and level=2 return the second best bid price
- Given a side and a level return the total size available for that level
- Given a side return all the orders from that side of the book, in level- and time-order

## Design

### Order

- Use of double may not accurately represent every price that could be entered into the order book, a form of scaled
  decimal is recommended.
- An enum could better represent side to avoid checking for invalid characters.
- I have used a record class for sake of brevity.
- Practically, a long representation is big enough to represent total order book volume.
- Would benefit from IllegalArgument exceptions for id > 0, side validation, -ve size, -ve price

### OrderBook

I chose a sorted heap structure with a sequence representing each level. My initial consideration was a PriorityQueue
but that does not guarantee iteration ordering

Style wise I default to using final for method parameters, just a safety measure to force avoidance of reassignment side
effects within the implementation

As it stands the OrderBook would be intended to run from a single thread. Data that is exposed is not deep copied.

## Tests

I haven't yet added any concurrency testing.

A basic JMH benchmark has been created, which typically takes 25 mins to run.

## Security

Interesting plugin available for [Snyk](https://docs.snyk.io/integrate-with-snyk/use-snyk-in-your-ide/jetbrains-plugins)

# Part B

*Please suggest (but do not implement) modifications or additions to the Order and/or OrderBook classes to make them
better
suited to support real-life, latency-sensitive trading operations.*

Allow order sizes to be directly mutated, avoiding object creation / replacements. Threading becomes a consideration for
mutable objects. These would require memory barriers, with volatile or synchronized access. A hashmap would allows for
best case O(1) order updates.

Reduce object creation to attempt to achieve steady state memory.
Some different approaches:

- Object Pooling
- Collapse data structures into a new single collection class
- Use primitive arrays for storage
- Avoid all autoboxing using primitives
- Populate the entire order book for all expected levels, use flags to control whether orders are visible or not. No
  object deletion
- Flyweights around order messages to directly use the buffers allocated on I/O boundaries,
  e.g. [SBE](https://github.com/real-logic/simple-binary-encoding/wiki/Sbe-Tool-Guide)
- Examine [Agrona](https://github.com/real-logic/agrona) data structure applicability

On concurrency, basic synchronisation is used. Depending on access patterns more sophisticated lock free methods could
be used, requiring more development time and complexity.

# JMH Benchmark

Only a basic test run for adds, would need full public interface coverage.
Could set up performance assertions in CI build

| Benchmark                            | (orderCount) | Mode | Cnt | Score   | Error   | Units |
|--------------------------------------|--------------|------|-----|---------|---------|-------|
| OrderBookBenchmark.addOrderBenchmark | 100          | avgt | 25  | 1.846   | ± 0.077 | us/op |
| OrderBookBenchmark.addOrderBenchmark | 1000         | avgt | 25  | 32.089  | ± 1.853 | us/op |
| OrderBookBenchmark.addOrderBenchmark | 10000        | avgt | 25  | 548.494 | ± 7.422 | us/op |