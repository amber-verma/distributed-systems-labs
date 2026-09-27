package io.github.amberverma.distsys.simulator;

/**
 * The kinds of facts recorded during a deterministic simulation.
 *
 * <p>Scheduler, message, and application observations share one ordered history.
 */
public enum TraceKind {
    /** An event entered the scheduler's pending queue. */
    TASK_SCHEDULED,
    /** A pending event was cancelled. */
    TASK_CANCELLED,
    /** The scheduler selected an event and began its action. */
    TASK_STARTED,
    /** An event action returned normally. */
    TASK_COMPLETED,
    /** An event action terminated with an exception or error. */
    TASK_FAILED,
    /** Logical time moved without selecting an event at the target time. */
    CLOCK_ADVANCED,
    /** A transmission entered the network. */
    MESSAGE_SENT,
    /** A transmission reached its destination handler. */
    MESSAGE_DELIVERED,
    /** A scripted fault discarded a transmission. */
    MESSAGE_DROPPED,
    /** A client received a qualifying reply. */
    REQUEST_COMPLETED,
    /** A client deadline expired with an unknown remote outcome. */
    REQUEST_TIMED_OUT,
    /** A reply could no longer complete its request. */
    REPLY_IGNORED,
    /** An application recorded an inspectable state value. */
    STATE_CHANGED
}
