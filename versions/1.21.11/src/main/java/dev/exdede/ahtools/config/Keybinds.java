package dev.exdede.ahtools.config;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

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
    private static KeyBinding openGui;
    private static KeyBinding toggleSeller;
    private static KeyBinding toggleSpammer;
    private static KeyBinding emergencyStop;

    private Keybinds() {}

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(
            Identifier.of(AhToolsMod.MOD_ID, "main"));
        openGui = bind("key.ahtools.open_gui", category);
        toggleSeller = bind("key.ahtools.toggle_seller", category);
        toggleSpammer = bind("key.ahtools.toggle_spammer", category);
        emergencyStop = bind("key.ahtools.emergency_stop", category);
    }

    private static KeyBinding bind(String translationKey, KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            translationKey, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
    }

    /**
     * Drained with a while loop rather than a single check, because a key
     * pressed twice inside one tick queues two presses and dropping one would
     * leave the toggle out of step with the key.
     */
    public static void poll(MinecraftClient client) {
        while (openGui.wasPressed()) client.setScreen(new ClickGuiScreen());
        while (toggleSeller.wasPressed()) AhTools.INSTANCE.toggleSeller();
        while (toggleSpammer.wasPressed()) AhTools.INSTANCE.toggleSpammer();
        while (emergencyStop.wasPressed()) AhTools.INSTANCE.emergencyStop();
    }
}
