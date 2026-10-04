package dev.exdede.ahtools.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluralFormTest {
    @Test
    void polishRules() {
        assertEquals(PluralForm.ONE, PluralForm.of(1));
        assertEquals(PluralForm.FEW, PluralForm.of(2));
        assertEquals(PluralForm.FEW, PluralForm.of(4));
        assertEquals(PluralForm.MANY, PluralForm.of(5));
        assertEquals(PluralForm.MANY, PluralForm.of(12));
        assertEquals(PluralForm.MANY, PluralForm.of(14));
        assertEquals(PluralForm.FEW, PluralForm.of(22));
        assertEquals(PluralForm.MANY, PluralForm.of(0));
        assertEquals(PluralForm.MANY, PluralForm.of(111));
        assertEquals(PluralForm.FEW, PluralForm.of(1_000_003));
    }
}
