package io.github.amberverma.distsys.simulator;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Single-threaded event scheduler driven by logical rather than wall-clock time.
 *
 * <p>Events are ordered first by due time and then by insertion sequence. No
 * background thread advances time, so a test has complete control over every
 * transition.
 */
public final class DeterministicScheduler {
    private static final int DEFAULT_MAX_TASKS_PER_RUN = 100_000;
    private static final Comparator<ScheduledTask> TASK_ORDER =
            Comparator.comparingLong((ScheduledTask task) -> task.dueNanos)
                    .thenComparingLong(task -> task.id);

    private final PriorityQueue<ScheduledTask> tasks = new PriorityQueue<>(TASK_ORDER);
    private final SimulationTrace trace;
    private final Thread ownerThread;

    private long nowNanos;
    private long nextTaskId;
    private int pendingTaskCount;

    /** Creates a scheduler with a new empty trace. */
    public DeterministicScheduler() {
        this(new SimulationTrace());
    }

    /**
     * Creates a scheduler that appends facts to the supplied trace.
     *
     * @param trace trace shared by simulation components
     */
    public DeterministicScheduler(SimulationTrace trace) {
        this.trace = Objects.requireNonNull(trace, "trace");
        ownerThread = Thread.currentThread();
    }

    /**
     * Returns the current logical time.
     *
     * @return non-negative, monotonic logical time
     */
    public Duration now() {
        verifyOwnerThread();
        return currentTime();
    }

    /**
     * Returns this scheduler's append-only trace.
     *
     * @return simulation trace
     */
    public SimulationTrace trace() {
        verifyOwnerThread();
        return trace;
    }

    /**
     * Returns the number of logically pending tasks.
     *
     * @return pending task count
     */
    public int pendingTaskCount() {
        verifyOwnerThread();
        return pendingTaskCount;
    }

    /**
     * Reports whether at least one task remains pending.
     *
     * @return true when pending work remains
     */
    public boolean hasPendingTasks() {
        return pendingTaskCount() > 0;
    }

    /**
     * Schedules an action relative to the current logical time.
     *
     * @param delay non-negative delay from the current logical time
     * @param label non-blank trace subject
     * @param action event action
     * @return handle that can cancel the event while it remains pending
     */
    public Cancellable schedule(Duration delay, String label, Runnable action) {
        verifyOwnerThread();
        var delayNanos = requireNonNegativeNanos(delay, "delay");
        var normalizedLabel = requireLabel(label);
        Objects.requireNonNull(action, "action");

        final long dueNanos;
        try {
            dueNanos = Math.addExact(nowNanos, delayNanos);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("scheduled time exceeds the logical-time range", overflow);
        }

        var task = new ScheduledTask(nextTaskId++, dueNanos, normalizedLabel, action);
        tasks.add(task);
        pendingTaskCount++;

        trace.record(
                currentTime(),
                TraceKind.TASK_SCHEDULED,
                task.label,
                Map.of(
                        "delayNanos", Long.toString(delayNanos),
                        "dueNanos", Long.toString(dueNanos),
                        "taskId", Long.toString(task.id)));
        return new ScheduledHandle(task);
    }

    /**
     * Executes the next pending event, advancing logical time to its due time.
     *
     * @return false when no pending event remains
     */
    public boolean runNext() {
        verifyOwnerThread();
        var task = pollNextPendingTask();
        if (task == null) {
            return false;
        }
        if (task.dueNanos < nowNanos) {
            throw new IllegalStateException("pending task exists in the logical-time past");
        }

        nowNanos = task.dueNanos;
        task.state = TaskState.EXECUTED;
        pendingTaskCount--;

        trace.record(
                currentTime(),
                TraceKind.TASK_STARTED,
                task.label,
                taskIdentity(task));
        try {
            task.action.run();
            trace.record(
                    currentTime(),
                    TraceKind.TASK_COMPLETED,
                    task.label,
                    taskIdentity(task));
            return true;
        } catch (RuntimeException | Error failure) {
            trace.record(
                    currentTime(),
                    TraceKind.TASK_FAILED,
                    task.label,
                    Map.of(
                            "exception", failure.getClass().getName(),
                            "message", Objects.toString(failure.getMessage(), ""),
                            "taskId", Long.toString(task.id)));
            throw failure;
        }
    }

    /**
     * Runs pending events until the queue is idle.
     *
     * <p>The task limit detects a zero-delay event that endlessly reschedules
     * itself.
     *
     * @return number of executed tasks
     */
    public int runUntilIdle() {
        return runUntilIdle(DEFAULT_MAX_TASKS_PER_RUN);
    }

