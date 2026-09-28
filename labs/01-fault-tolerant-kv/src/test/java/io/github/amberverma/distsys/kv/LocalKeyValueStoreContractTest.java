package io.github.amberverma.distsys.kv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Acceptance tests for observable behavior; no particular data structure is required. */
@Tag("local-kv-exercise")
class LocalKeyValueStoreContractTest {
    private final KeyValueStore store = new InMemoryKeyValueStore();

    @Test
    void freshStoreReturnsEmptyForMissingKeys() {
        assertEquals(Optional.empty(), store.get("missing"));
        assertEquals(Optional.empty(), store.get("another"));
    }

    @Test
    void putCreatesAReadableValue() {
        store.put("name", "Ada");

        assertEquals(Optional.of("Ada"), store.get("name"));
        assertEquals(Optional.of("Ada"), store.get("name"));
    }

    @Test
    void putReplacesTheWholeValue() {
        store.put("name", "Ada");
        store.put("name", "Grace");

        assertEquals(Optional.of("Grace"), store.get("name"));
    }

    @Test
    void appendCreatesAMissingKey() {
        assertEquals("hello", store.append("message", "hello"));
        assertEquals(Optional.of("hello"), store.get("message"));
    }

    @Test
    void appendReturnsAndStoresTheFullUpdatedValue() {
        store.put("message", "hello");

        assertEquals("hello world", store.append("message", " world"));
        assertEquals(Optional.of("hello world"), store.get("message"));
    }

    @Test
    void repeatedAppendsApplyEveryTimeInCallOrder() {
        assertEquals("x", store.append("message", "x"));
        assertEquals("xx", store.append("message", "x"));
        assertEquals("xxy", store.append("message", "y"));
        assertEquals(Optional.of("xxy"), store.get("message"));
    }

    @Test
    void putCanReplaceAnAccumulatedValueBeforeAnotherAppend() {
        store.append("message", "old");
        store.append("message", " text");
        store.put("message", "new");

        assertEquals("new text", store.append("message", " text"));
        assertEquals(Optional.of("new text"), store.get("message"));
    }

    @Test
    void operationsOnOneKeyDoNotChangeOtherKeys() {
        store.put("first", "A");
        store.put("second", "B");
        store.append("first", "1");
        store.put("second", "C");

        assertEquals(Optional.of("A1"), store.get("first"));
        assertEquals(Optional.of("C"), store.get("second"));
        assertEquals(Optional.empty(), store.get("third"));
    }

    @Test
    void storedEmptyStringIsDifferentFromAMissingKey() {
        store.put("empty", "");

        assertEquals(Optional.of(""), store.get("empty"));
        assertEquals(Optional.empty(), store.get("missing"));
        assertEquals("x", store.append("empty", "x"));
        store.put("empty", "");
        assertEquals(Optional.of(""), store.get("empty"));
    }

    @Test
    void emptyAppendCreatesAPresentEmptyValueForAMissingKey() {
        assertEquals("", store.append("empty", ""));
        assertEquals(Optional.of(""), store.get("empty"));
    }

    @Test
    void emptyAppendPreservesAnExistingValue() {
        store.put("message", "hello");

        assertEquals("hello", store.append("message", ""));
        assertEquals(Optional.of("hello"), store.get("message"));
    }

    @Test
    void emptyWhitespaceAndUnicodeStringsArePreservedExactly() {
        store.put("", "  leading and trailing  ");
        store.put(" ", "\t\n");
        store.put("\u03ba\u03bb\u03b5\u03b9\u03b4\u03af", "\u4f60\u597d");

        assertEquals(Optional.of("  leading and trailing  "), store.get(""));
        assertEquals("\t\n ", store.append(" ", " "));
        assertEquals("\u4f60\u597d\ud83d\ude00", store.append("\u03ba\u03bb\u03b5\u03b9\u03b4\u03af", "\ud83d\ude00"));
        assertEquals(Optional.of("\u4f60\u597d\ud83d\ude00"), store.get("\u03ba\u03bb\u03b5\u03b9\u03b4\u03af"));
    }

    @Test
    void keysAreCaseSensitiveAndAreNotTrimmed() {
        store.put("Key", "upper");
        store.put("key", "lower");
        store.put(" key ", "spaced");

        assertEquals(Optional.of("upper"), store.get("Key"));
        assertEquals(Optional.of("lower"), store.get("key"));
        assertEquals(Optional.of("spaced"), store.get(" key "));
        assertEquals(Optional.empty(), store.get("KEY"));
    }

    @Test
    void storeInstancesHaveIndependentState() {
        var other = new InMemoryKeyValueStore();
        store.put("shared-name", "first");
        assertEquals(Optional.empty(), other.get("shared-name"));
        other.put("shared-name", "second");
        other.append("shared-name", "!");

        assertEquals(Optional.of("first"), store.get("shared-name"));
        assertEquals(Optional.of("second!"), other.get("shared-name"));
    }

