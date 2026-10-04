package dev.exdede.ahtools.mc;

import dev.exdede.ahtools.gui.RiskWarningScreen;
import net.minecraft.client.Minecraft;

/** Opening screens from code that should not know about Minecraft. */
public final class Screens {
    private Screens() {}

    public static void openRiskWarning(Runnable onAccept) {
        Minecraft client = Minecraft.getInstance();
        client.gui.setScreen(new RiskWarningScreen(client.gui.screen(), onAccept));
    }
}
