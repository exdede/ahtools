package dev.exdede.ahtools.config;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

/**
 * The four rebindable actions, all unbound by default so a fresh install
 * cannot fire automation from a key the player did not choose.
 *
 * These are vanilla key bindings rather than a mod's own hotkey system, which
 * puts them in Options, Controls next to every other binding, the first place
 * anyone looks.
 *
 * Each one is a toggle rather than a start and stop pair: one key, one
 * meaning, which is also what makes an emergency stop reachable without
 * remembering which of two keys you are on.
 */
public final class Keybinds {
    private static KeyMapping openGui;
    private static KeyMapping toggleSeller;
    private static KeyMapping toggleSpammer;
    private static KeyMapping emergencyStop;

    private Keybinds() {}

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(AhToolsMod.MOD_ID, "main"));
        openGui = bind("key.ahtools.open_gui", category);
        toggleSeller = bind("key.ahtools.toggle_seller", category);
        toggleSpammer = bind("key.ahtools.toggle_spammer", category);
        emergencyStop = bind("key.ahtools.emergency_stop", category);
    }

    private static KeyMapping bind(String translationKey, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            translationKey, InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
    }

    /**
     * Drained with a while loop rather than a single check, because a key
     * pressed twice inside one tick queues two presses and dropping one would
     * leave the toggle out of step with the key.
     */
    public static void poll(Minecraft client) {
        while (openGui.consumeClick()) client.gui.setScreen(new ClickGuiScreen());
        while (toggleSeller.consumeClick()) AhTools.INSTANCE.toggleSeller();
        while (toggleSpammer.consumeClick()) AhTools.INSTANCE.toggleSpammer();
        while (emergencyStop.consumeClick()) AhTools.INSTANCE.emergencyStop();
    }
}
