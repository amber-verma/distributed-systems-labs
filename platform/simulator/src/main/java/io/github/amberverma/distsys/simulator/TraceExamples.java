package io.github.amberverma.distsys.simulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/** Exports the same named scenarios exercised by the tests. */
public final class TraceExamples {
    private TraceExamples() {
    }

    /**
     * Writes eight viewer inputs: a scheduler example and seven RPC histories.
     * @param args optional output directory; defaults to build/traces
     * @throws IOException if an output file cannot be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("usage: TraceExamples [output-directory]");
        }
        var directory = Path.of(args.length == 0 ? "build/traces" : args[0]);
        Files.createDirectories(directory);
        for (var scenario : RequestReplyScenario.Scenario.values()) {
            var result = RequestReplyScenario.run(scenario);
            var name = scenario.name().toLowerCase(Locale.ROOT).replace('_', '-');
            write(directory, name, TraceJson.format(result.events()));
        }
        var scheduler = new DeterministicScheduler();
        scheduler.schedule(Duration.ofMillis(8), "A", () -> { });
        var cancelled = scheduler.schedule(Duration.ofMillis(3), "B", () -> { });
        scheduler.schedule(Duration.ofMillis(3), "C", () -> { });
        cancelled.cancel();
        scheduler.runUntilIdle();
        write(directory, "scheduler-order", TraceJson.format(scheduler.trace().events()));
    }

    private static void write(Path directory, String name, String json) throws IOException {
        var output = directory.resolve(name + ".json");
        Files.writeString(output, json, StandardCharsets.UTF_8);
        System.out.println(output);
    }
}
