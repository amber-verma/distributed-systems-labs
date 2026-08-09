# Lab 3: Raft

**Status:** Planned

## First-principles question

How can replicas agree on one durable command order when nodes crash, restart,
and lose contact with one another?

## Learning path

Build Raft as four independently testable layers: elections, log replication,
crash recovery, and snapshot-based compaction.

## Visual learning

- Follower, candidate, and leader state machine.
- Election terms, split votes, and isolated leaders.
- Per-replica log alignment by index and term.
- Conflict detection and repair.
- Majority replication and commitment.
- Persistent versus volatile state.
- Snapshot installation for a lagging follower.

## Planned checkpoints

1. RequestVote and election timers.
2. AppendEntries and heartbeat behavior.
3. Log repair, replication, commitment, and application.
4. Persistent term, vote, and log.
5. Crash, restart, and partition scenarios.
6. Snapshot, compaction, and InstallSnapshot.

## Completion signal

Election safety, log matching, leader completeness, monotonic commitment, and
state-machine safety hold after every simulated event.

## Dependencies

Deterministic simulator, trace visualizer, and stable-storage abstraction.
