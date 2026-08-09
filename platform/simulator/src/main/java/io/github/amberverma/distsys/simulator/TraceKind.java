package io.github.amberverma.distsys.simulator;

/**
 * The kinds of facts recorded during a deterministic simulation.
 *
 * <p>The initial set describes scheduler behavior. Later Phase 0 checkpoints
 * will add network, crash, restart, and partition facts to the same trace.
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
    CLOCK_ADVANCED
}
