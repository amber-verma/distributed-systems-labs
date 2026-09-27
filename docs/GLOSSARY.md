# Glossary

Use this as a lookup reference after the motivating failure appears in a lab.
The [Foundations chapter](../labs/00-foundations/README.md) supplies the first
examples; definitions are collected here so they have one reference home.

## At-most-once processing

A server-side guarantee that a logical request is applied no more than once.
It does not guarantee that the request is applied at all.

In code, a client identity and request sequence let the server recognize a
retry and return the cached result instead of applying the operation again.

## Commit

The point at which a protocol has made a result authoritative under its stated
rules. Commitment is protocol-specific. It does not necessarily mean that every
replica has applied or persisted the result.

## Consensus

Agreement among participants on a single value or ordered decision despite
specified failures. Leader election supports consensus but is not, by itself,
the whole consensus problem.

## Deduplication

Recognizing multiple deliveries of the same logical request and suppressing
repeated effects. Deduplication requires identity and retained history; a retry
alone carries neither.

## Deterministic simulation

A simulation in which the same initial state, inputs, and event choices produce
the same ordered history. Determinism makes a failure replayable; it does not
claim that a real deployment has deterministic timing.

## Event

One discrete state transition selected by the simulator, such as a timer
firing. Later checkpoints add message delivery, crash, restart, and partition
events.

## Failure detector

A mechanism that produces suspicions about process failure, usually from missed
messages or timeouts. In an asynchronous system, a timeout cannot perfectly
distinguish a failed process from a slow or partitioned process.

## Happened-before

A causal ordering: events in one process follow its execution order, a send precedes its receive, and these relations are transitive. Events unrelated by this ordering are concurrent; their order in the simulator does not establish causation.

## Idempotent operation

An operation whose repeated application has the same intended effect as one
application. Put of the same value is commonly idempotent; Append generally is
not.

Idempotency is a property of an operation. Deduplication is a protocol
mechanism.

## Invariant

A property that remains true after every allowed state transition. Invariants
are the bridge between a protocol's guarantee and its implementation tests.

## Lamport clock

A per-process counter updated for local events and received timestamps. If A happened before B, A's timestamp is smaller. The converse is false: smaller timestamps alone do not prove causation.

## Lease

Time-bounded authority granted by one component to another. Expiration allows
work to be reassigned, but an expired worker may still be running. A lease
therefore requires fencing or attempt identity when stale work could be harmful.

## Linearizability

A correctness condition in which each completed operation appears to take
effect at one instant between its invocation and response, while respecting
real-time ordering between non-overlapping operations.

## Liveness

A property stating that useful progress eventually occurs under specified
conditions. Liveness always needs assumptions such as eventual message delivery
or a reachable majority.

## Local observation

Information directly available to one process: its memory, clock readings,
sent messages, received messages, and local failures. It does not include
unreceived facts about another process.

## Logical time

In this repository, the simulator's controlled clock for scheduling events and advancing deadlines without waiting. Nodes do not automatically share this clock. Lamport clocks solve a different problem: representing causal constraints using local counters and messages.

## Nondeterminism

Multiple possible next events from the same apparent state. Real scheduling and
networks are nondeterministic. The simulator converts chosen possibilities into
an explicit, reproducible event order.

## Outcome

What actually happened to an operation, whether or not the caller knows it. A
server may have applied an operation even when the client's observed outcome is
a timeout.

## Partition

A communication failure that divides nodes into groups that cannot exchange
messages even though the nodes themselves may remain healthy.

## Physical or wall-clock time

A machine's view of calendar time; machines can disagree and clock corrections can move readings. A monotonic elapsed-time clock is useful for local deadlines, but its expiration still does not prove remote failure.

## Quorum

A participant set chosen so relevant decision sets intersect under the protocol's rules. Raft uses majorities. Intersection supplies shared evidence; voting, persistence, and log rules are also needed to make that evidence preserve agreement.

## Replication

Maintaining copies of data or state on multiple nodes for availability or
durability. Replication does not automatically make the copies consistent.

## Safety

A property stating that an invalid state or result never occurs. Safety must
hold even during failures covered by the protocol's model.

## Sharding

Partitioning a dataset so different subsets are owned by different groups.
Replication creates copies; sharding divides ownership.

## Timeout

A local event saying that a deadline passed without a qualifying response. It
creates suspicion or triggers policy; it is not proof that a remote operation
did not execute.

## Trace

An ordered, immutable record of simulation facts. A trace is evidence for
debugging and replay, not the mutable state of the protocol itself.
