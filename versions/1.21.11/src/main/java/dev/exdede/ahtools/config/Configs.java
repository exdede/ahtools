package dev.exdede.ahtools.config;

import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.gui.Theme;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global settings. Seller pricing and delays live in presets rather than here,
 * so this file only holds what is global: the spammer's own list and delay,
 * the look of the screen, and debug logging.
 *
 * The section name and every key match what pre-1.0 builds wrote, so an
 * ahtools.json from an older build loads as is.
 *
 * Every delay here still passes through DelayRule, which clamps the floor. The
 * GUI's own minimum is a convenience, not the enforcement point.
 */
public final class Configs {
    private static final String CONFIG_FILE_NAME = AhToolsMod.MOD_ID + ".json";

    public static class General {
        public static final BoolSetting DEBUG_LOGGING = new BoolSetting(
            "debugLogging", false, "Verbose logging of every automated action and matched chat line");
        public static final StringListSetting SPAM_COMMANDS = new StringListSetting(
            "spamCommands", List.of(), "Commands the spammer sends in order");
        public static final BoolSetting SPAMMER_RANDOM_DELAY = new BoolSetting(
            "spammerRandomDelay", false, "Roll a fresh delay for every spammer message instead of using one fixed value");
        public static final IntSetting SPAMMER_DELAY_TICKS = new IntSetting(
            "spammerDelayTicks", 25, DelayRule.MIN_DELAY_TICKS, 1200,
            "Fixed delay between spammer messages, in ticks. 20 ticks is one second and is the hard minimum");
        public static final IntSetting SPAMMER_MIN_DELAY_TICKS = new IntSetting(
            "spammerMinDelayTicks", 25, DelayRule.MIN_DELAY_TICKS, 1200,
            "Shortest random delay between spammer messages, in ticks");
        public static final IntSetting SPAMMER_MAX_DELAY_TICKS = new IntSetting(
            "spammerMaxDelayTicks", 40, DelayRule.MIN_DELAY_TICKS, 1200,
            "Longest random delay between spammer messages, in ticks");

        public static final EnumSetting<Theme.Accent> GUI_ACCENT = new EnumSetting<>(
            "guiAccent", Theme.Accent.PINK, "Accent color of the AHTools screen and HUD");
        public static final IntSetting GUI_X = new IntSetting(
            "guiX", 40, 0, 4000, "Left edge of the AHTools panel, in pixels");
        public static final IntSetting GUI_Y = new IntSetting(
            "guiY", 40, 0, 4000, "Top edge of the AHTools panel, in pixels");

        public static final BoolSetting HUD_ENABLED = new BoolSetting(
            "hudEnabled", true, "Show session profit at the top of the screen");
        public static final BoolSetting HUD_SHOW_RATE = new BoolSetting(
            "hudShowRate", true, "Include profit per hour on the HUD");
        public static final BoolSetting HUD_SHOW_SALES = new BoolSetting(
            "hudShowSales", true, "Include the session sale count on the HUD");
        public static final IntSetting HUD_X = new IntSetting(
            "hudX", 500, 0, 1000, "Horizontal HUD position, in per mille of screen width");
        public static final IntSetting HUD_Y = new IntSetting(
            "hudY", 20, 0, 1000, "Vertical HUD position, in per mille of screen height");

        public static final IntSetting RISK_ACCEPTED_VERSION = new IntSetting(
            "riskAcceptedVersion", 0, 0, 1000,
            "Which version of the ban risk warning the player accepted. 0 means never");

        public static final List<Setting<?>> ALL = List.of(
            DEBUG_LOGGING, SPAM_COMMANDS, SPAMMER_RANDOM_DELAY,
            SPAMMER_DELAY_TICKS, SPAMMER_MIN_DELAY_TICKS, SPAMMER_MAX_DELAY_TICKS,
            GUI_ACCENT, GUI_X, GUI_Y,
            HUD_ENABLED, HUD_SHOW_RATE, HUD_SHOW_SALES, HUD_X, HUD_Y,
            RISK_ACCEPTED_VERSION);
    }

    private Configs() {}

    public static DelayRule spammerDelayRule() {
        return General.SPAMMER_RANDOM_DELAY.get()
            ? DelayRule.random(General.SPAMMER_MIN_DELAY_TICKS.get(), General.SPAMMER_MAX_DELAY_TICKS.get())
            : DelayRule.fixed(General.SPAMMER_DELAY_TICKS.get());
    }

    public static void loadFromFile() {
        SettingsFile.load(configFile(), sections());
    }

    public static void saveToFile() {
        SettingsFile.save(configFile(), sections());
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    private static Map<String, List<Setting<?>>> sections() {
        Map<String, List<Setting<?>>> sections = new LinkedHashMap<>();
        sections.put("General", General.ALL);
        return sections;
    }
}
