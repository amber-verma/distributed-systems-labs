# Primary Reading List

Readings are paired with implementation checkpoints. The goal is to interrogate
the design choices in each paper rather than memorize its terminology.

## Foundations

- Leslie Lamport, [Time, Clocks, and the Ordering of Events in a Distributed System](https://lamport.azurewebsites.net/pubs/time-clocks.pdf)
- Michael Fischer, Nancy Lynch, and Michael Paterson, [Impossibility of Distributed Consensus with One Faulty Process](https://groups.csail.mit.edu/tds/papers/Lynch/jacm85.pdf)
- MIT, [6.5840 Distributed Systems](https://pdos.csail.mit.edu/6.5840/)

## MapReduce

- Jeffrey Dean and Sanjay Ghemawat, [MapReduce: Simplified Data Processing on Large Clusters](https://research.google/pubs/mapreduce-simplified-data-processing-on-large-clusters/)

## Remote calls and retry semantics

- Andrew Birrell and Bruce Nelson, [Implementing Remote Procedure Calls](https://www.microsoft.com/en-us/research/publication/implementing-remote-procedure-calls/)
- Jerome Saltzer, David Reed, and David Clark, [End-to-End Arguments in System Design](https://web.mit.edu/Saltzer/www/publications/endtoend/endtoend.pdf)

## Raft

- Diego Ongaro and John Ousterhout, [In Search of an Understandable Consensus Algorithm](https://raft.github.io/raft.pdf)
- [Raft resources, dissertation, and formal specification](https://raft.github.io/)

## Linearizable replicated services

- Maurice Herlihy and Jeannette Wing, [Linearizability: A Correctness Condition for Concurrent Objects](https://www.cs.cmu.edu/~wing/publications/HerlihyWing90.pdf)

## Sharding and design contrasts

- James Corbett et al., [Spanner: Google's Globally-Distributed Database](https://research.google/pubs/spanner-googles-globally-distributed-database-2/)
- Giuseppe DeCandia et al., [Dynamo: Amazon's Highly Available Key-value Store](https://www.amazon.science/publications/dynamo-amazons-highly-available-key-value-store)
