package io.github.amberverma.distsys.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DeterministicSchedulerTest {

    /**
     * Earlier due tasks run first; tasks due together use their insertion order.
     * Use case: a future network simulator can deliver delayed messages in a
     * repeatable order, including messages that arrive at the same logical time.
     */
    @Test
    void ordersTasksByDueTimeAndThenByInsertionOrder() {
        var scheduler = new DeterministicScheduler();
        var observed = new ArrayList<String>();

        scheduler.schedule(Duration.ofMillis(10000), "late", () -> observed.add("late"));
        scheduler.schedule(Duration.ofMillis(5000), "first-at-five", () -> observed.add("first"));
        scheduler.schedule(Duration.ofMillis(5000), "second-at-five", () -> observed.add("second"));

        System.out.printf(
                "Before run: logical time=%s, pending tasks=%d%n",
                scheduler.now(), scheduler.pendingTaskCount());
        var executed = scheduler.runUntilIdle();
        logScenario("Tasks ordered by due time and insertion order",
                "The two tasks due at 5s run in insertion order, before the task due at 10s.",
                "Compare delayed message deliveries, including equal-time arrivals.", scheduler,
                "executed=" + executed + ", observed order=" + observed);

        assertEquals(3, executed);
        assertEquals(List.of("first", "second", "late"), observed);
        assertEquals(Duration.ofMillis(10000), scheduler.now());
        assertFalse(scheduler.hasPendingTasks());
    }

    /**
     * Advancing to 10 ms runs the 5 ms task but leaves the 15 ms task pending.
     * Use case: inspect a client after a timeout but before a delayed reply arrives.
     */
    @Test
    void advancesLogicalTimeWithoutWaitingForWallClockTime() {
        var scheduler = new DeterministicScheduler();
        var observed = new ArrayList<String>();

        scheduler.schedule(Duration.ofMillis(5), "due", () -> observed.add("due"));
        scheduler.schedule(Duration.ofMillis(15), "future", () -> observed.add("future"));

        var advanced = scheduler.advanceBy(Duration.ofMillis(10));
        System.out.printf(
                "After advanceBy(0.01s): executed=%d, observed=%s, logical time=%s, pending tasks=%d%n",
                advanced, observed, scheduler.now(), scheduler.pendingTaskCount());
        assertEquals(1, advanced);
        assertEquals(List.of("due"), observed);
        assertEquals(Duration.ofMillis(10), scheduler.now());
        assertEquals(1, scheduler.pendingTaskCount());

        var ranNext = scheduler.runNext();
        logScenario("Advance logical time, then run the next task",
                "advanceBy runs work due by 10 ms; runNext later runs the 15 ms task.",
                "Inspect a timeout boundary before a delayed reply arrives.", scheduler,
                "runNext=" + ranNext + ", observed order=" + observed);
        assertTrue(ranNext);
        assertEquals(List.of("due", "future"), observed);
        assertEquals(Duration.ofMillis(15), scheduler.now());
    }

    /**
     * The first cancellation removes pending work; cancelling again changes nothing.
     * Use case: cancel a client's retry timeout when its reply arrives first.
     */
    @Test
    void cancellationIsIdempotentAndCancelledTaskNeverExecutes() {
        var scheduler = new DeterministicScheduler();
        var executed = new AtomicReference<>(false);
        var handle = scheduler.schedule(
                Duration.ofSeconds(1),
                "cancelled",
                () -> executed.set(true));

        var firstCancellation = handle.cancel();
        var secondCancellation = handle.cancel();
        var ranNext = scheduler.runNext();
        logScenario("Cancel a task before it runs",
                "The first cancel succeeds, the second does nothing, and the action never runs.",
                "Cancel a retry timeout after the reply arrives.", scheduler,
                "first cancel=" + firstCancellation + ", second cancel=" + secondCancellation
                        + ", runNext=" + ranNext + ", action executed=" + executed.get());

        assertTrue(firstCancellation);
        assertFalse(secondCancellation);
        assertTrue(handle.isCancelled());
        assertTrue(handle.isDone());
        assertEquals(0, scheduler.pendingTaskCount());
        assertFalse(ranNext);
        assertFalse(executed.get());
        assertEquals(
                List.of(TraceKind.TASK_SCHEDULED, TraceKind.TASK_CANCELLED),
                scheduler.trace().events().stream().map(TraceEvent::kind).toList());
    }

    /**
     * An action may queue a zero-delay follow-up at its current logical time.
     * Use case: handling a delivered message can immediately queue a separate
     * reply-preparation step without making the two actions concurrent.
     */
    @Test
    void taskMayScheduleAnotherTaskAtTheSameLogicalTime() {
        var scheduler = new DeterministicScheduler();
        var observed = new ArrayList<String>();

        scheduler.schedule(Duration.ZERO, "outer", () -> {
            observed.add("outer");
            scheduler.schedule(Duration.ZERO, "inner", () -> observed.add("inner"));
        });

        var executed = scheduler.runUntilIdle();
        logScenario("An action schedules another task at the same logical time",
                "The outer action finishes before its zero-delay inner task runs at time zero.",
                "Queue reply preparation immediately after processing a delivered message.", scheduler,
                "executed=" + executed + ", observed order=" + observed);
        assertEquals(2, executed);
        assertEquals(List.of("outer", "inner"), observed);
        assertEquals(Duration.ZERO, scheduler.now());
    }

    /**
     * A throwing action is recorded as failed and is no longer pending.
     * Use case: a simulated message handler bug remains visible in the trace and
     * cannot silently cause the same action to run again.
     */
    @Test
    void failedTaskIsFinishedAndFailureIsRecorded() {
        var scheduler = new DeterministicScheduler();
        var handle = scheduler.schedule(
                Duration.ofMillis(2),
                "failing-task",
                () -> {
                    throw new IllegalStateException("expected failure");
                });

        var failure = assertThrows(IllegalStateException.class, scheduler::runNext);
        logScenario("A task throws an exception",
                "The scheduler records TASK_FAILED, rethrows the error, and does not retry the task.",
                "Pinpoint a simulated message handler failure in the trace.", scheduler,
                "failure=" + failure + ", handle done=" + handle.isDone());

        assertEquals("expected failure", failure.getMessage());
        assertTrue(handle.isDone());
        assertEquals(0, scheduler.pendingTaskCount());
        assertEquals(TraceKind.TASK_FAILED, scheduler.trace().events().getLast().kind());
    }

    /**
     * An absolute target earlier than the current logical time is rejected.
     * Use case: prevent later simulated events from appearing before events that
     * have already occurred in a protocol trace.
     */
    @Test
    void refusesToMoveLogicalTimeBackward() {
        var scheduler = new DeterministicScheduler();
        scheduler.advanceTo(Duration.ofSeconds(5));

        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> scheduler.advanceTo(Duration.ofSeconds(4)));
        logScenario("Reject a move backward in logical time",
                "After reaching 5s, advanceTo(4s) throws and leaves the clock at 5s.",
                "Preserve causal order in a protocol trace.", scheduler,
                "requested time=4s, rejection=" + failure.getMessage());
        assertEquals(Duration.ofSeconds(5), scheduler.now());
    }

    /**
     * A task budget stops an action that endlessly queues itself with zero delay.
     * Use case: detect an immediate-retry loop that would otherwise hang a
     * simulation while logical time remains unchanged.
     */
    @Test
    void taskLimitDetectsAnEndlessZeroDelayReschedule() {
        var scheduler = new DeterministicScheduler();
        var recurring = new Runnable[1];
        recurring[0] = () -> scheduler.schedule(Duration.ZERO, "again", recurring[0]);
        scheduler.schedule(Duration.ZERO, "start", recurring[0]);

        var failure = assertThrows(
                IllegalStateException.class,
                () -> scheduler.runUntilIdle(3));
        logScenario("Stop an endless zero-delay reschedule",
                "After three executions, another zero-delay task is still pending, so the run fails.",
                "Expose an immediate-retry loop instead of hanging the test.", scheduler,
                "task limit=3, failure=" + failure.getMessage());

        assertTrue(failure.getMessage().contains("still has pending work"));
        assertEquals(1, scheduler.pendingTaskCount());
    }

    /**
     * A virtual thread cannot call a scheduler created by the test thread.
     * Use case: keep simulated event order under one owner's control even when
     * runtime components later use virtual threads.
     */
    @Test
    void rejectsAccessFromAnotherThreadIncludingAVirtualThread() throws InterruptedException {
        var scheduler = new DeterministicScheduler();
        var observedFailure = new AtomicReference<Throwable>();

        var thread = Thread.ofVirtual().start(() -> {
            try {
                scheduler.now();
            } catch (Throwable failure) {
                observedFailure.set(failure);
            }
        });
        thread.join();

        logScenario("Reject access from a virtual thread",
                "The scheduler rejects now() because the caller is not its owning thread.",
                "Keep virtual-thread effects inside the deterministic event loop.", scheduler,
                "owner thread ID=" + Thread.currentThread().threadId()
                        + ", caller thread ID=" + thread.threadId()
                        + ", rejection=" + observedFailure.get());
        assertInstanceOf(IllegalStateException.class, observedFailure.get());
    }

    /** Prints each scenario's behavior, motivation, trace, and observed outcome. */
    private static void logScenario(
            String scenario,
            String explanation,
            String useCase,
            DeterministicScheduler scheduler,
            String outcome) {
        System.out.println();
        System.out.println("=== " + scenario + " ===");
        System.out.println("What it shows: " + explanation);
        System.out.println("Use case: " + useCase);
        System.out.println("Trace:");
        var formattedTrace = scheduler.trace().formatAsText();
        if (formattedTrace.isEmpty()) {
            System.out.println("(no trace events)");
        } else {
            System.out.print(formattedTrace);
        }
        System.out.printf(
                "Outcome: %s%nFinal logical time=%s, pending tasks=%d%n",
                outcome, scheduler.now(), scheduler.pendingTaskCount());
    }
}
