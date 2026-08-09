package io.github.amberverma.distsys.simulator;

/**
 * Handle for a scheduled simulation event.
 *
 * <p>Like the scheduler itself, a handle may be used only by the scheduler's
 * owning thread.
 */
public interface Cancellable {
    /**
     * Cancels a pending event.
     *
     * @return true only when this call changed the event from pending to
     *         cancelled
     */
    boolean cancel();

    /**
     * Reports whether cancellation made this event terminal.
     *
     * @return true only for a cancelled event
     */
    boolean isCancelled();

    /**
     * Reports whether this event is cancelled or executed.
     *
     * @return false only while the event remains pending
     */
    boolean isDone();
}
