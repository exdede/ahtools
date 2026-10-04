package dev.exdede.ahtools.mc;

import net.minecraft.client.MinecraftClient;

/**
 * The only place this mod writes to the network. Everything reaching here has
 * already been through the send gate, so nothing may call it directly.
 *
 * A leading slash means a command, which the client sends without the slash;
 * anything else is a plain chat message. The seller only ever produces the
 * former, but the spammer can be given either.
 */
public final class ChatOut {
    private ChatOut() {}

    public static void send(String message) {
        var handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null || message == null || message.isBlank()) return;
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) handler.sendChatCommand(trimmed.substring(1));
        else handler.sendChatMessage(trimmed);
    }
}
