package io.github.amberverma.distsys.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TraceJsonTest {
    @Test
    void escapesTextAndPreservesTimeWithoutJavascriptRounding() {
        var trace = new SimulationTrace();
        trace.record(Duration.ofSeconds(9_007_200, 1), TraceKind.STATE_CHANGED,
                "quote\" slash\\ newline\nend", Map.of("value", "\t\u0000😀"));
        var json = TraceJson.format(trace.events());
        assertTrue(json.contains("\"logicalTimeNanos\":\"9007200000000001\""));
        assertTrue(json.contains("\"subject\":\"quote\\\" slash\\\\ newline\\nend\""));
        assertTrue(json.contains("\"value\":\"\\t\\u0000\\ud83d\\ude00\""));
    }

    @Test
    void attributeInsertionOrderDoesNotChangeTheExport() {
        var first = new SimulationTrace();
        var second = new SimulationTrace();
        var attributes = new java.util.LinkedHashMap<String, String>();
        attributes.put("z", "last");
        attributes.put("a", "first");
        first.record(Duration.ZERO, TraceKind.STATE_CHANGED, "node", attributes);
        second.record(Duration.ZERO, TraceKind.STATE_CHANGED, "node", Map.of("a", "first", "z", "last"));
        assertEquals(TraceJson.format(first.events()), TraceJson.format(second.events()));
        assertEquals("{\"schemaVersion\":1,\"events\":[\n\n]}\n", TraceJson.format(java.util.List.of()));
    }
}
