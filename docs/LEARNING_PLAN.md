# Learning Plan

## Outcome

Develop a working mental model for distributed systems and demonstrate it with
Java implementations that survive reproducible failure scenarios.

The target depth is appropriate for a senior or principal engineer preparing
for distributed-systems design, implementation, and debugging interviews.

## Course method

Each implementation checkpoint has eight gates:

1. Read the first-principles explanation.
2. Walk through the static visual.
3. Replay an interactive failure scenario.
4. Explain the mechanism and its limits in your own words.
5. Derive or review pseudocode.
6. Implement the smallest new behavior.
7. Run deterministic tests and inspect the resulting trace.
8. Complete a design and interview review.

A later lab starts only after the current lab satisfies its acceptance criteria.
Implementation may be handwritten, pair-programmed, or AI-assisted, but the
reasoning gate remains mandatory.

## Explanation contract

Every complex term must include:

- The original problem in plain language.
- A minimal example.
- A failure timeline.
- An intuitive definition.
- A precise definition.
- A counterexample or misconception.
- The invariant the code must preserve.
- The Java types and tests that represent it.

Analogies may introduce an idea but never replace the exact protocol behavior.

## Phase 0: foundations and platform

**Current status:** Checkpoint 0A complete.

Start with two Java objects making a local method call. Progressively remove the
assumptions that the callee is reachable, replies arrive, processes stay alive,
messages retain order, and clocks agree.

Build a deterministic simulator with logical time, seeded faults, event traces,
and replay. Add a trace visualizer for messages, timers, crashes, state changes,
and commits. Add real TCP only after simulated RPC behavior is understood.

## Lab 1: MapReduce

Build a sequential executor, coordinator, virtual-thread workers, and simulated
RPC. Introduce crashes, lease expiry, retry, concurrent attempts, and atomic
output publication.

The key distinction is between duplicate execution and single-result
commitment.

## Lab 2: fault-tolerant KV

Implement Get, Put, and Append. Lose requests and replies, observe retry
ambiguity, then add client identities, request sequences, cached replies, and
at-most-once processing.

The core lab assumes one outstanding operation per logical client.

## Lab 3: Raft

Implement:

1. Follower, candidate, leader, voting, and election timers.
2. AppendEntries, log repair, replication, commitment, and ordered application.
3. Persistent term, vote, and log with crash recovery.
4. Snapshots, compaction, and InstallSnapshot.

Dynamic membership, pre-vote, ReadIndex, and leadership transfer are documented
extensions rather than core requirements.

## Lab 4: KV over Raft

Route Get, Put, and Append through the Raft log. Replicate deduplication state,
correlate pending requests with committed commands, recover with snapshots, and
check generated histories for linearizability.

## Lab 5: sharded KV

Use sixteen deterministic shards, multiple Raft groups, and a Raft-backed shard
controller. Process configurations sequentially and transfer shard contents
with their deduplication state.

The old group must stop serving before the new group starts. Temporary
unavailability is acceptable; simultaneous writable ownership is not.

## Definition of completion

Each lab is complete when:

- Its documented guarantees and non-guarantees match the implementation.
- Named failure scenarios pass deterministically.
- Randomized scenarios are reproducible by seed and trace.
- Important invariants are checked automatically.
- Visual traces correspond to executable scenarios.
- The design review identifies production gaps and alternative approaches.
- The implementation can be explained without relying on framework behavior.
