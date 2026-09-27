package io.github.amberverma.distsys.simulator;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/** Dependency-free JSON export of recorded facts for the offline viewer. */
public final class TraceJson {
    private TraceJson() {
    }

    /**
     * Serializes a snapshot in stable order. Times use decimal strings to avoid
     * losing nanoseconds in JavaScript numbers. This is a history, not a program
     * that can recreate arbitrary Runnable actions.
     *
     * @param events trace facts in sequence order
     * @return schema-versioned JSON, with no platform-specific line endings
     */
    public static String format(List<TraceEvent> events) {
        var json = new StringJoiner(",\n", "{\"schemaVersion\":1,\"events\":[\n", "\n]}\n");
        for (var event : events) {
            var nanos = BigInteger.valueOf(event.logicalTime().getSeconds())
                    .multiply(BigInteger.valueOf(1_000_000_000))
                    .add(BigInteger.valueOf(event.logicalTime().getNano()));
            var attributes = new StringJoiner(",", "{", "}");
            for (Map.Entry<String, String> attribute : new TreeMap<>(event.attributes()).entrySet()) {
                attributes.add(quote(attribute.getKey()) + ":" + quote(attribute.getValue()));
            }
            json.add("{\"schemaVersion\":" + event.schemaVersion()
                    + ",\"sequence\":" + event.sequence()
                    + ",\"logicalTimeNanos\":" + quote(nanos.toString())
                    + ",\"kind\":" + quote(event.kind().name())
                    + ",\"subject\":" + quote(event.subject())
                    + ",\"attributes\":" + attributes + "}");
        }
        return json.toString();
    }

    private static String quote(String value) {
        var json = new StringBuilder("\"");
        for (var i = 0; i < value.length(); i++) {
            var character = value.charAt(i);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20 || Character.isSurrogate(character)) {
                        json.append("\\u");
                        var hex = Integer.toHexString(character);
                        json.append("0".repeat(4 - hex.length())).append(hex);
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}
