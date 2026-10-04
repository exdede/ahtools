package dev.exdede.ahtools.gui.tabs;

import dev.exdede.ahtools.gui.Tab;
import dev.exdede.ahtools.gui.row.Row;

import java.util.List;

/** Builds the rows for one tab. Rebuilt on every tab switch, never cached. */
public final class Tabs {
    private Tabs() {}

    public static List<Row> build(Tab tab) {
        return switch (tab) {
            case GENERAL -> GeneralTab.build();
            case SELLER -> SellerTab.build();
            case PRESETS -> PresetsTab.build();
            case STATISTICS -> StatisticsTab.build();
            case SPAMMER -> SpammerTab.build();
        };
    }
}
