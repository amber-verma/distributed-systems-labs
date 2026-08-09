# Deterministic Simulator

**Status:** Checkpoint 0A implemented

The simulator will execute protocol events using logical time and a controlled
event queue. Tests will choose message delivery, timeout, crash, restart, and
partition events without relying on wall-clock sleeps.

## Responsibilities

- Address logical nodes and endpoints.
- Schedule and cancel logical timers.
- Delay, lose, duplicate, and reorder messages.
- Crash and restart nodes while preserving configured stable state.
- Generate seeded fault schedules.
- Record versioned, replayable event traces.
- Run invariant checks after every event.

## Implemented in checkpoint 0A

- Non-negative, monotonic logical time.
- Stable ordering by due time and insertion ID.
- Explicit time advance and queue drain.
- Idempotent cancellation.
- Single-owner-thread enforcement.
- Runaway zero-delay scheduling protection.
- Immutable, versioned trace facts.
- Java 25 tests, including a virtual-thread ownership boundary.

## Next checkpoint

Build simulated request and reply delivery on this scheduler, then introduce
delay, loss, duplication, reordering, partition, crash, and restart one failure
at a time.

## Non-goals

The simulator will not emulate CPU timing, operating-system scheduling, or
Byzantine nodes. Real virtual-thread and socket behavior belongs in dedicated
runtime tests.
