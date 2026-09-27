# Primary Reading List

Read a relevant section after deriving the motivating problem in a lab. There
is no requirement to read this entire list before coding. Use the paper to
challenge the assumptions and choices in your own implementation.

| When | Question to take into the reading |
| --- | --- |
| Foundations causality exercise | What order can messages establish without a shared physical clock? Start with Lamport's partial-order discussion. |
| Simulated RPC and retry-safe KV | What does a reply establish, and where must duplicate-effect protection live? Read RPC and end-to-end arguments. |
| MapReduce retries | How do repeated attempts still produce one accepted result? |
| Raft elections and commitment | What evidence makes an entry safe, and what must survive a crash? Read FLP here to examine progress assumptions, not as a prerequisite to 0A. |
| KV over Raft | Which concurrent histories admit the claimed linearization points? |
| After replicated KV | How does Dynamo trade consistency for availability, and how are conflicting versions reconciled? |
| After sharding | How do Spanner's replication, time assumptions, and transaction coordination fit together? |

The MIT course is a broader source of systems case studies; follow this
repository's [learning order](LEARNING_PLAN.md#sequence-and-dependencies).

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
