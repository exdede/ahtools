package dev.exdede.ahtools.gui;

import dev.exdede.ahtools.mc.Tr;

import java.util.Locale;

/**
 * The tab rail is narrow, so each tab carries a short rail label as well as a
 * full title, both from the lang files. There is no hotkeys tab: key binds
 * are vanilla now and live in Options, Controls.
 */
public enum Tab {
    GENERAL, SELLER, PRESETS, STATISTICS, SPAMMER;

    private String key() {
        return "ahtools.tab." + name().toLowerCase(Locale.ROOT);
    }

    public String shortLabel() { return Tr.t(key() + ".short"); }
    public String title() { return Tr.t(key()); }
}
