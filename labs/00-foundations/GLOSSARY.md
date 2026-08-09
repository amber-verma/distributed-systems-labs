# Glossary

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

## Invariant

A property that must hold after every allowed transition, not merely at the end
of a successful run.

## Liveness

A property stating that useful progress eventually occurs under specified
conditions. Liveness always needs assumptions such as eventual message delivery
or a reachable majority.

## Local observation

Information directly available to one process: its memory, clock readings,
sent messages, received messages, and local failures. It does not include
unreceived facts about another process.

## Logical time

An ordering abstraction advanced by the simulator. It lets a test execute a
five-second timeout immediately while preserving its order relative to other
events.

## Nondeterminism

Multiple possible next events from the same apparent state. Real scheduling and
networks are nondeterministic. The simulator converts chosen possibilities into
an explicit, reproducible event order.

## Outcome

What actually happened to an operation, whether or not the caller knows it. A
server may have applied an operation even when the client's observed outcome is
a timeout.

## Physical or wall-clock time

A machine's measurement of elapsed or calendar time. Separate machines can
disagree, and elapsed-time tests can be affected by scheduling and load.

## Safety

A property stating that an invalid state or result never occurs. Safety must
hold even during failures covered by the protocol's model.

## Timeout

A local event saying that a deadline passed without a qualifying response. It
creates suspicion or triggers policy; it is not proof that a remote operation
did not execute.

## Trace

An ordered, immutable record of simulation facts. A trace is evidence for
debugging and replay, not the mutable state of the protocol itself.
