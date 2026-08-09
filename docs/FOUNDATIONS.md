# Foundations

## Begin with an ordinary call

In one process, a method call has unusually strong implicit guarantees. The
caller and callee share memory, execution has one visible call stack, and a
returned value proves that the method reached its return statement.

Moving the callee across a network removes those guarantees. A timeout cannot
tell the caller whether:

- The request never arrived.
- The request arrived but the server crashed before acting.
- The server acted but crashed before replying.
- The server replied but the network lost the reply.
- The request or reply is merely delayed.

This ambiguity is the starting point for the course.

## Failure model

The core implementation assumes:

- Processes may crash and later restart.
- Volatile memory is lost on restart.
- Explicit stable storage survives a restart.
- Messages may be delayed, lost, duplicated, or reordered.
- A partition may isolate otherwise healthy nodes.
- Nodes are non-Byzantine: they may fail, but do not lie or forge protocol data.
- Clocks need not agree and timeout expiration is not proof of remote failure.

When the simulated network becomes healthy and nodes remain alive, messages that
are retried may eventually be delivered. Individual protocols must state any
additional liveness assumptions.

## Safety and liveness

Safety means that something invalid never happens. Examples include two
different commands becoming committed at the same Raft log index or two shard
groups accepting writes for the same configuration.

Liveness means that useful progress eventually happens under stated conditions.
Examples include electing a leader after the network stabilizes or completing a
request when a majority is reachable.

A system can preserve safety while temporarily losing liveness. Distributed
protocols commonly choose this behavior during uncertainty.

## Time

Wall-clock timestamps describe a machine's view of physical time. They are not a
safe foundation for ordering distributed events unless the protocol explicitly
accounts for clock uncertainty.

The simulator therefore uses logical time. Timers create future events, and the
simulator decides which event occurs next. Tests advance time intentionally
rather than sleeping and hoping operating-system scheduling produces a desired
interleaving.

## Invariants

An invariant is a property that must hold after every valid state transition.
It is stronger and more useful than checking only a final result.

For example, a MapReduce coordinator may allow two attempts to run for one task,
but it must never accept output from two attempts. Tests should check that rule
after every coordinator event, not only after the job finishes.

## Determinism and replay

Real concurrency is valuable for runtime learning but poor at reproducing rare
failures. Protocol state transitions will therefore be testable through a
seeded event simulator. Every failure run records enough information to replay
the same message order, timeout, crash, and restart.
