# Design: Deterministic Logical-Time Scheduler

## Goal

Provide the smallest reusable engine that gives tests explicit control over
event ordering and time. Networking and process lifecycle will be layered on
top rather than embedded in the scheduler.

## Public API

| Type | Responsibility |
| --- | --- |
| DeterministicScheduler | Own logical time and select the next event |
| Cancellable | Cancel or inspect one scheduled event |
| SimulationTrace | Store append-only immutable facts |
| TraceEvent | Represent one versioned fact |
| TraceKind | Classify trace facts |

## Main decisions

### Single owning thread

The scheduler is created and used by one thread. This prevents the operating
system from deciding queue interleavings behind the simulator's back.

Runtime code may later use virtual threads. Tests will inject their observable
effects into this deterministic event loop.

### Duration at the API, nanoseconds internally

Duration communicates units and prevents millisecond/second confusion. A
non-negative long nanosecond count makes ordering and exact overflow checks
straightforward.

This limits one simulation to roughly 292 years, which is more than sufficient
for protocol tests.

### Two-part ordering key

The priority queue orders by:

1. Due nanosecond.
2. Monotonic task ID.

This produces stable ordering for equal-time events without pretending that
real simultaneous events have a natural order.

### Lazy cancellation

Cancellation marks a queued task rather than searching the heap to remove it.
Polling discards cancelled entries.

- Cancellation is constant time.
- Polling may discard cancelled entries before finding the next pending task.
- Pending count tracks logical pending tasks, not physical heap entries.

### Fail fast on runaway scheduling

A task may schedule another zero-delay task. This is useful, but an accidental
infinite chain would otherwise hang the test process. Queue-drain and time
advance operations enforce a task budget.

### Append-only versioned trace

Trace events include a schema version, sequence, logical time, kind, subject,
and immutable string attributes. The trace is separate from scheduler state so
later components can share it.

String attributes keep the first schema simple. A later visualizer export can
add a serialization boundary without making core scheduling depend on JSON.

## State transitions

A task begins in Pending and can make exactly one terminal transition:

- Pending to Executed when selected.
- Pending to Cancelled through its handle.

An action failure still leaves the task Executed. The failure is recorded and
rethrown to the test; silently swallowing it would hide the causal event.

## Invariants

- nowNanos is non-negative and monotonic.
- Every task ID is unique within one scheduler.
- pendingTaskCount never becomes negative.
- pendingTaskCount equals tasks whose state is Pending.
- Only Pending tasks execute.
- A task produces at most one Started fact.
- A Started task produces exactly one Completed or Failed fact.
- Trace sequence is contiguous and monotonic.
- Trace snapshots do not change after publication.

## Complexity

| Operation | Cost |
| --- | --- |
| Schedule | O(log n) |
| Run next pending task | O(log n), plus discarded cancelled entries |
| Cancel | O(1) |
| Read logical time or pending count | O(1) |
| Snapshot trace | O(t) references for t events |

## Validation and errors

The API rejects:

- Negative durations.
- Blank labels.
- Time arithmetic overflow.
- Backward absolute-time movement.
- Use from a non-owner thread.
- Non-positive task budgets.

## Deliberate non-goals

Checkpoint 0A does not provide:

- Thread-safe concurrent scheduling.
- Real-time waiting.
- Distributed clock synchronization.
- Message transport.
- Probabilistic faults.
- Crash or stable-storage simulation.
- JSON trace export or an interactive viewer.

Each missing capability will be added only after its motivating failure is
introduced.
