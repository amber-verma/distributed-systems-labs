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
    void rejectedRecordDoesNotConsumeASequenceNumber() {
        var trace = new SimulationTrace();
        trace.record(Duration.ZERO, TraceKind.TASK_STARTED, "first", Map.of());
        assertThrows(IllegalArgumentException.class,
                () -> trace.record(Duration.ZERO, TraceKind.TASK_STARTED, " ", Map.of()));
        trace.record(Duration.ZERO, TraceKind.TASK_COMPLETED, "second", Map.of());

        assertEquals(java.util.List.of(0L, 1L),
                trace.events().stream().map(TraceEvent::sequence).toList());
    }

    /**
     * Recording copies the caller's attributes, and events() returns a snapshot
     * that cannot be edited or changed by later records.
     * Use case: a trace viewer can keep a stable view while a simulation continues.
     */
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
        trace.record(
                Duration.ofMillis(4),
                TraceKind.TASK_COMPLETED,
                "example",
                Map.of("taskId", "7"));

        assertEquals("7", snapshot.getFirst().attributes().get("taskId"));
        assertEquals(1, snapshot.size());
        assertEquals(2, trace.events().size());
        var listMutation = assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(snapshot.getFirst()));
        var attributeMutation = assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getFirst().attributes().put("extra", "value"));
        logScenario("Immutable trace snapshots and attributes",
                "Changing the source map or recording another fact does not change the earlier snapshot.",
                "Keep an earlier trace view stable while a simulation continues.", trace,
                "source taskId=" + mutableAttributes.get("taskId")
                        + ", snapshot taskId=" + snapshot.getFirst().attributes().get("taskId")
                        + ", snapshot events=" + snapshot.size()
                        + ", current events=" + trace.events().size()
                        + ", list edit=" + listMutation.getClass().getSimpleName()
                        + ", attribute edit=" + attributeMutation.getClass().getSimpleName());
    }

    /**
     * Each recorded fact gets the next sequence number. Text formatting sorts
     * attributes and renders nanosecond durations as readable seconds.
     * Use case: compare or replay protocol histories with stable event order.
     */
    @Test
    void assignsMonotonicSequencesAndFormatsAttributesDeterministically() {
        var trace = new SimulationTrace();
        trace.record(
                Duration.ZERO,
                TraceKind.TASK_SCHEDULED,
                "example",
                Map.of(
                        "zeta", "last",
                        "alpha", "first",
                        "delayNanos", "1500000000",
                        "dueNanos", "1500000000"));
        trace.record(
                Duration.ofMillis(1500),
                TraceKind.TASK_STARTED,
                "example",
                Map.of());

        assertEquals(0, trace.events().getFirst().sequence());
        assertEquals(1, trace.events().getLast().sequence());

        var formatted = trace.formatAsText();
        logScenario("Stable sequence numbers and readable formatting",
                "Events receive #0 and #1; attributes are sorted and durations display in seconds.",
                "Compare protocol traces across repeatable simulation runs.", trace,
                "sequences=" + trace.events().stream().map(TraceEvent::sequence).toList());
        assertTrue(formatted.contains("{alpha=first, delaySeconds=1.5s, dueSeconds=1.5s, zeta=last}"));
        assertTrue(formatted.contains("#1 t=1.5s TASK_STARTED example"));
    }

    /** Prints each scenario's behavior, motivation, trace, and observed outcome. */
    private static void logScenario(
            String scenario,
            String explanation,
            String useCase,
            SimulationTrace trace,
            String outcome) {
        System.out.println();
        System.out.println("=== " + scenario + " ===");
        System.out.println("What it shows: " + explanation);
        System.out.println("Use case: " + useCase);
        System.out.println("Trace:");
        System.out.print(trace.formatAsText());
        System.out.println("Outcome: " + outcome);
    }
}
