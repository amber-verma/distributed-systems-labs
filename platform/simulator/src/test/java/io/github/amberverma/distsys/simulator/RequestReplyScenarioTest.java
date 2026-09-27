package io.github.amberverma.distsys.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RequestReplyScenarioTest {
    /**
     * JUnit invokes this test once per scenario; "first" and "second" run that
     * same scenario twice. Checks one client completion and an identical,
     * chronologically ordered trace on both runs.
     * Use case: inspect request/reply delivery, loss, duplication, and timeout
     * races directly in the test output without opening the trace viewer.
     */
    @ParameterizedTest
    @EnumSource(RequestReplyScenario.Scenario.class)
    void everyScenarioCompletesOnceAndIsExactlyRepeatable(RequestReplyScenario.Scenario scenario) {
        var first = RequestReplyScenario.run(scenario);
        var second = RequestReplyScenario.run(scenario);
        var firstJson = TraceJson.format(first.events());
        var secondJson = TraceJson.format(second.events());
        logScenario(scenario, first, second, firstJson.equals(secondJson));

        assertEquals(first, second);
        assertEquals(firstJson, secondJson);
        assertEquals(1, first.completionCount());
        assertEquals(1, first.events().stream()
                .filter(e -> e.kind() == TraceKind.REQUEST_COMPLETED || e.kind() == TraceKind.REQUEST_TIMED_OUT)
                .count());
        for (var i = 0; i < first.events().size(); i++) {
            assertEquals(i, first.events().get(i).sequence());
            if (i > 0) {
                assertTrue(first.events().get(i).logicalTime()
                        .compareTo(first.events().get(i - 1).logicalTime()) >= 0);
            }
        }
    }

    /**
     * A reply before the deadline succeeds and cancels the pending timeout.
     * Use case: verify the normal request, server append, and reply path.
     */
    @Test
    void successfulReplyCancelsTheDeadline() {
        var result = RequestReplyScenario.run(RequestReplyScenario.Scenario.SUCCESS);
        var deadlineCancelled = result.events().stream().anyMatch(e -> e.kind() == TraceKind.TASK_CANCELLED);
        var timedOut = result.events().stream().anyMatch(e -> e.kind() == TraceKind.REQUEST_TIMED_OUT);
        logTest("Successful reply cancels the deadline",
                "The client succeeds when the reply arrives and cancels its pending deadline.",
                "Follow a successful append without waiting for the cancelled timeout.",
                "deadline cancelled=" + deadlineCancelled + ", timeout event=" + timedOut,
                new NamedRun("SUCCESS", result));

        assertEquals(RequestReplyScenario.Outcome.SUCCEEDED, result.outcome());
        assertEquals("x", result.serverValue());
        assertTrue(deadlineCancelled);
        assertFalse(timedOut);
    }

    /**
     * Both losses time out with the same client observations, even though only
     * the lost-reply case changes server state.
     * Use case: show why a client timeout cannot reveal the remote outcome.
     */
    @Test
    void lostRequestAndLostReplyAreIndistinguishableToTheClient() {
        var lostRequest = RequestReplyScenario.run(RequestReplyScenario.Scenario.LOST_REQUEST);
        var lostReply = RequestReplyScenario.run(RequestReplyScenario.Scenario.LOST_REPLY);
        var requestObservations = clientObservations(lostRequest);
        var replyObservations = clientObservations(lostReply);
        logTest("Lost request and lost reply look the same to the client",
                "Both clients time out, but only the lost reply lets the server append x.",
                "Compare the client's limited view with the actual server state.",
                "client observations identical=" + requestObservations.equals(replyObservations),
                new NamedRun("LOST_REQUEST", lostRequest), new NamedRun("LOST_REPLY", lostReply));

        assertEquals(RequestReplyScenario.Outcome.TIMED_OUT, lostRequest.outcome());
        assertEquals(lostRequest.outcome(), lostReply.outcome());
        assertEquals("", lostRequest.serverValue());
        assertEquals("x", lostReply.serverValue());
        assertEquals(requestObservations, replyObservations);
    }

    /**
     * A reply delivered after the deadline is ignored, leaving the timeout
     * terminal even though the server already appended x.
     * Use case: verify that a late network response cannot reverse a timeout.
     */
    @Test
    void lateReplyCannotChangeATimeoutIntoSuccess() {
        var result = RequestReplyScenario.run(RequestReplyScenario.Scenario.LATE_REPLY);
        var ignoredReplies = result.events().stream().filter(e -> e.kind() == TraceKind.REPLY_IGNORED).count();
        logTest("Late reply cannot change a timeout into success",
                "The deadline fires first; the later reply is delivered but ignored.",
                "Check that client state stays timed out after a delayed response.",
                "ignored replies=" + ignoredReplies,
                new NamedRun("LATE_REPLY", result));

        assertEquals(RequestReplyScenario.Outcome.TIMED_OUT, result.outcome());
        assertEquals("x", result.serverValue());
        assertEquals(1, ignoredReplies);
    }

    /**
     * The first matching reply succeeds; a repeated reply is ignored rather
     * than completing the same client request again.
     * Use case: inspect duplicate delivery without a second client completion.
     */
    @Test
    void duplicateReplyDoesNotCompleteTwice() {
        var result = RequestReplyScenario.run(RequestReplyScenario.Scenario.DUPLICATE_REPLY);
        var ignoredReplies = result.events().stream().filter(e -> e.kind() == TraceKind.REPLY_IGNORED).count();
        logTest("Duplicate reply does not complete twice",
                "The first reply succeeds; the second arrives after completion and is ignored.",
                "Check client completion when the network delivers a duplicate reply.",
                "ignored replies=" + ignoredReplies,
                new NamedRun("DUPLICATE_REPLY", result));

        assertEquals(RequestReplyScenario.Outcome.SUCCEEDED, result.outcome());
        assertEquals(1, result.completionCount());
        assertEquals(1, ignoredReplies);
    }

    /**
     * Reply and deadline can both be due at 10 ms. Their insertion order picks
     * different client outcomes while both runs still finish once.
     * Use case: examine a deterministic tie between delivery and timeout.
     */
    @Test
    void bothEqualTimeOrdersAreSafeButHaveDifferentClientOutcomes() {
        var replyFirst = RequestReplyScenario.run(RequestReplyScenario.Scenario.REPLY_FIRST);
        var timeoutFirst = RequestReplyScenario.run(RequestReplyScenario.Scenario.TIMEOUT_FIRST);
        var replyFirstCompletion = terminalEvent(replyFirst);
        var timeoutFirstCompletion = terminalEvent(timeoutFirst);
        logTest("Equal-time reply and deadline have two safe orders",
                "Both actions are due at 10 ms; the task inserted first decides the client outcome.",
                "Compare a reply/deadline race without depending on wall-clock timing.",
                "reply-first terminal=" + replyFirstCompletion.kind() + " at "
                        + replyFirstCompletion.logicalTime().toMillis() + " ms"
                        + ", timeout-first terminal=" + timeoutFirstCompletion.kind() + " at "
                        + timeoutFirstCompletion.logicalTime().toMillis() + " ms",
                new NamedRun("REPLY_FIRST", replyFirst), new NamedRun("TIMEOUT_FIRST", timeoutFirst));

        assertEquals(RequestReplyScenario.Outcome.SUCCEEDED, replyFirst.outcome());
        assertEquals(RequestReplyScenario.Outcome.TIMED_OUT, timeoutFirst.outcome());
        assertEquals("x", replyFirst.serverValue());
        assertEquals(replyFirst.serverValue(), timeoutFirst.serverValue());
        assertEquals(java.time.Duration.ofMillis(10), replyFirstCompletion.logicalTime());
        assertEquals(java.time.Duration.ofMillis(10), timeoutFirstCompletion.logicalTime());
    }

    private static TraceEvent terminalEvent(RequestReplyScenario.Result result) {
        return result.events().stream()
                .filter(e -> e.kind() == TraceKind.REQUEST_COMPLETED || e.kind() == TraceKind.REQUEST_TIMED_OUT)
                .findFirst().orElseThrow();
    }

    private static List<String> clientObservations(RequestReplyScenario.Result result) {
        return result.events().stream()
                .filter(e -> e.kind() == TraceKind.REQUEST_COMPLETED || e.kind() == TraceKind.REQUEST_TIMED_OUT
                        || (e.kind() == TraceKind.MESSAGE_SENT && "client".equals(e.attributes().get("source")))
                        || (e.kind() == TraceKind.MESSAGE_DELIVERED && "client".equals(e.attributes().get("destination")))
                        || (e.kind() == TraceKind.STATE_CHANGED && e.subject().equals("client")))
                .map(e -> e.logicalTime() + " " + e.kind() + " " + new java.util.TreeMap<>(e.attributes()))
                .toList();
    }

    /** Prints the scenario's purpose, first trace, final state, and repeat-run check. */
    private static void logScenario(
            RequestReplyScenario.Scenario scenario,
            RequestReplyScenario.Result first,
            RequestReplyScenario.Result second,
            boolean sameJson) {
        var notes = switch (scenario) {
            case SUCCESS -> new ScenarioNotes(
                    "The reply arrives before the deadline and cancels it.",
                    "Follow a successful request from send through server append to client completion.");
            case LOST_REQUEST -> new ScenarioNotes(
                    "The request is dropped; the server stays unchanged and the client times out.",
                    "See what a client observes when its request never reaches the server.");
            case LOST_REPLY -> new ScenarioNotes(
                    "The server appends x, but its dropped reply leaves the client timed out.",
                    "See why a timeout cannot tell the client whether server work happened.");
            case LATE_REPLY -> new ScenarioNotes(
                    "The deadline fires before the reply, so the client ignores that reply.",
                    "Check that a late response cannot reverse a timeout.");
            case DUPLICATE_REPLY -> new ScenarioNotes(
                    "The first reply completes the client; the second is ignored.",
                    "Check that duplicate delivery does not complete the client twice.");
            case REPLY_FIRST -> new ScenarioNotes(
                    "Reply and deadline are due at 10 ms; the reply was inserted first.",
                    "Inspect the successful side of an equal-time race.");
            case TIMEOUT_FIRST -> new ScenarioNotes(
                    "Reply and deadline are due at 10 ms; the deadline was inserted first.",
                    "Inspect the timed-out side of an equal-time race.");
        };

        System.out.println();
        System.out.println("=== " + scenario + " ===");
        System.out.println("What it shows: " + notes.explanation());
        System.out.println("Use case: " + notes.useCase());
        System.out.println("Trace (first run):");
        printTrace(first.events());
        System.out.printf("Outcome: client=%s, server value=\"%s\", client completions=%d%n",
                first.outcome(), first.serverValue(), first.completionCount());
        System.out.printf("Repeat run: result identical=%s, JSON trace identical=%s%n",
                first.equals(second), sameJson);
        if (!first.equals(second) || !sameJson) {
            System.out.println("Trace (second run):");
            printTrace(second.events());
        }
    }

    /** Prints the purpose, all compared traces, outcomes, and key observation. */
    private static void logTest(
            String title,
            String explanation,
            String useCase,
            String observation,
            NamedRun... runs) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.println("What it shows: " + explanation);
        System.out.println("Use case: " + useCase);
        for (var run : runs) {
            System.out.println("Trace (" + run.label() + "):");
            printTrace(run.result().events());
            System.out.printf("Outcome (%s): client=%s, server value=\"%s\", client completions=%d%n",
                    run.label(), run.result().outcome(), run.result().serverValue(), run.result().completionCount());
        }
        System.out.println("Observed: " + observation);
    }

    private static void printTrace(List<TraceEvent> events) {
        var formattedTrace = SimulationTrace.formatAsText(events);
        if (formattedTrace.isEmpty()) {
            System.out.println("(no trace events)");
        } else {
            System.out.print(formattedTrace);
        }
    }

    private record ScenarioNotes(String explanation, String useCase) {
    }

    private record NamedRun(String label, RequestReplyScenario.Result result) {
    }
}
