package io.github.amberverma.distsys.simulator;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable fact observed during a simulation.
 *
 * @param schemaVersion version of the trace schema
 * @param sequence monotonic event sequence within one trace
 * @param logicalTime simulated time at which the fact was recorded
 * @param kind category of fact
 * @param subject human-readable event subject
 * @param attributes structured details used by tests and visualizers
 */
public record TraceEvent(
        int schemaVersion,
        long sequence,
        Duration logicalTime,
        TraceKind kind,
        String subject,
        Map<String, String> attributes) {

    /**
     * Validates scalar values and defensively copies the attribute map.
     */
    public TraceEvent {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }

        Objects.requireNonNull(logicalTime, "logicalTime");
        if (logicalTime.isNegative()) {
            throw new IllegalArgumentException("logicalTime must not be negative");
        }

        Objects.requireNonNull(kind, "kind");
        subject = requireText(subject, "subject");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        var normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
