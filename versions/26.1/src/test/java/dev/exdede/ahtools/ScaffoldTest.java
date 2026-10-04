package dev.exdede.ahtools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScaffoldTest {
    @Test
    void modIdIsStable() {
        assertEquals("ahtools", AhToolsMod.MOD_ID);
    }
}
