package dev.exdede.ahtools.gui.tabs;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.gui.row.Row;
import dev.exdede.ahtools.gui.row.Rows;
import dev.exdede.ahtools.hud.HudPositionScreen;
import dev.exdede.ahtools.mc.Tr;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * The hotkey line is a label rather than a binder: key binds are vanilla now,
 * so the one true place to change them is the game's own controls screen, and
 * a second binder here would only be a second place to look.
 */
public final class GeneralTab {
    private GeneralTab() {}

    public static List<Row> build() {
        return List.of(
            Rows.toggle(Tr.t("ahtools.general.debug_logging"), Configs.General.DEBUG_LOGGING)
                .withTooltip(Tr.t("ahtools.tip.debug_logging")),
            Rows.cycle(Tr.t("ahtools.general.accent"), Configs.General.GUI_ACCENT, "ahtools.accent.")
                .withTooltip(Tr.t("ahtools.tip.accent")),
            Rows.toggle(Tr.t("ahtools.general.hud"), Configs.General.HUD_ENABLED)
                .withTooltip(Tr.t("ahtools.tip.hud")),
            Rows.toggle(Tr.t("ahtools.general.hud_rate"), Configs.General.HUD_SHOW_RATE)
                .withTooltip(Tr.t("ahtools.tip.hud_rate")),
            Rows.toggle(Tr.t("ahtools.general.hud_sales"), Configs.General.HUD_SHOW_SALES)
                .withTooltip(Tr.t("ahtools.tip.hud_sales")),
            Rows.action(Tr.t("ahtools.general.reposition_hud"), () -> {
                Minecraft client = Minecraft.getInstance();
                client.setScreen(new HudPositionScreen(client.screen));
            }).withTooltip(Tr.t("ahtools.tip.reposition_hud")),
            Rows.label(""),
            Rows.label(Tr.t("ahtools.general.hotkeys_hint"), Theme.TEXT_DIM));
    }
}
