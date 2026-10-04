package dev.exdede.ahtools.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.net.URI;

/**
 * Opens a web page through the game's own "open this link?" screen, so a
 * click in the panel never jumps to a browser without the player agreeing.
 */
public final class Links {
    public static final String GITHUB = "https://github.com/exdede/ahtools";
    public static final String WEBSITE = "https://exdede.xyz";

    private Links() {}

    public static void open(String url) {
        Minecraft client = Minecraft.getInstance();
        ConfirmLinkScreen.confirmLinkNow(client.gui.screen(), URI.create(url));
    }
}
