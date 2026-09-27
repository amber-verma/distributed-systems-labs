package io.github.amberverma.distsys.simulator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Scripted message delivery on one deterministic scheduler.
 *
 * <p>Endpoints and handlers belong to the scheduler's owning thread. Each send
 * represents one transmission; sending the same message again models duplication.
 * There are no sockets, background threads, crashes, or automatic retries.
 */
public final class SimulatedNetwork {
    /** The test's decision for a particular transmission. */
    public enum Delivery {
        /** Invoke the destination handler after the chosen delay. */
        DELIVER,
        /** Record loss after the chosen delay without invoking the handler. */
        DROP
    }

    private final DeterministicScheduler scheduler;
    private final Map<String, Consumer<Message>> endpoints = new HashMap<>();
    private long nextMessageId;

    /**
     * Creates a network using the scheduler's clock and trace.
     * @param scheduler simulation event loop
     */
    public SimulatedNetwork(DeterministicScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /**
     * Registers a unique endpoint.
     * @param endpoint non-blank endpoint identity
     * @param handler action to run on delivery
     */
    public void register(String endpoint, Consumer<Message> handler) {
        scheduler.now(); // Enforce ownership before modifying network state.
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(handler, "handler");
        if (endpoint.isBlank() || endpoints.containsKey(endpoint)) {
            throw new IllegalArgumentException("endpoint must be non-blank and unique");
        }
        endpoints.put(endpoint, handler);
    }

    /**
     * Schedules one independently controlled transmission.
     * @param message immutable request or reply
     * @param delay non-negative delivery delay
     * @param delivery scripted delivery or loss decision
     */
    public void send(Message message, Duration delay, Delivery delivery) {
        scheduler.now();
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(delay, "delay");
        Objects.requireNonNull(delivery, "delivery");
        if (!endpoints.containsKey(message.source()) || !endpoints.containsKey(message.destination())) {
            throw new IllegalArgumentException("both message endpoints must be registered");
        }
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        try {
            Math.addExact(scheduler.now().toNanos(), delay.toNanos());
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("delivery exceeds logical-time range", overflow);
        }

        var attributes = Map.of(
                "messageId", Long.toString(nextMessageId++),
                "requestId", message.requestId(),
                "source", message.source(),
                "destination", message.destination(),
                "payload", message.payload());
        scheduler.trace().record(scheduler.now(), TraceKind.MESSAGE_SENT, message.requestId(), attributes);
        scheduler.schedule(delay, "deliver-" + attributes.get("messageId"), () -> {
            var kind = delivery == Delivery.DROP
                    ? TraceKind.MESSAGE_DROPPED : TraceKind.MESSAGE_DELIVERED;
            scheduler.trace().record(scheduler.now(), kind, message.requestId(), attributes);
            if (delivery == Delivery.DELIVER) {
                endpoints.get(message.destination()).accept(message);
            }
        });
    }
}
