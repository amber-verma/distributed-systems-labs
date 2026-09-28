package io.github.amberverma.distsys.kv;

import java.util.Optional;

/**
 * Contract for a local, in-memory store of string keys and string values.
 *
 * <p>Each new store is empty and independent of every other instance. Calls are
 * sequential on one thread; thread safety and persistence are not required.
 * Keys and values are used exactly as supplied: empty strings, whitespace, and
 * Unicode are valid, and keys are case-sensitive. No trimming or normalization
 * is performed.
 *
 * <p>Every null argument must cause {@link NullPointerException} without changing
 * any stored state. Exception messages are not part of this contract. Operations
 * on one key leave all other keys unchanged.
 */
public interface KeyValueStore {
    /**
     * Reads a key without changing the store.
     *
     * <p>A missing key returns {@link Optional#empty()}; a key storing the empty
     * string returns {@code Optional.of("")}. The result itself is never null.
     *
     * @param key exact key to read; must not be null
     * @return the current value if the key exists, otherwise an empty optional
     * @throws NullPointerException if key is null
     */
    Optional<String> get(String key);

    /**
     * Creates a key or replaces its entire existing value.
     *
     * <p>Storing the empty string creates a present key; it does not delete it.
     *
     * @param key exact key to write; must not be null
     * @param value replacement value; must not be null
     * @throws NullPointerException if key or value is null
     */
    void put(String key, String value);

    /**
     * Appends a suffix to the current value and returns the full updated value.
     *
     * <p>A missing key is treated as having an empty value and is created by this
     * operation. An empty suffix is valid: it leaves an existing value unchanged,
     * or creates a missing key with the empty string. Each call applies its suffix
     * again, including repeated calls with identical arguments.
     *
     * @param key exact key to update; must not be null
     * @param suffix text to concatenate after the current value; must not be null
     * @return the full value stored after this append; never null
     * @throws NullPointerException if key or suffix is null
     */
    String append(String key, String suffix);

    /**
     * Removes a key and its value, if present.
     *
     * <p>A key storing the empty string still counts as present. After deletion,
     * {@link #get(String)} returns an empty optional for that key. Deleting a
     * missing key returns false without changing the store. A later put or append
     * may create the key again using its usual missing-key behavior.
     *
     * @param key exact key to remove; must not be null
     * @return true if the key existed and was removed, otherwise false
     * @throws NullPointerException if key is null
     */
    boolean delete(String key);
}
