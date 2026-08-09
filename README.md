# Distributed Systems Labs

A first-principles study of distributed systems, implemented in modern Java.

The goal is not merely to produce working implementations. Each lab derives a
protocol from the failure it must survive, states its guarantees precisely, and
tests those guarantees with deterministic failure injection.

## Learning approach

Every concept follows the same sequence:

1. Start with the simplest single-node solution.
2. Introduce one concrete failure.
3. Observe exactly what breaks.
4. Derive the minimum mechanism needed to address it.
5. Name and define the mechanism.
6. State what it guarantees and what it does not.
7. Express the guarantee as an invariant.
8. Map the invariant to Java code, executable traces, and tests.

Complex terminology is introduced only after its motivating problem is clear.
Visual timelines, state machines, log views, and interactive trace replays are
part of the learning material rather than decorative additions.

## Roadmap

| Lab | Central question | Status |
| --- | --- | --- |
| [Phase 0: Foundations](labs/00-foundations/README.md) | What assumptions disappear when a Java call crosses a network boundary? | Checkpoint 0A complete |
| [MapReduce](labs/01-mapreduce/README.md) | How can work be retried safely when workers fail? | Planned |
| [Fault-tolerant KV](labs/02-fault-tolerant-kv/README.md) | What does a timeout tell a client, and how can retries avoid duplicate effects? | Planned |
| [Raft](labs/03-raft/README.md) | How can replicas agree on one ordered log through crashes and partitions? | Planned |
| [KV over Raft](labs/04-kv-over-raft/README.md) | How does a replicated log become a linearizable service? | Planned |
| [Sharded KV](labs/05-sharded-kv/README.md) | How can ownership move without creating two writable owners? | Planned |

The sequence is intentionally cumulative. MapReduce and the first KV lab are
independent. KV over Raft depends on Raft, and sharded KV builds on both.

The current implementation checkpoint is Phase 0A: deterministic logical time,
event ordering, cancellation, and immutable trace facts.

## Repository structure

    docs/       Course plan, foundations, terminology, readings, and interview track
    labs/       One independently documented module per distributed-systems concept
    platform/   Deterministic simulation, trace visualization, and TCP transport

Each lab will grow to contain:

- CONCEPTS.md for first-principles theory and failure analysis.
- VISUAL_GUIDE.md for diagrams and guided trace walkthroughs.
- GLOSSARY.md for plain-language and precise definitions.
- DESIGN.md for invariants, interfaces, and tradeoffs.
- LAB.md for implementation checkpoints.
- INTERVIEW.md for design and debugging discussions.
- Java source, deterministic tests, and interactive visuals.

## Technology direction

- Java 25 LTS, using stable language and runtime features by default.
- Gradle Kotlin DSL with a committed wrapper.
- Records, sealed types, pattern matching, immutable data, and virtual threads.
- JUnit plus deterministic and model-based testing.
- Plain Java protocol implementations without Spring, gRPC, or a database.

The Java and Gradle toolchain will be introduced as the first verified
implementation checkpoint. The documentation-first initial commit intentionally
contains no untested build files.

## Failure model

The core labs cover non-Byzantine crash and recovery, message loss, delay,
duplication, reordering, and network partitions. They do not assume synchronized
clocks. Byzantine behavior, authentication, TLS, multi-key transactions, and
production deployment are outside the core curriculum.

## Course documents

- [Learning plan](docs/LEARNING_PLAN.md)
- [Foundations](docs/FOUNDATIONS.md)
- [Glossary](docs/GLOSSARY.md)
- [Primary reading list](docs/READING_LIST.md)
- [Interview track](docs/INTERVIEW_TRACK.md)

## License

This project is licensed under the [MIT License](LICENSE).
