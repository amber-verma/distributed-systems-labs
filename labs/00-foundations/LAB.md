# Lab: Checkpoint 0A

## Prerequisites

- JDK 25.
- The committed Gradle wrapper.
- No global Gradle installation is required.

Verify the environment:

    gradlew.bat --version

Run the simulator tests:

    gradlew.bat :platform:simulator:test

## Exercise 1: Predict event order

Without running the code, predict the result:

1. Schedule A after 8 ms.
2. Schedule B after 3 ms.
3. Schedule C after 3 ms.
4. Cancel B.
5. Drain the scheduler.

Write down:

- Execution order.
- Final logical time.
- Final pending count.
- Expected trace kinds.

Then encode the scenario as a test and compare the actual trace.

## Exercise 2: Timeout and reply race

Schedule a reply and timeout at the same logical time in both insertion orders.

Questions:

- Which one runs first in each history?
- Would it be safe for production code to rely on that order?
- What client state would prevent a late event from completing a request twice?

The final question previews request lifecycle state in the next checkpoint.

## Exercise 3: Failed action

Schedule an action that throws.

Confirm:

- Logical time advances to the action's due time.
- The task is no longer pending.
- A Failed trace fact is recorded.
- The exception reaches the test.

Explain why retrying the action automatically inside the scheduler would be the
wrong abstraction.

## Exercise 4: Virtual-thread boundary

Create the scheduler on the test thread and call it from a virtual thread.
Observe the ownership failure.

Discuss why virtual threads are still appropriate for blocking workers and
socket handlers even though the deterministic simulator rejects concurrent
mutation.

## Exercise 5: Add one invariant

Choose one invariant from DESIGN.md and write a focused test that would fail if
the invariant were removed. Prefer a transition-level assertion over checking
only the final output.

## Acceptance checklist

- All tests pass with Java lint warnings treated as errors.
- No test uses Thread.sleep.
- A repeated run produces the same order and trace.
- The timeout is explained as uncertainty, not remote-failure proof.
- Safety and liveness are explained separately.
