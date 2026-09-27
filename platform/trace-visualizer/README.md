# Trace Visualizer

**Status:** v1 implemented for checkpoints 0A and 0B.

The viewer is an offline HTML page for inspecting executable lab histories.
Use it after predicting a scenario, then explain each transition as you step.

## Open a trace

From the repository root:

~~~powershell
.\gradlew.bat :platform:simulator:exportTraces
~~~

Use ./gradlew on macOS/Linux. Open [index.html](index.html) in a modern browser,
then select a JSON file from platform/simulator/build/traces/. File import works
directly from disk; there is no server, package installation, or upload.
Pasting an exported trace is also supported.

Start with scheduler-order.json. Then compare lost-request.json and
lost-reply.json: both clients time out, but only the second server contains x.
The [Foundations chapter](../../labs/00-foundations/README.md#run-and-inspect)
lists all examples and their intended lessons.

## Reading the page

- Start returns to the point before the first fact. Next/Previous step through
  facts; End shows the completed history. The slider and event buttons jump.
- Recorded state contains only STATE_CHANGED observations up to the selected
  event. Stepping backward discards later observations from the display.
- Message lanes show each transmission's source, destination, identity, payload,
  and in-flight/delivered/dropped status. Rows follow send order, with explicit
  times; vertical spacing does not encode elapsed time.
- Scheduled timers and events include pending network deliveries as well as
  deadlines. The selected-event panel exposes the underlying fact.
- The server card is the harness's knowledge. The client cannot use that card
  to resolve its timeout.
- Empty traces are supported. Invalid JSON, unsupported schema versions, or
  malformed events produce an error while preserving the previous valid view.

## Development checks

JDK 25 generates the fixtures; Node.js 22 or newer runs the dependency-free model
tests (CI uses Node.js 24):

~~~powershell
.\gradlew.bat check javadoc :platform:simulator:exportTraces
node --test platform/trace-visualizer/viewer.test.cjs
~~~

The tests consume Java-generated files, rather than a separate set of invented
visual examples. They cover scheduler histories, RPC outcomes, rewind, empty
input, malformed input, and exact nanosecond formatting. No Node installation
is required to open the viewer itself.

The [simulator reference](../simulator/README.md#trace-and-export-contract)
documents schema version 1. Unknown future event kinds can still be inspected
in event details; specialized views require their own observations.

## Later extensions

Add deduplication tables with the retry-safe KV lab, task/attempt state with
MapReduce, replica logs with Raft, and ownership with sharding. Add automatic
playback or richer topology only when it improves a concrete exercise.

This viewer replays recorded observations. It does not rerun Java code, infer
unrecorded state, or offer interactive fault injection. Reproducing execution
currently means rerunning the named deterministic scenario.
