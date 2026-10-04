package dev.exdede.ahtools.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

/**
 * Client-side status lines. These never leave the client: they are written
 * straight into the local chat HUD, so toggling automation cannot itself
 * become a visible message on the server.
 */
public final class Feedback {
    private Feedback() {}

    public static void message(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.literal("[AHTools] " + text));
    }

    /**
     * For something that stopped on its own. A line in chat is easy to miss
     * while looking elsewhere, so this also plays a low note, heard only here.
     */
    public static void alert(String text) {
        message(text);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
    }
}
