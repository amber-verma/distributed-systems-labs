# Lab 2: Fault-tolerant KV

**Status:** Planned

## First-principles question

After a timeout, how can a client know whether a remote operation ran?

## Learning path

Begin with an in-memory Get, Put, and Append service. Lose requests and replies,
observe how naive retries duplicate Append, then derive logical request
identity, cached replies, and at-most-once processing.

## Visual learning

- Lost request and lost reply shown side by side.
- Duplicate Append failure.
- Request identity through client, network, and server.
- Deduplication table changes after original and repeated delivery.
- Idempotency, deduplication, and exactly-once claim comparison.

## Planned checkpoints

1. Reliable local KV service.
2. Simulated transport with omission faults.
3. Retrying client and demonstrated duplicate effects.
4. Client ID, request sequence, and reply cache.
5. At-most-once tests and bounded-history design discussion.

## Completion signal

Every accepted logical operation executes zero or one time under tested
failures, without claiming that the system can guarantee exactly-once execution.

## Dependencies

Deterministic simulator and trace visualizer only.
