package dev.exdede.ahtools.gui;

import dev.exdede.ahtools.config.Configs;

/**
 * The only file that names a color. Every widget asks here, so changing the
 * accent is one setting rather than a search across the screen code.
 *
 * No Minecraft imports on purpose: the HUD and the tests both read these
 * values, and neither wants a client on the classpath to do it.
 */
public final class Theme {
    public enum Accent {
        PINK(0xFFFF5FA2),
        PURPLE(0xFFB05FFF),
        CYAN(0xFF3FD9FF),
        GREEN(0xFF4CE07A),
        ORANGE(0xFFFF9A3F);

        private final int argb;

        Accent(int argb) { this.argb = argb; }

        public int argb() { return argb; }
    }

    public static final int PANEL_BG = 0xF0121218;
    public static final int TAB_RAIL_BG = 0xF01A1A22;
    public static final int TITLE_BG = 0xF01E1E28;
    public static final int ROW_BG = 0x00000000;
    public static final int ROW_HOVER = 0x22FFFFFF;
    public static final int CHILD_BG = 0x14FFFFFF;
    public static final int SEPARATOR = 0x33FFFFFF;
    public static final int TRACK = 0xFF2A2A36;
    public static final int TEXT = 0xFFE8E8F0;
    public static final int TEXT_MUTED = 0xFF9A9AAC;
    public static final int TEXT_DIM = 0xFF6A6A7C;
    public static final int PROFIT_POSITIVE = 0xFF4CE07A;
    public static final int PROFIT_NEGATIVE = 0xFFFF5F5F;
    public static final int OVERLAY_DIM = 0xA0000000;
    public static final int TOOLTIP_BG = 0xF20C0C12;

    private Theme() {}

    public static int accent() {
        return Configs.General.GUI_ACCENT.get().argb();
    }

    /** Keeps the color, replaces the alpha. Used by anything that fades. */
    public static int withAlpha(int argb, float alpha) {
        int clamped = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (clamped << 24) | (argb & 0x00FFFFFF);
    }
}
