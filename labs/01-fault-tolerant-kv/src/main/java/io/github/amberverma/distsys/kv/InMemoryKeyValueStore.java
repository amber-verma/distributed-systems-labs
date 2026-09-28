package io.github.amberverma.distsys.kv;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Your implementation of the local KV coding exercise.
 *
 * <p>Implement the {@link KeyValueStore} contract in this file. You may add private
 * fields and helpers, but keep the public constructor and method signatures.
 */
public final class InMemoryKeyValueStore implements KeyValueStore {
    /** Creates a new, empty, independent store. */

    Map<String, String> kvStore = new HashMap<>();

    public InMemoryKeyValueStore() {
//        kvStore = new HashMap<>();
        // TODO: Add any instance state your implementation needs.
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> get(String key) {
        // TODO: Implement the contract.
        if ( key == null  )
            throw new NullPointerException();
        return Optional.ofNullable(kvStore.get(key));

    }

    /** {@inheritDoc} */
    @Override
    public void put(String key, String value) {
        // TODO: Implement the contract.
        if (key != null && value != null)
            kvStore.put(key,value);
        else
            throw new NullPointerException();

//        throw new UnsupportedOperationException("TODO: implement put");
    }

    /** {@inheritDoc} */
    @Override
    public String append(String key, String suffix) {
        // TODO: Implement the contract.
        if ( key == null || suffix == null )
             throw new NullPointerException();
        String finalValue = "";
        if (kvStore.containsKey(key)){
            finalValue = kvStore.get(key).concat(suffix);
            kvStore.put(key, finalValue);
        } else{
            finalValue = suffix;
            kvStore.put(key, finalValue);
        }
        return finalValue;
//        throw new UnsupportedOperationException("TODO: implement append");
    }

    /** {@inheritDoc} */
    @Override
    public boolean delete(String key) {
        // TODO: Implement the contract.
        if (key != null ){
            if (kvStore.containsKey(key)){
                kvStore.remove(key);
                return true;
            }
            return false;
        } else {
            throw new NullPointerException();
        }


    }
}
