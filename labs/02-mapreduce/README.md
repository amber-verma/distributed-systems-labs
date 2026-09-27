# Lab 2: MapReduce

**Status:** Planned

**Learning order:** after [retry-safe KV](../01-fault-tolerant-kv/README.md), though
the two labs are independent. See the [course sequence](../../docs/LEARNING_PLAN.md#sequence-and-dependencies).

## First-principles question

How can a large deterministic computation be divided across workers when any
worker may be slow, disappear, or finish after its work has been reassigned?

## Learning path

Start with a sequential map/reduce executor. Add task partitioning, a
coordinator, virtual-thread workers, and simulated RPC. Then inject failure one
case at a time until leases, retries, attempt identities, and atomic publication
become necessary.

## Visual learning

- Input, map, shuffle, reduce, and output pipeline.
- Coordinator task-state machine.
- Lease expiration and reassignment timeline.
- Lost completion reply.
- Duplicate attempts racing to publish one result.

## Planned checkpoints

1. Generic functions and sequential executor.
2. Coordinator and task phases.
3. Virtual-thread workers.
4. Simulated RPC and deterministic faults.
5. Leases, retries, duplicate attempts, and output fencing.
6. Word-count and inverted-index applications.

## Completion signal

The result remains correct through named crash points, and the distinction
between duplicate execution and single-result commitment can be explained and
demonstrated from an event trace.

## Dependencies

The deterministic simulator. Add worker crash and stale-attempt scenarios as
their motivating failures appear. Use the existing viewer and extend its task
state observations alongside the lab.
