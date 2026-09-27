# Foundations: local calls, uncertain outcomes, and controlled time

**Implementation:** checkpoints 0A and 0B are implemented and tested.
The offline trace viewer v1 is available. Personal mastery is recorded in the
[learning checklist](#learning-checklist), separately from code status.

**First study:** [derive the concepts](#first-principles) →
[run the scenarios](#run-and-inspect) → [do the exercises](#exercises) →
[explain the design](#self-check-and-design-review).

**Returning for revision:** read the next section, predict one failure trace,
and answer its self-check before opening code.

## First principles

### 1. Begin with the smallest possible system

Consider one Java object calling another:

    var result = inventory.reserve(itemId);

Both objects are inside one JVM. This gives us useful guarantees that are easy
to overlook:

- The call enters the method or throws before it does.
- If it returns, the caller knows the method reached a return point.
- The caller receives either a value or an exception through the same call
  stack.
- Caller and callee share one process lifetime.
- The language runtime defines memory visibility and exception propagation.

This does not make local code infallible. The process can crash, code can hang,
and a method can partially mutate state before throwing. The important point is
that the caller and callee share one failure boundary and one direct control
flow.

In a local call, control and the result travel through one language runtime.

~~~mermaid
sequenceDiagram
    participant C as Caller object
    participant S as Callee object
    C->>S: reserve(itemId)
    activate S
    S-->>C: Reservation
    deactivate S
    Note over C,S: One JVM, one direct call stack
~~~

The return is positive evidence that the method produced that result.

### 2. Move the callee across a network

Now the same source-level intention requires at least two messages:

1. The client sends a request.
2. The server sends a reply.

Between those messages are independent processes, operating systems, queues,
network links, and clocks. The client can no longer observe the server's call
stack or memory.

Suppose the client waits for one second and then times out. At least four worlds
are compatible with that observation:

1. The request never reached the server.
2. The request arrived, but the server crashed before applying it.
3. The server applied it, but crashed before sending the reply.
4. The server applied it and sent a reply, but the reply was lost or delayed.

The client's local evidence is identical in all four worlds: no reply arrived
before its timer expired.

**Compare the two histories:**

~~~mermaid
sequenceDiagram
    participant C as Client
    participant N as Network
    participant S as Server
    C->>N: Reserve request
    Note over N: Request is lost
    C->>C: Local deadline expires
    Note over C: Observed outcome: timeout
~~~

The server did not apply the operation.

~~~mermaid
sequenceDiagram
    participant C as Client
    participant N as Network
    participant S as Server
    C->>N: Reserve request
    N->>S: Reserve request
    S->>S: Apply reservation
    S-->>N: Success reply
    Note over N: Reply is lost
    C->>C: Local deadline expires
    Note over C: Observed outcome: timeout
~~~

The server did apply the operation.

The client's visible history is the same in both diagrams:

| Observation | Request-lost world | Reply-lost world |
| --- | --- | --- |
| Request sent | Yes | Yes |
| Reply received | No | No |
| Deadline expired | Yes | Yes |
| Harness-only fact: server applied operation | No | Yes |

Checkpoint 0B below reproduces this ambiguity.

### 3. The key first-principles result

If two different external realities produce the same local observations, no
client-only algorithm can determine which reality occurred.

This is not a limitation of Java, TCP, gRPC, or a particular timeout value. The
missing information never arrived.

A timeout therefore means:

> The caller does not currently know the outcome.

It does not mean:

> The operation did not execute.

This distinction is the reason an automatic retry can be dangerous. Retrying a
read may be harmless. Retrying an Append or payment instruction may repeat a
business effect.

### 4. Knowledge is local

Distributed protocols should be read as rules about knowledge.

- A node knows only its local state and received messages.
- A timeout adds the fact that no qualifying message arrived before a local
  deadline.
- Silence does not distinguish a crashed node from a slow or partitioned node.
- A message proves that its sender produced that message at some earlier point;
  it does not prove the sender is still alive now.

Later mechanisms create stronger evidence:

- Request IDs let a server recognize the same logical request.
- Quorums make relevant decision sets intersect; voting and persistence rules determine how that shared evidence is used.
- Terms and log indexes identify the context of a Raft statement.
- Fencing tokens let a receiver reject stale authority.

None of these remove uncertainty universally. They make particular decisions
safe under explicitly stated assumptions.

### 5. Safety and liveness

Safety asks:

> Can the system ever do something invalid?

Examples:

- Apply one logical payment twice.
- Commit two different Raft commands at one log index.
- Allow two groups to accept writes for the same shard configuration.

Liveness asks:

> Under what conditions does the system eventually make progress?

Examples:

- A request eventually completes after the network heals.
- A reachable majority eventually elects a leader.
- A shard migration eventually finishes.

During a partition, a protocol may preserve safety by refusing work. That loses
availability temporarily, but it does not violate safety. Always state safety and liveness separately.

### 6. Why wall-clock tests are inadequate

A common concurrency test sleeps for a duration and hopes background threads
reach a desired state. Such a test depends on:

- Operating-system scheduling.
- Machine load.
- Timer precision.
- JVM warm-up.
- Thread interleaving.

When the failure occurs once in ten thousand runs, the passing runs provide
little diagnostic evidence and the failing run may be impossible to recreate.

Distributed-systems tests need control over event order, not merely faster
sleep calls.

### 7. Logical time

Logical time in this simulator is a controlled ordering mechanism:

- Time begins at zero.
- Scheduling adds an event with a due time.
- Nothing happens in the background.
- Running an event advances time directly to its due time.
- Advancing to a target executes every event due by that target.

If a five-second timeout is scheduled, a test does not wait five real seconds.
It moves the simulation clock to five seconds and executes the timeout event.

Logical time here is not an attempt to synchronize clocks across nodes. It is a
testing abstraction that makes the order of simulated events explicit and
replayable.

### 8. Why equal-time insertion order matters

Two events can have the same due time. A priority queue ordered only by time is
free to return them in an unspecified order.

That would make two runs of the same scenario disagree. The scheduler therefore
uses a second key: a monotonically increasing task ID assigned at insertion.

The ordering is:

1. Smaller due time first.
2. For equal due times, smaller task ID first.

This is not a claim about how a real network orders simultaneous events. It is a
deterministic tie-breaker for the simulator.

Suppose three events are added at logical time zero:

| Task | Delay | Due time | Insertion ID |
| --- | ---: | ---: | ---: |
| late | 10 ms | 10 ms | 0 |
| first-at-five | 5 ms | 5 ms | 1 |
| second-at-five | 5 ms | 5 ms | 2 |

The queue order is derived, not timed experimentally:

~~~mermaid
flowchart LR
    Z["t = 0 ms"] --> A["t = 5 ms<br/>first-at-five, id 1"]
    A --> B["t = 5 ms<br/>second-at-five, id 2"]
    B --> C["t = 10 ms<br/>late, id 0"]
~~~

The insertion ID matters only when due times are equal.

### 9. Why the scheduler has one owning thread

The simulator controls interleaving explicitly. If multiple real threads could
mutate its event queue concurrently, operating-system scheduling would
reintroduce hidden nondeterminism.

The scheduler records its creating thread and rejects calls from every other
thread, including virtual threads. Later runtime adapters may use virtual
threads, but protocol correctness tests remain under one deterministic event
loop.

### Course failure model and current boundary

The course's core failure model is:

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

Checkpoint 0B currently models scripted delay, loss, and repeated transmissions.
Different delays can reorder delivery. It does not yet implement process
crashes, restart, partitions, stable storage, or random fault schedules.
The server has one in-memory string and no retry or deduplication mechanism.

### What the implementation adds

The scheduler controls when an event happens. The network gives the event
meaning: a message moves from one named endpoint to another, or is lost.
An immutable message carries request identity, source, destination, and payload.

The client starts Pending and arms a deadline. A matching reply changes it to
Succeeded and cancels the deadline. The deadline changes it to Timed out if it
is still Pending. After either transition, further replies are recorded and
ignored. Request identity correlates replies; it does not itself prevent a
server from executing the same request twice.

~~~mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    C->>S: Append x (request-1)
    S->>S: value becomes x
    Note over C: Deadline expires: Timed out
    S-->>C: Late reply for request-1
    C->>C: Ignore late reply, remain Timed out
~~~

The minimal repair here prevents double completion. We deliberately leave
server-side duplicate effects for the retry-safe KV exercise, where the failure
will motivate deduplication.

## Run and inspect

Prerequisites: JDK 25 and the committed wrapper. Run from the repository root:

~~~powershell
.\gradlew.bat --version
.\gradlew.bat :platform:simulator:test
.\gradlew.bat :platform:simulator:exportTraces
~~~

On macOS/Linux replace .\gradlew.bat with ./gradlew. No global Gradle installation
is needed. The existing scheduler tests print explanatory text traces.

Open the [offline viewer](../../platform/trace-visualizer/index.html), then select
one of the files in platform/simulator/build/traces/. Start before the first
event, predict the next transition, and step forward. Use Previous or the
slider to inspect the earlier recorded state.

| File | Predicted client result | Actual final server value | What it demonstrates |
| --- | --- | --- | --- |
| scheduler-order.json | No RPC client | No server | C runs before A; B is cancelled |
| success.json | Succeeded | x | A reply arrives and cancels the deadline |
| lost-request.json | Timed out | Empty | No server execution |
| lost-reply.json | Timed out | x | Execution without client confirmation |
| late-reply.json | Timed out | x | A late reply cannot complete the client again |
| duplicate-reply.json | Succeeded | x | Two reply deliveries produce one completion |
| reply-first.json | Succeeded | x | Reply wins an equal-time race |
| timeout-first.json | Timed out | x | Timeout wins the same equal-time race |

For both tie scenarios, the request is delivered at time zero and reply/deadline
are due at 10 ms. The reply-first scenario processes the zero-delay request
before inserting the deadline; the other inserts the deadline first. This
deliberately constructs both queue orders without implying either is guaranteed
in production.

Every RPC example is executed by RequestReplyScenarioTest; viewer tests consume
the Java-generated exports. The viewer's server card is harness knowledge.
Compare only client observations when explaining why the two timeout histories
are indistinguishable to that client.

A text trace for scheduling and running a five-millisecond task looks like:

~~~text
#0 t=0s TASK_SCHEDULED timeout {delaySeconds=0.005s, dueSeconds=0.005s, taskId=0}
#1 t=0.005s TASK_STARTED timeout {taskId=0}
#2 t=0.005s TASK_COMPLETED timeout {taskId=0}
~~~

Sequence numbers establish trace order; time says when the fact occurred; kind
names the transition; subject identifies its owner or purpose; attributes carry
structured details. Text formatting displays seconds. JSON preserves exact
nanoseconds as decimal strings.

For API contracts and code links, use the
[simulator reference](../../platform/simulator/README.md).


## Quick revision

| Remember | What to be able to explain |
| --- | --- |
| Timeout = unknown outcome | A lost request and a lost reply look the same to the client, but leave different server states |
| Knowledge is local | The test harness can inspect all nodes; the client cannot use the viewer as protocol evidence |
| Safety and progress differ | A request must complete at most once locally, even if it cannot finish successfully |
| Determinism controls choices | Due time plus insertion ID chooses event order; real networks promise neither tie order |
| Client completion is not deduplication | Ignoring a late reply prevents a second callback, but does not stop repeated server effects |
| Simulator time is an observer tool | It is different from a node's physical clock or Lamport clock |
| A trace is an immutable history | Stepping backward reconstructs recorded state; it does not undo Java actions |

The core invariant for 0B is **at most one terminal client transition per
request**. A successful reply lets this example report the Append result; a
timeout makes no assertion about whether the server applied it.

## Learning checklist

Check these only after doing the exercise and explanation yourself:

- [X] Predict scheduler order, cancellation, and final time without running code.
- [X] Explain local-call versus remote-call evidence and the ambiguous timeout.
- [X] State safety and liveness separately, with their assumptions.
- [X] Reproduce both lost-message histories and both equal-time race orders.
- [X] Explain why client completion and server deduplication are separate.
- [X] Add an invariant assertion and explain the failure it catches.
- [X] Distinguish simulated time from causal order and a node's local clock.
- [X] Revisit the quick revision section later and explain a trace without notes.

The implementation acceptance checks cover monotonic time, event order,
cancellation, failures, immutable traces, owner-thread access, runaway scheduling,
contiguous valid trace sequences, and request lifecycle scenarios. Tests use no
Thread.sleep. Repeat runs of each RPC scenario produce identical facts.

**Next:** [retry-safe KV](../01-fault-tolerant-kv/README.md). Keep the
[shared glossary](../../docs/GLOSSARY.md) nearby for lookup; there is no separate
glossary-reading gate.
