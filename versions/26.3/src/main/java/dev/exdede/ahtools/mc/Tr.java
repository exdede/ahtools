package dev.exdede.ahtools.mc;

import net.minecraft.client.resources.language.I18n;

/** Every player-facing string goes through here, so the lang files are the only place text lives. */
public final class Tr {
    private Tr() {}

    public static String t(String key, Object... args) {
        return I18n.get(key, args);
    }
}
