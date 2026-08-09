# Lab 5: Sharded KV

**Status:** Planned

## First-principles question

How can a key range move between replicated groups without two groups accepting
writes as its owner?

## Learning path

Start with one replicated KV group, expose its capacity boundary, partition the
keyspace, then derive versioned configuration and an idempotent ownership
transfer protocol.

## Visual learning

- Key to shard to replica-group mapping.
- Replication and sharding comparison.
- Versioned configuration changes.
- Shard ownership state machine.
- Old-owner stop and new-owner start timeline.
- Data and deduplication metadata moving together.
- Transfer retry, acknowledgement, and garbage collection.

## Planned checkpoints

1. Deterministic key-to-shard mapping.
2. Raft-backed shard controller.
3. Join, leave, move, query, and deterministic balancing.
4. Client configuration discovery.
5. Sequential reconfiguration and shard transfer.
6. Idempotent acknowledgement and old-copy collection.
7. Linearizability testing across migration.

## Completion signal

Reconfiguration may temporarily stop progress but never creates two writable
owners, loses accepted operations, or duplicates logical requests.

## Dependencies

Raft, KV over Raft, deterministic simulator, and trace visualizer.
