package dev.exdede.ahtools.spammer;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpamSequenceTest {
    @Test
    void repeatsInOrder() {
        SpamSequence sequence = new SpamSequence(List.of("/one", "/two", "/three"));
        assertEquals("/one", sequence.next());
        assertEquals("/two", sequence.next());
        assertEquals("/three", sequence.next());
        assertEquals("/one", sequence.next());
        assertEquals("/two", sequence.next());
    }

    @Test
    void handlesASingleCommand() {
        SpamSequence sequence = new SpamSequence(List.of("/only"));
        assertEquals("/only", sequence.next());
        assertEquals("/only", sequence.next());
    }

    @Test
    void anEmptyListYieldsNothing() {
        SpamSequence sequence = new SpamSequence(List.of());
        assertNull(sequence.next());
    }

    @Test
    void dropsBlankEntries() {
        SpamSequence sequence = new SpamSequence(java.util.Arrays.asList("/one", "  ", null, "/two"));
        assertEquals(List.of("/one", "/two"), sequence.commands());
    }

    @Test
    void resetGoesBackToTheStart() {
        SpamSequence sequence = new SpamSequence(List.of("/one", "/two"));
        sequence.next();
        sequence.reset();
        assertEquals("/one", sequence.next());
    }
}
