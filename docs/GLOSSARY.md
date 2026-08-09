# Glossary

This is a living glossary. Every lab will add terms only after deriving the
problem that motivates them.

## At-most-once processing

A server-side guarantee that a logical request is applied no more than once.
It does not guarantee that the request is applied at all.

In code, a client identity and request sequence let the server recognize a
retry and return the cached result instead of applying the operation again.

## Commit

The point at which a protocol has made a result authoritative under its stated
rules. Commitment is protocol-specific. It does not necessarily mean that every
replica has applied or persisted the result.

## Consensus

Agreement among participants on a single value or ordered decision despite
specified failures. Leader election supports consensus but is not, by itself,
the whole consensus problem.

## Deduplication

Recognizing multiple deliveries of the same logical request and suppressing
repeated effects. Deduplication requires identity and retained history; a retry
alone carries neither.

## Idempotent operation

An operation whose repeated application has the same intended effect as one
application. Put of the same value is commonly idempotent; Append generally is
not.

Idempotency is a property of an operation. Deduplication is a protocol
mechanism.

## Invariant

A property that remains true after every allowed state transition. Invariants
are the bridge between a protocol's guarantee and its implementation tests.

## Lease

Time-bounded authority granted by one component to another. Expiration allows
work to be reassigned, but an expired worker may still be running. A lease
therefore requires fencing or attempt identity when stale work could be harmful.

## Linearizability

A correctness condition in which each completed operation appears to take
effect at one instant between its invocation and response, while respecting
real-time ordering between non-overlapping operations.

## Partition

A communication failure that divides nodes into groups that cannot exchange
messages even though the nodes themselves may remain healthy.

## Quorum

A set large enough that any two such sets intersect under the protocol's
membership rules. Raft uses majority quorums so two independently committed
decisions cannot avoid sharing at least one voter.

## Replication

Maintaining copies of data or state on multiple nodes for availability or
durability. Replication does not automatically make the copies consistent.

## Sharding

Partitioning a dataset so different subsets are owned by different groups.
Replication creates copies; sharding divides ownership.
