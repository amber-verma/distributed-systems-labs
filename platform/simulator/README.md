# Deterministic Simulator

**Status:** Planned

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

## Non-goals

The simulator will not emulate CPU timing, operating-system scheduling, or
Byzantine nodes. Real virtual-thread and socket behavior belongs in dedicated
runtime tests.
