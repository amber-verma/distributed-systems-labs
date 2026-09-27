# Deterministic Simulator

**Implemented:** 0A logical-time scheduler and immutable trace model; 0B scripted
request/reply network and teaching scenarios; JSON export for the offline viewer.

Start with the [Foundations chapter](../../labs/00-foundations/README.md) for the
derivation and exercises. This page owns reusable API contracts and design
decisions.

## Run

From the repository root:

~~~powershell
.\gradlew.bat :platform:simulator:test
.\gradlew.bat :platform:simulator:exportTraces
~~~

Use ./gradlew on macOS/Linux. Generated JSON goes to
platform/simulator/build/traces/ and can be opened with the
[trace viewer](../trace-visualizer/README.md).

To inspect all request/reply tests in the console, run the test class (or use
its Run gutter icon in the IDE):

~~~powershell
.\gradlew.bat :platform:simulator:test --tests "io.github.amberverma.distsys.simulator.RequestReplyScenarioTest" --rerun-tasks
~~~

Each test prints what it shows, its use case, the relevant ordered trace or
traces, client and server outcomes, and the observation being checked. The
parameterized test prints all seven histories and whether each repeat run
matches. `--rerun-tasks` makes Gradle show the output even when the test is up
to date.

## Public API

All source types below are in io.github.amberverma.distsys.simulator.

| Type | Contract |
| --- | --- |
| DeterministicScheduler | Own time, select due events, enforce thread ownership and execution budgets |
| Cancellable | Cancel a pending event once; inspect whether it is terminal |
| SimulationTrace | Append immutable facts, snapshot them, format human-readable text |
| TraceEvent / TraceKind | Represent and classify versioned facts |
| [Message](src/main/java/io/github/amberverma/distsys/simulator/Message.java) | Immutable request ID, source, destination, and string payload; reply echoes request ID |
| [SimulatedNetwork](src/main/java/io/github/amberverma/distsys/simulator/SimulatedNetwork.java) | Register unique endpoint handlers; schedule each transmission with a delay and DELIVER or DROP decision |
| [RequestReplyScenario](src/main/java/io/github/amberverma/distsys/simulator/RequestReplyScenario.java) | Run a named one-shot Append scenario and return immutable client outcome, server value, completion count, and trace |
| TraceJson | Export facts in deterministic JSON without runtime dependencies |
| TraceExamples | Generate scheduler and request/reply examples from executable code |

The scheduler, network, and endpoint handlers run on one owner thread.
SimulationTrace is not independently thread-safe; components share it through
that event loop. Snapshots can be inspected without mutating their facts.

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

String attributes keep the first schema simple. TraceJson supplies the serialization boundary without making core scheduling
depend on JSON.

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
- Successfully recorded trace sequence is contiguous and monotonic; rejected
  records do not consume a sequence number.
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

## Request and reply contracts

Endpoints must exist before sending, and their names must be unique and
non-blank. Misconfiguration is rejected; only an explicit DROP models packet
loss. Delivery executes the receiver after the specified delay. Dropping records
loss at the scheduled delivery time without invoking the receiver. Each send
gets a new messageId; repeated sends may share the same logical requestId.

Different delays can reorder messages. Sending the same immutable envelope
twice models duplicate transmissions. The network supplies no automatic retry,
deduplication, delivery acknowledgement, or background work. Its handler failures
propagate through the scheduler's Failed fact and exception path.

The teaching client permits Pending → Succeeded or Pending → Timed out once.
A matching reply cancels its deadline; late or repeated replies are ignored.
The server appends x when a request reaches it. That server effect and the
client's terminal observation are separate state. Timeout does not cancel work
at the server and does not imply that the Append did not happen.

## Trace and export contract

JSON uses a top-level schemaVersion of 1 and an events array in sequence order.
Each fact has schemaVersion, sequence, logicalTimeNanos, kind, subject, and
attributes. Logical time is a decimal string of nanoseconds, preserving values
beyond JavaScript's exact-number range. Attribute keys are sorted for stable
export; their values are strings. JSON escapes quotes, controls, backslashes,
and UTF-16 surrogate code units.

Message facts include messageId, requestId, source, destination, and payload.
Application STATE_CHANGED facts include node and value. The viewer reconstructs
only recorded observations; missing observations remain unknown. It derives
pending events from TASK_SCHEDULED, TASK_STARTED, and TASK_CANCELLED.

Text output renders durations in seconds. The in-memory scheduler still stores
nanoseconds. Schema versioning describes the data, not executable replay:
Runnable actions are not serialized. Exact reruns currently use the named
scenario code and scripted choices. Seeded failure generation and executable
fault-schedule replay remain future work.

## Mapping concepts to Java

| Idea | Java representation |
| --- | --- |
| Logical clock | Duration backed by a non-negative nanosecond count |
| Stable event order | PriorityQueue ordered by due time and task ID |
| Event lifecycle | Private enum: pending, cancelled, executed |
| Cancellation authority | Cancellable handle |
| Immutable fact | TraceEvent record |
| Append-only history | SimulationTrace |
| Single event-loop owner | Captured Thread identity |
| Runaway protection | Maximum task count per drain or time advance |

Records model immutable trace facts. A virtual-thread test demonstrates that
modern concurrency is being used deliberately rather than everywhere by
default.

## Current tests and boundaries

DeterministicSchedulerTest covers ordering, advancing time, cancellation,
follow-up scheduling, action failure, backward-time rejection, runaway work,
and virtual-thread rejection. SimulationTraceTest covers immutable snapshots,
stable formatting, and rejected records preserving sequence continuity.

RequestReplyScenarioTest covers all seven RPC histories, checks one completion,
and compares complete repeat runs. SimulatedNetworkTest demonstrates scripted
loss, reordered delivery, repeated transmission, and invalid endpoint rejection.
TraceJsonTest checks deterministic export and escaping. Viewer tests parse the
actual exported Java histories and check rewind behaviour and outcomes.

The platform does not yet provide crash/restart, persistent storage, partition
management, random fault schedules, or a general post-event invariant-checker
registration API. Add these when the corresponding lab needs them. It also
does not emulate CPU timing, synchronize clocks, run real sockets, or model
Byzantine nodes. Virtual-thread/socket runtime tests belong in the later adapter.
