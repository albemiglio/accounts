package it.albemiglio.accounts.core.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PairTest {

    @Test
    void ofExposesLeftAndRight() {
        Pair<String, Integer> pair = Pair.of("a", 1);
        assertEquals("a", pair.getLeft());
        assertEquals(1, pair.getRight());
    }

    @Test
    void reverseSwapsSides() {
        Pair<Integer, String> reversed = Pair.of("a", 1).reverse();
        assertEquals(1, reversed.getLeft());
        assertEquals("a", reversed.getRight());
    }

    @Test
    void equalsAndHashCodeAreValueBased() {
        assertEquals(Pair.of("a", 1), Pair.of("a", 1));
        assertEquals(Pair.of("a", 1).hashCode(), Pair.of("a", 1).hashCode());
    }

    @Test
    void differentValuesAreNotEqual() {
        assertNotEquals(Pair.of("a", 1), Pair.of("b", 1));
        assertNotEquals(Pair.of("a", 1), Pair.of("a", 2));
    }
}
