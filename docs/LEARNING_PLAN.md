# Learning Plan

## Outcome and method

Develop a working mental model of distributed systems and demonstrate it with
Java implementations that survive reproducible failures. Build toward the
design and debugging judgement expected of a principal engineer through
implementation, comparisons, and explicit operational tradeoffs.

Use the [root progress table](../README.md#current-progress-and-learning-order)
for implementation status. This page owns the curriculum, not a second progress
ledger. Start with the [Foundations chapter](../labs/00-foundations/README.md).

For each checkpoint:

1. Predict the simplest solution's behaviour and a concrete failure trace.
2. Derive the repair: assumptions, mechanism, invariant, and counterexample.
3. Implement or modify the smallest behaviour and test the invariant.
4. Explain the result and its limits in your own words; revisit it without notes.

Diagrams sit beside the explanations they support. Use text traces for scheduler
exercises and the offline viewer for 0B request/reply scenarios. Neither a new
viewer feature nor interview presentation is a prerequisite for understanding
the next mechanism. Learning quality comes from the reasoning and exercises.

Complex terms need a motivating problem, minimal example, intuitive and precise
definitions, failure timeline, misconception, invariant, and connection to code.
Analogies introduce an idea; the protocol's exact behaviour establishes it.
Handwritten, pair-programmed, and AI-assisted code all require the same
explanation gate.

## Sequence and dependencies

Folder numbers follow the learning order:

**Foundations → simulated request/reply → trace viewer v1 → retry-safe KV →
MapReduce → Raft → KV over Raft → sharded KV → real TCP capstone.**

KV retries and MapReduce are independent. Raft supplies the replicated log for
KV over Raft; sharding depends on that replicated service. Grow the shared
platform only when the next failure exercise needs it.

## Phase 0: foundations and platform

- **0A:** logical time, stable event order, cancellation, failure recording,
  owner-thread boundaries, and immutable trace facts.
- **0B:** one client and one Append server; immutable messages, explicit delivery
  delays and drops, correlated replies, and one terminal client outcome.
  Demonstrate lost requests, lost replies, late/duplicate replies, and both
  equal-time timeout/reply orders.
- **Viewer v1, immediately after 0B:** import the same tested histories and step
  through messages, timers, event details, and recorded state. Rewinding shows
  only the selected history prefix.
- Add worker crashes and stale work when MapReduce requires them. Introduce
  partitions and persistent state before Raft recovery exercises. Add seeded
  fault schedules and their recorded decisions when randomized scenarios begin.
- Keep protocol state transitions on the deterministic loop. Runtime adapters
  translate real-thread or socket activity into the same explicit behaviours.

The observer's simulated clock is not a clock shared by the nodes. Add the
small happened-before/Lamport-clock exercise in Foundations before relying on
event order as distributed knowledge.

## Retry-safe KV (folder 01, next)

Begin with an in-memory Get, Put, and Append service. Lose requests and replies,
observe naive retries applying Append twice, then derive logical client identity,
request sequence, cached replies, and at-most-once processing.

Assume one outstanding operation per logical client. Initially the server stays
alive and retains its deduplication table. State that boundary explicitly:
crash-safe at-most-once behaviour requires data and deduplication state to survive
together. Do not infer durability from a network-retry test.

Completion means retries preserve the specified effects under the tested faults,
the failure without deduplication is demonstrable, and bounded-history choices
can be explained. This is the next implementation lab after reviewing 0B.

## MapReduce (folder 02)

Build a sequential executor, coordinator, virtual-thread workers, and simulated
RPC. Introduce crashes, lease expiry, retry, concurrent attempts, and atomic
output publication.

The key distinction is between duplicate execution and single-result
commitment.

## Raft

Implement:

1. Follower, candidate, leader, voting, and election timers.
2. AppendEntries, log repair, replication, commitment, and ordered application.
3. Persistent term, vote, and log with crash recovery.
4. Snapshots, compaction, and InstallSnapshot.

Dynamic membership, pre-vote, ReadIndex, and leadership transfer are documented
extensions rather than core requirements.

## KV over Raft

Route Get, Put, and Append through the Raft log. Replicate deduplication state,
correlate pending requests with committed commands, recover with snapshots, and
check generated histories for linearizability.

## Sharded KV

Use sixteen deterministic shards, multiple Raft groups, and a Raft-backed shard
controller. Process configurations sequentially and transfer shard contents
with their deduplication state.

The old group must stop serving before the new group starts. Temporary
unavailability is acceptable; simultaneous writable ownership is not.


## Broader reasoning and capstone

These are later learning additions, not extra setup prerequisites or new
documentation folders today.

| Placement | Exercise and completion evidence |
| --- | --- |
| Before/during Raft | Derive majority intersection, distinguish election from agreement, and state the timing/fair-delivery assumptions needed for progress. Use an adversarial schedule to motivate the limits of asynchronous consensus. |
| After KV over Raft | Compare linearizability, causal ordering, and eventual consistency using small read/write histories. Build a two-replica conflict/reconciliation exercise and discuss Dynamo, quorum rules, and background repair. |
| After sharding | Implement a small two-participant commit exercise. Crash the coordinator after one participant prepares and explain blocking, recovery, and atomicity versus isolation. Read Spanner as a design comparison. |
| Final capstone | Use real processes and TCP, framing, deadlines, reconnection, and graceful shutdown. Measure latency/throughput under increasing load, bound queues and retries, inject slow peers, and diagnose a failure from observations. |

Multi-key transactions stay outside the core sharded-KV guarantee; the later
transaction exercise has its own explicit assumptions. The
[reading list](READING_LIST.md) pairs these additions with primary sources.

## Completion and revision

For each checkpoint, record implementation/test status separately from personal
exercise completion. A passing test suite does not establish that the learner
can derive or explain the mechanism.

A checkpoint is learned when you can:

- Predict a named failure trace, including what each node can know.
- State safety, progress assumptions, and deliberate non-guarantees.
- Map the invariant to state transitions and tests.
- Change one assumption and explain or demonstrate how the design must change.
- Repeat the explanation later using only the chapter's short revision section.

For each completed lab, its documented guarantees must match code; named failure
scenarios must pass; important invariants must be checked; and visual examples
must come from executable scenarios. Once randomized testing is introduced,
record the initial state, seed, and fault choices needed to reproduce it.
An exported history supports inspection; it does not serialize arbitrary Java
actions into an executable replay program.

## Design review and optional interview practice

### Explanation exercise

For every mechanism, answer:

1. What concrete failure motivates it?
2. What is the smallest example that demonstrates the failure?
3. What invariant does the mechanism preserve?
4. What assumptions are required for safety?
5. What additional assumptions are required for progress?
6. What state must survive a crash?
7. What happens to an old or delayed message?
8. How would an operator detect that the mechanism is unhealthy?

### Design review

Each lab ends with a short design review covering:

- API and protocol boundary.
- State ownership and persistence.
- Normal request path.
- Worst failure trace.
- Backpressure and resource bounds.
- Observability and replay.
- Compatibility and rollout concerns.
- Deliberate simplifications.
- The first changes required for production use.

### Debugging exercise

Given an event trace:

- Reconstruct the state known by each node at each step.
- Separate facts from assumptions based on timeouts.
- Identify the earliest invariant violation.
- Reduce the failure to the shortest reproducing trace.
- Propose a test that fails before the fix and passes afterward.

### Comparison exercise

For each completed lab, compare at least two alternatives. Examples include:

- Lease reassignment versus fixed ownership.
- Idempotent APIs versus general deduplication.
- Leader-based replication versus quorum reads and writes.
- Logging reads versus ReadIndex-style optimization.
- Stop-and-copy shard transfer versus more available migration protocols.

The expected answer includes workload, failure, operational, and complexity
tradeoffs rather than declaring one design universally better.
