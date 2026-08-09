# Lab 4: KV over Raft

**Status:** Planned

## First-principles question

When can a replicated service safely tell a client that an operation happened?

## Learning path

Turn the Raft log into a state-machine boundary. Route all initial operations
through the log, replicate deduplication state, correlate requests with applied
commands, and validate external histories for linearizability.

## Visual learning

- Client request through leader, log, majority, commit, apply, and reply.
- Invocation, linearization point, and response.
- Concurrent read and write histories.
- Retry after a committed operation's reply is lost.
- A partitioned former leader receiving requests.
- Commitment versus state-machine application.

## Planned checkpoints

1. KV command and reply model.
2. Pending request correlation.
3. Client leader discovery and retry.
4. Replicated deduplication state.
5. KV and session snapshots.
6. Java linearizability checker and generated histories.

## Completion signal

Concurrent histories remain linearizable through leader changes, lost replies,
partitions, crashes, restarts, and snapshot recovery.

## Dependencies

Raft, deterministic simulator, trace visualizer, and stable storage.
