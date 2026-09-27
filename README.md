# Distributed Systems Labs

Learn distributed systems from first principles by implementing small Java
protocols and making their failure modes reproducible.

For each mechanism: start with a simple solution, introduce a failure, observe
what breaks, derive the minimum repair, and express its guarantee as an
invariant. Explain both the guarantee and its limits before moving on.

## Start here

You need JDK 25; the committed Gradle wrapper supplies Gradle. No global Gradle
installation or application framework is required.

From the repository root, in Windows PowerShell:

~~~powershell
.\gradlew.bat --version
.\gradlew.bat check javadoc
.\gradlew.bat :platform:simulator:exportTraces
~~~

On macOS/Linux use the same tasks with ./gradlew.

1. Open [Foundations](labs/00-foundations/README.md). Its quick revision section
   is the return point; its guided chapter is the first-study path.
2. Predict the lost-request and lost-reply outcomes before running the scenarios.
3. Open the [trace viewer](platform/trace-visualizer/index.html) in your browser.
   Import a generated JSON file from platform/simulator/build/traces/.
   See the [viewer instructions](platform/trace-visualizer/README.md).
4. Complete the Foundations self-checks, then continue to
   [retry-safe KV](labs/01-fault-tolerant-kv/README.md).

## Current progress and learning order

Implementation status is evidence about the code. Your explanation and exercise
checklist in each active chapter records learning separately.

| Lab | Topic | Implementation status |
| --- | --- | --- |
| 00 | [Foundations](labs/00-foundations/README.md) | 0A scheduler and 0B scripted request/reply implemented and tested |
| 01 | [Retry-safe KV](labs/01-fault-tolerant-kv/README.md) | Next; planned |
| 02 | [MapReduce](labs/02-mapreduce/README.md) | Planned |
| 03 | [Raft](labs/03-raft/README.md) | Planned |
| 04 | [KV over Raft](labs/04-kv-over-raft/README.md) | Planned |
| 05 | [Sharded KV](labs/05-sharded-kv/README.md) | Planned |

Folder numbers follow the learning order. The KV retry lab and MapReduce are
independent.

The only Java build module is currently platform:simulator. It contains
logical time, cancellation, immutable traces, scripted message delay/loss, and
one-shot request lifecycle examples. The first offline trace viewer is
implemented. Crash/restart, stable storage, partitions, seeded faults, and the
distributed protocols remain future work.

## Where material lives

| Location | Use it for |
| --- | --- |
| [Learning plan](docs/LEARNING_PLAN.md) | Sequence, scope, completion criteria, and later depth |
| Each lab README | First-principles chapter, diagrams, exercises, and revision |
| [Simulator reference](platform/simulator/README.md) | Shared API contracts, invariants, and implementation decisions |
| [Glossary](docs/GLOSSARY.md) | Looking up a term after meeting its motivating problem |
| [Reading list](docs/READING_LIST.md) | A paper paired with a specific question |
| [TCP transport](platform/tcp-transport/README.md) | Planned real-network capstone |

Future topic folders intentionally contain short outlines. Expand a topic's
README when its implementation begins; there is no requirement to create a
separate concepts, design, interview, or visual-guide file for every lab.

## Technology and boundaries

- Java 25, Gradle Kotlin DSL, JUnit, and plain Java protocol code.
- Immutable data and explicit state transitions; virtual threads for later
  blocking runtime adapters, with deterministic protocol tests on one thread.
- The viewer is plain HTML/JavaScript with no build or server requirement.
  Node.js 22 or newer is used only for its development tests (CI uses 24).
- Core failures are non-Byzantine crashes, recovery, message loss, delay,
  duplication, reordering, and partitions. Their implementation is incremental.
- Authentication, TLS, production deployment, and multi-key transactions are
  outside the initial implementation core. Transactions return as a later
  focused extension.

This project is licensed under the [MIT License](LICENSE).
