# Interview Track

The interview track converts implementation experience into concise engineering
reasoning. It runs alongside the labs rather than beginning after them.

## Explanation exercise

For every mechanism, answer:

1. What concrete failure motivates it?
2. What is the smallest example that demonstrates the failure?
3. What invariant does the mechanism preserve?
4. What assumptions are required for safety?
5. What additional assumptions are required for progress?
6. What state must survive a crash?
7. What happens to an old or delayed message?
8. How would an operator detect that the mechanism is unhealthy?

## Design review

Each lab ends with a short design review covering:

- API and protocol boundary.
- State ownership and persistence.
- Normal request path.
- Worst failure trace.
- Backpressure and resource bounds.
- Observability and replay.
- Compatibility and rollout concerns.
- Deliberate simplifications.
- The first changes required for production use.

## Debugging exercise

Given an event trace:

- Reconstruct the state known by each node at each step.
- Separate facts from assumptions based on timeouts.
- Identify the earliest invariant violation.
- Reduce the failure to the shortest reproducing trace.
- Propose a test that fails before the fix and passes afterward.

## Comparison exercise

Every lab compares at least two alternatives. Examples include:

- Lease reassignment versus fixed ownership.
- Idempotent APIs versus general deduplication.
- Leader-based replication versus quorum reads and writes.
- Logging reads versus ReadIndex-style optimization.
- Stop-and-copy shard transfer versus more available migration protocols.

The expected answer includes workload, failure, operational, and complexity
tradeoffs rather than declaring one design universally better.
