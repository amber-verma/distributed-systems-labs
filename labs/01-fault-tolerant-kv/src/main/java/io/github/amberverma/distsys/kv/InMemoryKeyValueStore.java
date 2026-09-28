package io.github.amberverma.distsys.kv;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link KeyValueStore} for sequential use on one thread.
 */
public final class InMemoryKeyValueStore implements KeyValueStore {
    private final Map<String, String> kvStore;

    /** Creates a new, empty, independent store. */
    public InMemoryKeyValueStore() {
        kvStore = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(kvStore.get(key));
    }

    /** {@inheritDoc} */
    @Override
    public void put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        kvStore.put(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public String append(String key, String suffix) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(suffix, "suffix");
        String updatedValue = kvStore.getOrDefault(key, "").concat(suffix);
        kvStore.put(key, updatedValue);
        return updatedValue;
    }

    /** {@inheritDoc} */
    @Override
    public boolean delete(String key) {
        Objects.requireNonNull(key, "key");
        return kvStore.remove(key) != null;
    }
}
