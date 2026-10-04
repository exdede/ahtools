package dev.exdede.ahtools.mc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;

/**
 * Opens a web page through the game's own "open this link?" screen, so a
 * click in the panel never jumps to a browser without the player agreeing.
 */
public final class Links {
    public static final String GITHUB = "https://github.com/exdede/ahtools";
    public static final String WEBSITE = "https://exdede.xyz";

    private Links() {}

    public static void open(String url) {
        MinecraftClient client = MinecraftClient.getInstance();
        ConfirmLinkScreen.open(client.currentScreen, url);
    }
}
