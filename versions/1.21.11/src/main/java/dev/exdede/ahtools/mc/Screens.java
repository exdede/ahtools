package dev.exdede.ahtools.mc;

import dev.exdede.ahtools.gui.RiskWarningScreen;
import net.minecraft.client.MinecraftClient;

/** Opening screens from code that should not know about MinecraftClient. */
public final class Screens {
    private Screens() {}

    public static void openRiskWarning(Runnable onAccept) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new RiskWarningScreen(client.currentScreen, onAccept));
    }
}
