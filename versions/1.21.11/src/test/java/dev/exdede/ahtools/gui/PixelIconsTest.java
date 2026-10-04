package dev.exdede.ahtools.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelIconsTest {
    @Test
    void iconsAreRectangular() {
        for (String[] icon : new String[][] {PixelIcons.GITHUB, PixelIcons.GLOBE}) {
            int width = PixelIcons.width(icon);
            assertEquals(16, width);
            assertEquals(16, PixelIcons.height(icon));
            for (String row : icon) assertEquals(width, row.length(), "ragged row: " + row);
        }
    }

    @Test
    void outOfRangeIsDark() {
        assertFalse(PixelIcons.lit(PixelIcons.GITHUB, -1, 0));
        assertFalse(PixelIcons.lit(PixelIcons.GITHUB, 0, 99));
        assertTrue(PixelIcons.lit(PixelIcons.GITHUB, 5, 0));
    }
}
