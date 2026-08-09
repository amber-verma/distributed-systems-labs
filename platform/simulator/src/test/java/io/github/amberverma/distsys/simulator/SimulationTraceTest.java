package io.github.amberverma.distsys.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimulationTraceTest {

    @Test
    void snapshotsAndAttributesAreImmutable() {
        var trace = new SimulationTrace();
        var mutableAttributes = new HashMap<String, String>();
        mutableAttributes.put("taskId", "7");

        trace.record(
                Duration.ofMillis(3),
                TraceKind.TASK_STARTED,
                "example",
                mutableAttributes);
        var snapshot = trace.events();
        mutableAttributes.put("taskId", "changed");

        assertEquals("7", snapshot.getFirst().attributes().get("taskId"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(snapshot.getFirst()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getFirst().attributes().put("extra", "value"));
    }

    @Test
    void assignsMonotonicSequencesAndFormatsAttributesDeterministically() {
        var trace = new SimulationTrace();
        trace.record(
                Duration.ZERO,
                TraceKind.TASK_SCHEDULED,
                "example",
                Map.of("zeta", "last", "alpha", "first"));
        trace.record(
                Duration.ofNanos(5),
                TraceKind.TASK_STARTED,
                "example",
                Map.of());

        assertEquals(0, trace.events().getFirst().sequence());
        assertEquals(1, trace.events().getLast().sequence());

        var formatted = trace.formatAsText();
        assertTrue(formatted.contains("{alpha=first, zeta=last}"));
        assertTrue(formatted.contains("#1 t=5ns TASK_STARTED example"));
    }
}
