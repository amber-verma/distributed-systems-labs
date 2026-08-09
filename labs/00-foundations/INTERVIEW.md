# Interview Track: Foundations

## 1. The client timed out. Did the write happen?

A strong answer should:

- Say that the timeout alone is insufficient.
- Separate lost request, server failure, lost reply, and delay.
- Explain that these worlds are locally indistinguishable to the client.
- Ask what request identity, server deduplication, persistence, or read-back
  mechanisms exist.
- Avoid claiming that a longer timeout solves the semantic ambiguity.

## 2. Why not retry every failed request?

Discuss:

- Idempotent and non-idempotent operations.
- Logical request identity.
- Duplicate execution after a lost reply.
- At-most-once processing versus guaranteed completion.
- Business-level idempotency keys for payments.

## 3. Is a timeout a failure detector?

Explain that it is an imperfect failure detector: it can suspect a failed
process but can also suspect a slow or partitioned healthy process. State what
the protocol does with suspicion and which safety rules remain necessary.

## 4. Why build a simulator instead of using integration tests?

A strong answer includes:

- Reproducible ordering and logical time.
- Testing rare interleavings without wall-clock waits.
- Seed and trace replay.
- Invariant checks after every transition.
- The continuing need for separate real-thread, socket, and persistence tests.

The answer should not imply that simulation proves production correctness.

## 5. Why force one scheduler thread?

The goal is not throughput. The goal is to make the test harness, rather than
the operating system, choose the next protocol event. Production adapters can
remain concurrent while their protocol effects are tested deterministically.

## 6. What is the difference between safety and availability?

Use a partitioned replicated service:

- Refusing writes may preserve safety but reduce availability.
- Accepting writes on both sides may improve short-term availability while
  violating a single-owner or strong-consistency guarantee.

State the exact guarantee before choosing behavior.

## 7. Debugging prompt

A trace shows:

1. A timeout fires.
2. The client retries.
3. The original reply arrives.
4. The retry reply arrives.
5. Two callbacks mutate client state.

Identify the missing invariant and propose the smallest client-side lifecycle
state that prevents double completion. Do not yet solve server-side duplicate
effects; that belongs to the KV retry lab.
