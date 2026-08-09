# Concepts: From Local Calls to Distributed Uncertainty

## 1. Begin with the smallest possible system

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

## 2. Move the callee across a network

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

## 3. The key first-principles result

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

## 4. Knowledge is local

Distributed protocols should be read as rules about knowledge.

- A node knows only its local state and received messages.
- A timeout adds the fact that no qualifying message arrived before a local
  deadline.
- Silence does not distinguish a crashed node from a slow or partitioned node.
- A message proves that its sender produced that message at some earlier point;
  it does not prove the sender is still alive now.

Later mechanisms create stronger evidence:

- Request IDs let a server recognize the same logical request.
- Quorums ensure that two authoritative decisions intersect.
- Terms and log indexes identify the context of a Raft statement.
- Fencing tokens let a receiver reject stale authority.

None of these remove uncertainty universally. They make particular decisions
safe under explicitly stated assumptions.

## 5. Safety and liveness

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
availability temporarily, but it does not violate safety. Interview answers
should always state safety and liveness separately.

## 6. Why wall-clock tests are inadequate

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

## 7. Logical time

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

## 8. Why equal-time insertion order matters

Two events can have the same due time. A priority queue ordered only by time is
free to return them in an unspecified order.

That would make two runs of the same scenario disagree. The scheduler therefore
uses a second key: a monotonically increasing task ID assigned at insertion.

The ordering is:

1. Smaller due time first.
2. For equal due times, smaller task ID first.

This is not a claim about how a real network orders simultaneous events. It is a
deterministic tie-breaker for the simulator.

## 9. Why the scheduler has one owning thread

The simulator controls interleaving explicitly. If multiple real threads could
mutate its event queue concurrently, operating-system scheduling would
reintroduce hidden nondeterminism.

The scheduler records its creating thread and rejects calls from every other
thread, including virtual threads. Later runtime adapters may use virtual
threads, but protocol correctness tests remain under one deterministic event
loop.

## 10. Scheduler invariants

After every public operation:

- Logical time is non-negative and never decreases.
- Each task is pending, cancelled, or executed.
- A task transitions out of pending at most once.
- The pending count equals the number of pending tasks.
- Cancelled tasks never run.
- An executed task never becomes pending again.
- Trace sequence numbers increase by one.
- Published trace events and attributes are immutable.

These invariants are more valuable than a test that checks only the final list
of executed actions.

## 11. Mapping the reasoning to Java

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

## 12. Boundary of this checkpoint

Checkpoint 0A controls time and event ordering. It does not yet simulate:

- Requests and replies.
- Message loss or duplication.
- Reordering caused by different network delays.
- Crashed endpoints.
- Network partitions.
- Stable state across restart.

Those behaviors will be layered on this scheduler rather than mixed into its
core ordering rules.
