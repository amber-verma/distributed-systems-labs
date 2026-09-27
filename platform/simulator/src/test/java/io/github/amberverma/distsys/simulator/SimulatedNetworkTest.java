package io.github.amberverma.distsys.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulatedNetworkTest {
    @Test
    void testControlsReorderingLossAndRepeatedTransmission() {
        var scheduler = new DeterministicScheduler();
        var network = new SimulatedNetwork(scheduler);
        var received = new ArrayList<String>();
        network.register("client", message -> { });
        network.register("server", message -> received.add(message.payload()));
        var repeated = new Message("same-request", "client", "server", "fast");
        network.send(new Message("slow-request", "client", "server", "slow"),
                Duration.ofMillis(9), SimulatedNetwork.Delivery.DELIVER);
        network.send(repeated, Duration.ofMillis(2), SimulatedNetwork.Delivery.DELIVER);
        network.send(repeated, Duration.ofMillis(3), SimulatedNetwork.Delivery.DELIVER);
        network.send(new Message("lost-request", "client", "server", "lost"),
                Duration.ofMillis(1), SimulatedNetwork.Delivery.DROP);

        assertEquals(List.of(), received); // No hidden background work.
        scheduler.runUntilIdle();
        assertEquals(List.of("fast", "fast", "slow"), received);
        assertEquals(1, scheduler.trace().events().stream()
                .filter(e -> e.kind() == TraceKind.MESSAGE_DROPPED).count());
    }

    @Test
    void unknownEndpointIsAConfigurationErrorRatherThanAnImplicitNetworkFault() {
        var scheduler = new DeterministicScheduler();
        var network = new SimulatedNetwork(scheduler);
        network.register("client", message -> { });
        assertThrows(IllegalArgumentException.class, () -> network.send(
                new Message("request", "client", "missing", "x"),
                Duration.ZERO, SimulatedNetwork.Delivery.DELIVER));
        assertEquals(0, scheduler.pendingTaskCount());
        assertEquals(List.of(), scheduler.trace().events());
    }
}
