# Visual Guide

## 1. One call stack

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

## 2. Remote call: request lost

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

## 3. Remote call: reply lost

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

| Client event | Request-lost world | Reply-lost world |
| --- | --- | --- |
| Request sent | Yes | Yes |
| Reply received | No | No |
| Deadline expired | Yes | Yes |
| Server applied operation | No | Yes |

This is the retry ambiguity we will reproduce in the next checkpoint.

## 4. Deterministic event ordering

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

## 5. Task lifecycle

~~~mermaid
stateDiagram-v2
    [*] --> Pending: schedule
    Pending --> Executed: run
    Pending --> Cancelled: cancel
    Executed --> [*]
    Cancelled --> [*]
~~~

There is no transition from Cancelled or Executed back to Pending.

## 6. Trace walkthrough

Scheduling and running one event produces facts like:

    #0 t=0ns TASK_SCHEDULED timeout {delayNanos=5000000, dueNanos=5000000, taskId=0}
    #1 t=5000000ns TASK_STARTED timeout {taskId=0}
    #2 t=5000000ns TASK_COMPLETED timeout {taskId=0}

Read this from left to right:

1. Sequence numbers establish total trace order.
2. Logical time shows when the fact occurred.
3. The kind identifies the state transition.
4. The subject names the event.
5. Attributes contain stable machine-readable details.

## 7. Thought experiment

Add a reply event and a timeout event with the same due time.

- If the reply was inserted first, the deterministic scheduler delivers it
  first.
- If the timeout was inserted first, the timeout fires first.

A real system cannot assume either order. A correct protocol must be safe in
both histories. The simulator's deterministic tie-breaker lets us test each
history intentionally.
