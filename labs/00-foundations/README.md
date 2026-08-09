# Phase 0: Foundations

**Status:** Checkpoint 0A complete

## First-principles question

What assumptions disappear when an ordinary Java method call crosses a network
boundary?

Phase 0 begins before RPC frameworks, consensus, or replication. We first need
to understand what a caller can and cannot know after a remote timeout. That
uncertainty motivates request identity, retry rules, leases, replicated logs,
and most of the later course.

## Checkpoint 0A

This checkpoint establishes:

- The difference between a local call and a remote interaction.
- Why a timeout is evidence of uncertainty rather than proof of failure.
- Safety and liveness as separate dimensions.
- Logical time as a deterministic test mechanism.
- A single-threaded event scheduler with replayable trace facts.

The scheduler is intentionally smaller than a network simulator. Message loss,
duplication, reordering, partitions, crash, and restart are the next Phase 0
checkpoint.

## Learning order

1. Read [Concepts](CONCEPTS.md).
2. Walk through [Visual Guide](VISUAL_GUIDE.md).
3. Review the [Glossary](GLOSSARY.md).
4. Inspect the implementation decisions in [Design](DESIGN.md).
5. Run the exercises in [Lab](LAB.md).
6. Use [Interview](INTERVIEW.md) for explanation and design practice.

## Implementation

The reusable scheduler and trace model live in
[platform/simulator](../../platform/simulator/README.md).

Run the checkpoint:

    gradlew.bat :platform:simulator:test

## Completion criteria

- Logical time never moves backward.
- Earlier due events execute first.
- Equal-time events execute in insertion order.
- Cancelled events never execute.
- A failed action is recorded and is not left pending.
- Trace snapshots and attributes are immutable.
- Runaway zero-delay rescheduling is detected.
- The scheduler rejects access from a second platform or virtual thread.