    @Test
    void previouslyReturnedResultsDoNotChangeAfterLaterWrites() {
        store.put("message", "hello");
        var readResult = store.get("message");
        var appendResult = store.append("message", "!");
        store.put("message", "replacement");

        assertEquals(Optional.of("hello"), readResult);
        assertEquals("hello!", appendResult);
        assertEquals(Optional.of("replacement"), store.get("message"));
    }

    @Test
    void deleteMissingKeyReturnsFalseAndLeavesItMissing() {
        assertFalse(store.delete("missing"));
        assertEquals(Optional.empty(), store.get("missing"));
    }

    @Test
    void deleteRemovesAnExistingKeyAndRepeatedDeleteReturnsFalse() {
        store.put("message", "hello");

        assertTrue(store.delete("message"));
        assertEquals(Optional.empty(), store.get("message"));
        assertFalse(store.delete("message"));
        assertEquals(Optional.empty(), store.get("message"));
    }

    @Test
    void deleteRemovesEmptyValuesAndAcceptsAnEmptyKey() {
        store.put("empty-value", "");
        store.put("", "");

        assertTrue(store.delete("empty-value"));
        assertEquals(Optional.empty(), store.get("empty-value"));
        assertTrue(store.delete(""));
        assertEquals(Optional.empty(), store.get(""));
        assertFalse(store.delete(""));
    }

    @Test
    void putAndAppendCanRecreateADeletedKey() {
        store.put("message", "old");
        assertTrue(store.delete("message"));
        store.put("message", "new");
        assertEquals(Optional.of("new"), store.get("message"));

        assertTrue(store.delete("message"));
        assertEquals("fresh", store.append("message", "fresh"));
        assertEquals(Optional.of("fresh"), store.get("message"));

        assertTrue(store.delete("message"));
        assertEquals("", store.append("message", ""));
        assertEquals(Optional.of(""), store.get("message"));
    }

    @Test
    void deleteUsesTheExactKeyAndLeavesOtherKeysUnchanged() {
        store.put("Key", "upper");
        store.put("key", "lower");
        store.put(" key ", "spaced");
        store.put("\u03ba", "unicode");

        assertFalse(store.delete("KEY"));
        assertTrue(store.delete("key"));
        assertEquals(Optional.empty(), store.get("key"));
        assertEquals(Optional.of("upper"), store.get("Key"));
        assertEquals(Optional.of("spaced"), store.get(" key "));
        assertEquals(Optional.of("unicode"), store.get("\u03ba"));

        assertTrue(store.delete(" key "));
        assertEquals(Optional.empty(), store.get(" key "));
        assertTrue(store.delete("\u03ba"));
        assertEquals(Optional.empty(), store.get("\u03ba"));
        assertEquals(Optional.of("upper"), store.get("Key"));
    }

    @Test
    void deleteDoesNotAffectAnotherStoreInstance() {
        var other = new InMemoryKeyValueStore();
        store.put("shared-name", "first");
        other.put("shared-name", "second");

        assertTrue(store.delete("shared-name"));
        assertFalse(store.delete("shared-name"));
        assertEquals(Optional.of("second"), other.get("shared-name"));
    }

    @Test
    void deleteRejectsNullWithoutChangingExistingValues() {
        assertThrows(NullPointerException.class, () -> store.delete(null));
        store.put("kept", "original");
        store.put("", "empty-key");

        assertThrows(NullPointerException.class, () -> store.delete(null));
        assertEquals(Optional.of("original"), store.get("kept"));
        assertEquals(Optional.of("empty-key"), store.get(""));
    }

    @Test
    void nullKeysAreRejectedWithoutChangingExistingValues() {
        assertThrows(NullPointerException.class, () -> store.get(null));
        assertThrows(NullPointerException.class, () -> store.put(null, "value"));
        assertThrows(NullPointerException.class, () -> store.append(null, "suffix"));

        store.put("kept", "original");
        assertThrows(NullPointerException.class, () -> store.get(null));
        assertThrows(NullPointerException.class, () -> store.put(null, "value"));
        assertThrows(NullPointerException.class, () -> store.append(null, "suffix"));
        assertEquals(Optional.of("original"), store.get("kept"));
    }

    @Test
    void nullValuesAndSuffixesAreRejectedWithoutCreatingOrChangingKeys() {
        assertThrows(NullPointerException.class, () -> store.put("missing", null));
        assertEquals(Optional.empty(), store.get("missing"));
        assertThrows(NullPointerException.class, () -> store.append("missing", null));
        assertEquals(Optional.empty(), store.get("missing"));

        store.put("kept", "original");
        assertThrows(NullPointerException.class, () -> store.put("kept", null));
        assertEquals(Optional.of("original"), store.get("kept"));
        assertThrows(NullPointerException.class, () -> store.append("kept", null));
        assertEquals(Optional.of("original"), store.get("kept"));
    }
}
