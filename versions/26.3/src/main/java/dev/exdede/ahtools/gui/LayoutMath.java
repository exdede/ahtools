package dev.exdede.ahtools.gui;

/**
 * Every pixel calculation the screen makes, kept apart from the screen itself
 * so the off-by-ones have tests. Nothing here touches Minecraft.
 */
public final class LayoutMath {
    /** Gap kept between a tooltip and the screen edge. */
    public static final int TOOLTIP_MARGIN = 4;
    /** Inner padding of a tooltip box, on every side. */
    public static final int TOOLTIP_PADDING = 5;
    /** Widest a tooltip's text may run before it wraps. */
    public static final int TOOLTIP_MAX_WIDTH = 170;

    private LayoutMath() {}

    /** Keeps a panel fully on screen, pinning to the left edge when it cannot fit. */
    public static int clampPanelX(int x, int panelWidth, int screenWidth) {
        if (panelWidth >= screenWidth) return 0;
        return Math.max(0, Math.min(x, screenWidth - panelWidth));
    }

    public static int clampPanelY(int y, int panelHeight, int screenHeight) {
        if (panelHeight >= screenHeight) return 0;
        return Math.max(0, Math.min(y, screenHeight - panelHeight));
    }

    /** The value a click at mouseX means on a track starting at trackX of trackWidth pixels. */
    public static int sliderValueAt(double mouseX, int trackX, int trackWidth, int min, int max) {
        if (max <= min || trackWidth <= 0) return min;
        double fraction = (mouseX - trackX) / trackWidth;
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        return min + (int) Math.round(fraction * (max - min));
    }

    /** How many pixels of the track are filled for a value. The inverse of sliderValueAt. */
    public static int sliderFillWidth(int value, int min, int max, int trackWidth) {
        if (max <= min || trackWidth <= 0) return 0;
        double fraction = (double) (value - min) / (max - min);
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        return (int) Math.round(fraction * trackWidth);
    }

    /** Scroll offsets never expose empty space, and vanish entirely when the content fits. */
    public static int clampScroll(int scroll, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - viewportHeight);
        return Math.max(0, Math.min(scroll, maximum));
    }

    /** How wide tooltip text may run on a screen this wide, so the box always fits with its margins. */
    public static int tooltipWrapWidth(int screenWidth) {
        int available = screenWidth - 2 * TOOLTIP_MARGIN - 2 * TOOLTIP_PADDING;
        return Math.max(20, Math.min(TOOLTIP_MAX_WIDTH, available));
    }

    /**
     * Top left corner of a tooltip box. It prefers below and right of the
     * cursor, flips left at the right edge and above at the bottom edge, and is
     * then clamped, so no cursor position on any screen size puts it partly
     * off screen.
     */
    public static int[] tooltipPosition(int mouseX, int mouseY, int boxWidth, int boxHeight,
                                        int screenWidth, int screenHeight) {
        int x = mouseX + 12;
        if (x + boxWidth > screenWidth - TOOLTIP_MARGIN) x = mouseX - 8 - boxWidth;
        int y = mouseY + 16;
        if (y + boxHeight > screenHeight - TOOLTIP_MARGIN) y = mouseY - 4 - boxHeight;
        x = Math.max(TOOLTIP_MARGIN, Math.min(x, screenWidth - TOOLTIP_MARGIN - boxWidth));
        y = Math.max(TOOLTIP_MARGIN, Math.min(y, screenHeight - TOOLTIP_MARGIN - boxHeight));
        return new int[] {x, y};
    }
}
