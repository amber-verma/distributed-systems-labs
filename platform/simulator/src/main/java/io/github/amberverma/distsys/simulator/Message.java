package io.github.amberverma.distsys.simulator;

import java.util.Objects;

/**
 * Immutable request or reply. A reply carries the original request identity.
 *
 * @param requestId identity of the logical request
 * @param source sending endpoint
 * @param destination receiving endpoint
 * @param payload immutable application data
 */
public record Message(String requestId, String source, String destination, String payload) {
    /** Validates identities and the payload. */
    public Message {
        requestId = requireIdentity(requestId);
        source = requireIdentity(source);
        destination = requireIdentity(destination);
        Objects.requireNonNull(payload, "payload");
    }

    private static String requireIdentity(String value) {
        Objects.requireNonNull(value, "identity");
        if (value.isBlank()) {
            throw new IllegalArgumentException("identity must not be blank");
        }
        return value;
    }
}
