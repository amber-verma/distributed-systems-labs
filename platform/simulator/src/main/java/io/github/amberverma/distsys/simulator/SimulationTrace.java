package io.github.amberverma.distsys.simulator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Append-only event history for one simulation.
 */
public final class SimulationTrace {
    /** Current schema version assigned to newly recorded events. */
    public static final int SCHEMA_VERSION = 1;

    private final List<TraceEvent> events = new ArrayList<>();
    private long nextSequence;

    /** Creates an empty trace. */
    public SimulationTrace() {
        // The explicit constructor documents the public lifecycle.
    }

    /**
     * Appends a fact and assigns its monotonic sequence.
     *
     * @param logicalTime current simulation time
     * @param kind fact category
     * @param subject human-readable event subject
     * @param attributes structured fact details
     * @return the immutable appended event
     */
    public TraceEvent record(
            Duration logicalTime,
            TraceKind kind,
            String subject,
            Map<String, String> attributes) {
        var event = new TraceEvent(
                SCHEMA_VERSION,
                nextSequence++,
                logicalTime,
                kind,
                subject,
                attributes);
        events.add(event);
        return event;
    }

    /**
     * Returns an immutable snapshot. Later records do not modify the snapshot.
     *
     * @return immutable event snapshot
     */
    public List<TraceEvent> events() {
        return List.copyOf(events);
    }

    /**
     * Reports whether no facts have been recorded.
     *
     * @return true when the trace is empty
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Produces stable, line-oriented text suitable for teaching and test output.
     *
     * @return formatted event history
     */
    public String formatAsText() {
        var output = new StringBuilder();
        for (var event : events) {
            output.append('#')
                    .append(event.sequence())
                    .append(" t=")
                    .append(event.logicalTime().toNanos())
                    .append("ns ")
                    .append(event.kind())
                    .append(' ')
                    .append(event.subject());

            if (!event.attributes().isEmpty()) {
                output.append(' ').append(formatAttributes(event.attributes()));
            }
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    private static String formatAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes");
        var sorted = new TreeMap<>(attributes);
        var joiner = new StringJoiner(", ", "{", "}");
        sorted.forEach((key, value) -> joiner.add(key + '=' + value));
        return joiner.toString();
    }
}
