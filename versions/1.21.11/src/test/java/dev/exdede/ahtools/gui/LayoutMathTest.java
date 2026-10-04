package dev.exdede.ahtools.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutMathTest {
    @Test
    void panelStaysFullyOnScreen() {
        assertEquals(0, LayoutMath.clampPanelX(-40, 200, 640));
        assertEquals(440, LayoutMath.clampPanelX(900, 200, 640));
        assertEquals(100, LayoutMath.clampPanelX(100, 200, 640));
    }

    @Test
    void aPanelWiderThanTheScreenPinsToTheLeftEdge() {
        assertEquals(0, LayoutMath.clampPanelX(50, 800, 640));
    }

    @Test
    void sliderValueMapsFromMouseXAcrossTheTrack() {
        assertEquals(20, LayoutMath.sliderValueAt(100, 100, 200, 20, 1200));
        assertEquals(1200, LayoutMath.sliderValueAt(300, 100, 200, 20, 1200));
        assertEquals(610, LayoutMath.sliderValueAt(200, 100, 200, 20, 1200));
    }

    @Test
    void sliderValueClampsOutsideTheTrack() {
        assertEquals(20, LayoutMath.sliderValueAt(-500, 100, 200, 20, 1200));
        assertEquals(1200, LayoutMath.sliderValueAt(9999, 100, 200, 20, 1200));
    }

    @Test
    void sliderFillIsTheInverseOfSliderValue() {
        assertEquals(0, LayoutMath.sliderFillWidth(20, 20, 1200, 200));
        assertEquals(200, LayoutMath.sliderFillWidth(1200, 20, 1200, 200));
        assertEquals(100, LayoutMath.sliderFillWidth(610, 20, 1200, 200));
    }

    @Test
    void aZeroWidthRangeDoesNotDivideByZero() {
        assertEquals(5, LayoutMath.sliderValueAt(150, 100, 200, 5, 5));
        assertEquals(0, LayoutMath.sliderFillWidth(5, 5, 5, 200));
    }

    @Test
    void scrollStopsAtTheEndsAndDisablesWhenContentFits() {
        assertEquals(0, LayoutMath.clampScroll(-30, 500, 300));
        assertEquals(200, LayoutMath.clampScroll(9999, 500, 300));
        assertEquals(0, LayoutMath.clampScroll(50, 100, 300));
    }

    @Test
    void tooltipSitsBelowAndRightOfTheCursorWhenItFits() {
        assertArrayEquals(new int[] {112, 66}, LayoutMath.tooltipPosition(100, 50, 80, 30, 640, 360));
    }

    @Test
    void tooltipFlipsLeftAtTheRightEdge() {
        int[] at = LayoutMath.tooltipPosition(600, 50, 80, 30, 640, 360);
        assertEquals(600 - 8 - 80, at[0]);
        assertTrue(at[0] + 80 <= 640 - LayoutMath.TOOLTIP_MARGIN);
    }

    @Test
    void tooltipFlipsAboveAtTheBottomEdge() {
        int[] at = LayoutMath.tooltipPosition(100, 340, 80, 30, 640, 360);
        assertEquals(340 - 4 - 30, at[1]);
    }

    @Test
    void tooltipNeverLeavesASmallScreen() {
        for (int mouseX = 0; mouseX <= 200; mouseX += 10) {
            for (int mouseY = 0; mouseY <= 120; mouseY += 10) {
                int[] at = LayoutMath.tooltipPosition(mouseX, mouseY, 150, 60, 200, 120);
                assertTrue(at[0] >= LayoutMath.TOOLTIP_MARGIN && at[0] + 150 <= 200 - LayoutMath.TOOLTIP_MARGIN,
                    "x out of screen at " + mouseX + "," + mouseY);
                assertTrue(at[1] >= LayoutMath.TOOLTIP_MARGIN && at[1] + 60 <= 120 - LayoutMath.TOOLTIP_MARGIN,
                    "y out of screen at " + mouseX + "," + mouseY);
            }
        }
    }

    @Test
    void tooltipWrapWidthShrinksOnNarrowScreens() {
        assertEquals(LayoutMath.TOOLTIP_MAX_WIDTH, LayoutMath.tooltipWrapWidth(640));
        assertEquals(120 - 2 * LayoutMath.TOOLTIP_MARGIN - 2 * LayoutMath.TOOLTIP_PADDING,
            LayoutMath.tooltipWrapWidth(120));
    }
}
