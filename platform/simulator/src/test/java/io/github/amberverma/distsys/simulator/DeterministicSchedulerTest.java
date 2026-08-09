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

    @Test
    void ordersTasksByDueTimeAndThenByInsertionOrder() {
        var scheduler = new DeterministicScheduler();
        var observed = new ArrayList<String>();

        scheduler.schedule(Duration.ofMillis(10), "late", () -> observed.add("late"));
        scheduler.schedule(Duration.ofMillis(5), "first-at-five", () -> observed.add("first"));
        scheduler.schedule(Duration.ofMillis(5), "second-at-five", () -> observed.add("second"));

        assertEquals(3, scheduler.runUntilIdle());
        assertEquals(List.of("first", "second", "late"), observed);
        assertEquals(Duration.ofMillis(10), scheduler.now());
        assertFalse(scheduler.hasPendingTasks());
    }

    @Test
    void advancesLogicalTimeWithoutWaitingForWallClockTime() {
        var scheduler = new DeterministicScheduler();
        var observed = new ArrayList<String>();

        scheduler.schedule(Duration.ofMillis(5), "due", () -> observed.add("due"));
        scheduler.schedule(Duration.ofMillis(15), "future", () -> observed.add("future"));

        assertEquals(1, scheduler.advanceBy(Duration.ofMillis(10)));
        assertEquals(List.of("due"), observed);
        assertEquals(Duration.ofMillis(10), scheduler.now());
        assertEquals(1, scheduler.pendingTaskCount());

        assertTrue(scheduler.runNext());
        assertEquals(List.of("due", "future"), observed);
        assertEquals(Duration.ofMillis(15), scheduler.now());
    }

    @Test
    void cancellationIsIdempotentAndCancelledTaskNeverExecutes() {
        var scheduler = new DeterministicScheduler();
        var executed = new AtomicReference<>(false);
        var handle = scheduler.schedule(
                Duration.ofSeconds(1),
                "cancelled",
                () -> executed.set(true));

        assertTrue(handle.cancel());
        assertFalse(handle.cancel());
        assertTrue(handle.isCancelled());
        assertTrue(handle.isDone());
        assertEquals(0, scheduler.pendingTaskCount());
        assertFalse(scheduler.runNext());
        assertFalse(executed.get());
        assertEquals(
                List.of(TraceKind.TASK_SCHEDULED, TraceKind.TASK_CANCELLED),
                scheduler.trace().events().stream().map(TraceEvent::kind).toList());
    }

    @Test
    void taskMayScheduleAnotherTaskAtTheSameLogicalTime() {
        var scheduler = new DeterministicScheduler();
        var observed = new ArrayList<String>();

        scheduler.schedule(Duration.ZERO, "outer", () -> {
            observed.add("outer");
            scheduler.schedule(Duration.ZERO, "inner", () -> observed.add("inner"));
        });

        assertEquals(2, scheduler.runUntilIdle());
        assertEquals(List.of("outer", "inner"), observed);
        assertEquals(Duration.ZERO, scheduler.now());
    }

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

        assertEquals("expected failure", failure.getMessage());
        assertTrue(handle.isDone());
        assertEquals(0, scheduler.pendingTaskCount());
        assertEquals(TraceKind.TASK_FAILED, scheduler.trace().events().getLast().kind());
    }

    @Test
    void refusesToMoveLogicalTimeBackward() {
        var scheduler = new DeterministicScheduler();
        scheduler.advanceTo(Duration.ofSeconds(5));

        assertThrows(
                IllegalArgumentException.class,
                () -> scheduler.advanceTo(Duration.ofSeconds(4)));
        assertEquals(Duration.ofSeconds(5), scheduler.now());
    }

    @Test
    void taskLimitDetectsAnEndlessZeroDelayReschedule() {
        var scheduler = new DeterministicScheduler();
        var recurring = new Runnable[1];
        recurring[0] = () -> scheduler.schedule(Duration.ZERO, "again", recurring[0]);
        scheduler.schedule(Duration.ZERO, "start", recurring[0]);

        var failure = assertThrows(
                IllegalStateException.class,
                () -> scheduler.runUntilIdle(3));

        assertTrue(failure.getMessage().contains("still has pending work"));
        assertEquals(1, scheduler.pendingTaskCount());
    }

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

        assertInstanceOf(IllegalStateException.class, observedFailure.get());
    }
}
