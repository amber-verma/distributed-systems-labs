# Lab 1: RPC and Retry-safe KV

**Status:** Local KV contract, starter, and acceptance tests are ready.
The operation implementations are intentionally left for you to write.

**Learning order:** next after [Foundations](../00-foundations/README.md), before
[MapReduce](../02-mapreduce/README.md).

## Your current exercise: local KV

Implement a local, in-memory string key/value store. This checkpoint uses direct
Java method calls on one thread. It needs no simulator or external dependencies
at runtime.

Read the contract in
[KeyValueStore.java](src/main/java/io/github/amberverma/distsys/kv/KeyValueStore.java),
then implement
[InMemoryKeyValueStore.java](src/main/java/io/github/amberverma/distsys/kv/InMemoryKeyValueStore.java).
That is your only required implementation file. Keep its public constructor and
method signatures; add private fields and helpers as needed. Choose your own
data structure. The starter contains no KV operation logic.

| Method | Required behavior |
| --- | --- |
| `Optional<String> get(String key)` | Return the current value, or `Optional.empty()` if the key is missing. Do not change state. |
| `void put(String key, String value)` | Create the key or replace its entire value. |
| `String append(String key, String suffix)` | Concatenate the suffix after the current value, store it, and return the full updated value. A missing key starts with an empty value. |
| `boolean delete(String key)` | Remove the key and its value. Return `true` if it existed, otherwise `false`. |

### Contract details

- A new store is empty. Different instances have independent state.
- All non-null strings are valid keys and values, including empty strings,
  whitespace, and Unicode. Preserve them exactly. Keys are case-sensitive.
- `Optional.empty()` means missing; `Optional.of("")` means a present empty value.
- Appending an empty suffix to a missing key creates a present empty value.
- Repeated Append calls each apply their suffix again.
- Delete removes even a key holding an empty value. Deleting an already missing
  key returns `false` without changing state. Put or Append can recreate a deleted key.
- Any null argument throws `NullPointerException` before any stored state changes.
  The exception message is your choice.
- Operations on one key leave other keys unchanged.
- Calls are sequential. State lasts only for that store instance's lifetime.

### Example

Starting with a new store, the following calls happen in order:

| Call | Return value | Value stored at `note` afterward |
| --- | --- | --- |
| `get("note")` | `Optional.empty()` | Missing |
| `put("note", "A")` | No return value | `"A"` |
| `append("note", "B")` | `"AB"` | `"AB"` |
| `append("note", "B")` | `"ABB"` | `"ABB"` |
| `get("note")` | `Optional.of("ABB")` | `"ABB"` |
| `put("note", "")` | No return value | `""` (present) |
| `get("note")` | `Optional.of("")` | `""` (present) |
| `delete("note")` | `true` | Missing |
| `get("note")` | `Optional.empty()` | Missing |
| `delete("note")` | `false` | Missing |
| `append("note", "C")` | `"C"` | `"C"` |

### Run your acceptance tests

From the repository root in PowerShell:

~~~powershell
.\gradlew.bat :labs:01-fault-tolerant-kv:exerciseTest
~~~

On macOS/Linux use `./gradlew` with the same task. The 24 visible tests are in
[LocalKeyValueStoreContractTest.java](src/test/java/io/github/amberverma/distsys/kv/LocalKeyValueStoreContractTest.java).
They check behavior through the API and do not require a particular data structure.
You may read them, and you may add your own tests. Preserve the supplied assertions.

The starter deliberately throws `UnsupportedOperationException`, so the acceptance
tests initially fail. Implement the methods and rerun the task as you work.
The HTML report is at `build/reports/tests/exerciseTest/index.html` inside this lab.

To run one acceptance test:

~~~powershell
.\gradlew.bat :labs:01-fault-tolerant-kv:exerciseTest --tests "*LocalKeyValueStoreContractTest.putCreatesAReadableValue"
~~~

The acceptance class is tagged `local-kv-exercise` and runs through `exerciseTest`.
The regular `check` task compiles the starter and tests but excludes that tag while
the exercise is unfinished. **A green `check` does not mean you passed this exercise.**
After implementation and review, include these acceptance tests in the normal
verification path.

### Completion and review

- [ ] Implement the four methods without changing the public API.
- [ ] Pass all acceptance tests, including null-input and empty-string cases.
- [ ] Explain the time and space costs, including how Append depends on string length.
- [ ] Ask for a code review of your implementation.

The review will cover contract correctness, edge cases, state isolation, clarity,
and complexity. Passing tests are one part of that review. Keep this checkpoint
focused on the local operations; the later protocol work starts after your review.

## Later: RPC and retry safety

The next question will be: after a timeout, how can a client know whether a remote
operation ran?

### Learning path

Begin with an in-memory Get, Put, Append, and Delete service. Lose requests and replies,
observe how naive retries duplicate Append, then derive logical request
identity, cached replies, and at-most-once processing.

Assume one outstanding operation per logical client. The initial server remains
alive and retains its in-memory deduplication state. Crash-safe guarantees need
data and deduplication state to survive together; persistence is a later exercise.

### Visual learning

- Lost request and lost reply shown side by side.
- Duplicate Append failure.
- Request identity through client, network, and server.
- Deduplication table changes after original and repeated delivery.
- Idempotency, deduplication, and exactly-once claim comparison.

### Lab checkpoints

1. Reliable local KV service: current coding exercise.
2. Simulated transport with omission faults.
3. Retrying client and demonstrated duplicate effects.
4. Client ID, request sequence, and reply cache.
5. At-most-once tests and bounded-history design discussion.

### Whole-lab completion signal

Every accepted logical operation executes zero or one time under tested
failures, without claiming that the system can guarantee exactly-once execution.

### Later dependencies

The later RPC checkpoints use the scripted request/reply simulator from
Foundations. Use the existing trace viewer to inspect those failures; a new
viewer feature is not a learning prerequisite. The local coding exercise is
independent of that platform.
