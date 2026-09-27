package io.github.amberverma.distsys.simulator;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executable teaching scenarios: one client appends "x" to one server.
 *
 * <p>The server deliberately has no deduplication. Client completion and
 * server-side effects are different properties; the next KV lab explores this.
 */
public final class RequestReplyScenario {
    /** Named fault schedules shared by tests and exported visual examples. */
    public enum Scenario {
        /** Request and reply arrive before the deadline. */
        SUCCESS,
        /** The request is lost, so the server does nothing. */
        LOST_REQUEST,
        /** The server acts, but its reply is lost. */
        LOST_REPLY,
        /** The reply arrives after the client has timed out. */
        LATE_REPLY,
        /** Two copies of a reply arrive before the deadline. */
        DUPLICATE_REPLY,
        /** Equal-time reply and deadline, with the reply inserted first. */
        REPLY_FIRST,
        /** Equal-time reply and deadline, with the deadline inserted first. */
        TIMEOUT_FIRST
    }

    /** A request can leave pending only once. */
    public enum Outcome {
        /** No terminal outcome has been observed. */
        PENDING,
        /** A qualifying reply arrived before the timeout action. */
        SUCCEEDED,
        /** The local deadline expired; the remote outcome remains unknown. */
        TIMED_OUT
    }

    /**
     * Immutable result of an entire scenario.
     * @param outcome client's terminal observation
     * @param serverValue actual server value, available to the test harness
     * @param completionCount number of client terminal transitions
     * @param events immutable trace snapshot
     */
    public record Result(Outcome outcome, String serverValue, int completionCount, List<TraceEvent> events) {
        /** Copies the trace for stable inspection. */
        public Result {
            events = List.copyOf(events);
        }
    }

    private RequestReplyScenario() {
    }

    /**
     * Runs a named scenario to completion without wall-clock waits.
     * @param scenario fault and ordering choices
     * @return client observation, actual server state, and trace
     */
    public static Result run(Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        var simulation = new Simulation(scenario);
        return simulation.run();
    }

    private static final class Simulation {
        private final DeterministicScheduler scheduler = new DeterministicScheduler();
        private final SimulatedNetwork network = new SimulatedNetwork(scheduler);
        private final Scenario scenario;
        private Outcome outcome = Outcome.PENDING;
        private String serverValue = "";
        private int completions;
        private Cancellable timeout;

        private Simulation(Scenario scenario) {
            this.scenario = scenario;
            network.register("client", this::receiveReply);
            network.register("server", this::receiveRequest);
        }

        private Result run() {
            recordState("client", outcome.name());
            recordState("server", serverValue);
            var race = scenario == Scenario.REPLY_FIRST || scenario == Scenario.TIMEOUT_FIRST;
            network.send(new Message("request-1", "client", "server", "x"),
                    race ? Duration.ZERO : Duration.ofMillis(5),
                    scenario == Scenario.LOST_REQUEST
                            ? SimulatedNetwork.Delivery.DROP : SimulatedNetwork.Delivery.DELIVER);

            if (scenario == Scenario.REPLY_FIRST) {
                // Deliver the zero-delay request before inserting the deadline.
                // Both histories still arm that deadline at logical time zero.
                scheduler.runNext();
            }
            timeout = scheduler.schedule(Duration.ofMillis(race ? 10 : 20), "client-deadline",
                    () -> finish(Outcome.TIMED_OUT, TraceKind.REQUEST_TIMED_OUT));
            scheduler.runUntilIdle();
            return new Result(outcome, serverValue, completions, scheduler.trace().events());
        }

        private void receiveRequest(Message request) {
            serverValue += request.payload();
            recordState("server", serverValue);
            var reply = new Message(request.requestId(), "server", "client", serverValue);
            var delay = switch (scenario) {
                case LATE_REPLY -> Duration.ofMillis(30);
                case REPLY_FIRST, TIMEOUT_FIRST -> Duration.ofMillis(10);
                default -> Duration.ofMillis(5);
            };
            network.send(reply, delay, scenario == Scenario.LOST_REPLY
                    ? SimulatedNetwork.Delivery.DROP : SimulatedNetwork.Delivery.DELIVER);
            if (scenario == Scenario.DUPLICATE_REPLY) {
                network.send(reply, delay.plusMillis(1), SimulatedNetwork.Delivery.DELIVER);
            }
        }

        private void receiveReply(Message reply) {
            if (outcome != Outcome.PENDING || !reply.requestId().equals("request-1")) {
                scheduler.trace().record(scheduler.now(), TraceKind.REPLY_IGNORED, "client",
                        Map.of("requestId", reply.requestId(), "outcome", outcome.name()));
                return;
            }
            finish(Outcome.SUCCEEDED, TraceKind.REQUEST_COMPLETED);
            timeout.cancel();
        }

        private void finish(Outcome terminal, TraceKind kind) {
            if (outcome != Outcome.PENDING) {
                return;
            }
            outcome = terminal;
            completions++;
            scheduler.trace().record(scheduler.now(), kind, "client",
                    Map.of("requestId", "request-1", "outcome", outcome.name()));
            recordState("client", outcome.name());
        }

        private void recordState(String node, String value) {
            scheduler.trace().record(scheduler.now(), TraceKind.STATE_CHANGED, node,
                    Map.of("node", node, "value", value));
        }
    }
}