    /**
     * Runs pending events until idle or the caller's task budget is exhausted.
     *
     * @param maxTasks maximum number of actions to execute
     * @return number of executed tasks
     */
    public int runUntilIdle(int maxTasks) {
        verifyOwnerThread();
        if (maxTasks <= 0) {
            throw new IllegalArgumentException("maxTasks must be positive");
        }

        var executed = 0;
        while (executed < maxTasks && runNext()) {
            executed++;
        }
        if (hasPendingTasks()) {
            throw new IllegalStateException(
                    "simulation still has pending work after executing " + maxTasks + " tasks");
        }
        return executed;
    }

    /**
     * Advances logical time by a non-negative duration, executing every event
     * due on or before the resulting time.
     *
     * @param duration non-negative amount of logical time
     * @return number of executed tasks
     */
    public int advanceBy(Duration duration) {
        verifyOwnerThread();
        var deltaNanos = requireNonNegativeNanos(duration, "duration");

        final long targetNanos;
        try {
            targetNanos = Math.addExact(nowNanos, deltaNanos);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("target exceeds the logical-time range", overflow);
        }
        return advanceToNanos(targetNanos);
    }

    /**
     * Advances to an absolute logical time and executes all events due by then.
     *
     * @param targetTime non-negative absolute logical time
     * @return number of executed tasks
     */
    public int advanceTo(Duration targetTime) {
        verifyOwnerThread();
        var targetNanos = requireNonNegativeNanos(targetTime, "targetTime");
        if (targetNanos < nowNanos) {
            throw new IllegalArgumentException("logical time cannot move backward");
        }
        return advanceToNanos(targetNanos);
    }

    private int advanceToNanos(long targetNanos) {
        var executed = 0;
        while (true) {
            var next = peekNextPendingTask();
            if (next == null || next.dueNanos > targetNanos) {
                break;
            }
            if (executed == DEFAULT_MAX_TASKS_PER_RUN) {
                throw new IllegalStateException(
                        "time advance exceeded " + DEFAULT_MAX_TASKS_PER_RUN + " executed tasks");
            }
            runNext();
            executed++;
        }

        if (nowNanos < targetNanos) {
            var previousNanos = nowNanos;
            nowNanos = targetNanos;
            trace.record(
                    currentTime(),
                    TraceKind.CLOCK_ADVANCED,
                    "logical-clock",
                    Map.of(
                            "fromNanos", Long.toString(previousNanos),
                            "toNanos", Long.toString(targetNanos)));
        }
        return executed;
    }

    private ScheduledTask pollNextPendingTask() {
        while (!tasks.isEmpty()) {
            var task = tasks.remove();
            if (task.state == TaskState.PENDING) {
                return task;
            }
        }
        return null;
    }

    private ScheduledTask peekNextPendingTask() {
        while (!tasks.isEmpty() && tasks.element().state != TaskState.PENDING) {
            tasks.remove();
        }
        return tasks.peek();
    }

    private Duration currentTime() {
        return Duration.ofNanos(nowNanos);
    }

    private void verifyOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                    "deterministic scheduler may only be used by its owning thread");
        }
    }

    private static long requireNonNegativeNanos(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException(name + " exceeds the logical-time range", overflow);
        }
    }

    private static String requireLabel(String label) {
        Objects.requireNonNull(label, "label");
        var normalized = label.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        return normalized;
    }

    private static Map<String, String> taskIdentity(ScheduledTask task) {
        return Map.of("taskId", Long.toString(task.id));
    }

    private enum TaskState {
        PENDING,
        CANCELLED,
        EXECUTED
    }

    private static final class ScheduledTask {
        private final long id;
        private final long dueNanos;
        private final String label;
        private final Runnable action;
        private TaskState state = TaskState.PENDING;

        private ScheduledTask(long id, long dueNanos, String label, Runnable action) {
            this.id = id;
            this.dueNanos = dueNanos;
            this.label = label;
            this.action = action;
        }
    }

    private final class ScheduledHandle implements Cancellable {
        private final ScheduledTask task;

        private ScheduledHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public boolean cancel() {
            verifyOwnerThread();
            if (task.state != TaskState.PENDING) {
                return false;
            }

            task.state = TaskState.CANCELLED;
            pendingTaskCount--;
            trace.record(
                    currentTime(),
                    TraceKind.TASK_CANCELLED,
                    task.label,
                    taskIdentity(task));
            return true;
        }

        @Override
        public boolean isCancelled() {
            verifyOwnerThread();
            return task.state == TaskState.CANCELLED;
        }

        @Override
        public boolean isDone() {
            verifyOwnerThread();
            return task.state != TaskState.PENDING;
        }
    }
}
