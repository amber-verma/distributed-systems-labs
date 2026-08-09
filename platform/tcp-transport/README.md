# TCP Transport

**Status:** Planned

The TCP transport is a capstone adapter after simulated RPC is understood. It
will expose real connection lifecycle and wire behavior without introducing a
framework.

## Planned behavior

- Virtual thread per accepted connection.
- Explicit versioned message codec.
- Length-prefixed framing.
- Partial-read and partial-write handling.
- Request deadlines.
- Connection close and reconnect behavior.
- Graceful shutdown and bounded resources.

## Non-goals

The adapter is not a production RPC framework. TLS, authentication, service
discovery, load balancing, and cross-language schema generation remain outside
the core curriculum.
