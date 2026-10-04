package dev.exdede.ahtools.hud;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.mc.Tr;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Drag the real HUD line rather than a stand-in, so what the player positions
 * is exactly what they will see in game.
 *
 * X snaps to the center within a few per mille, because top center is where
 * almost everyone wants it and hitting 500 by hand with a mouse is a nuisance.
 */
public class HudPositionScreen extends Screen {
    private static final int SNAP_PER_MILLE = 12;

    private final Screen parent;
    private int perMilleX;
    private int perMilleY;

    public HudPositionScreen(Screen parent) {
        super(Component.translatable("ahtools.hud_screen.title"));
        this.parent = parent;
        this.perMilleX = Configs.General.HUD_X.get();
        this.perMilleY = Configs.General.HUD_Y.get();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, Theme.OVERLAY_DIM);
        super.extractRenderState(context, mouseX, mouseY, delta);

        HudRenderer.drawAt(context, this.width, this.height, perMilleX, perMilleY);

        String hint = Tr.t("ahtools.hud_screen.hint");
        context.text(this.font, hint,
            (this.width - this.font.width(hint)) / 2,
            this.height - 20, Theme.TEXT_MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        moveTo(click.x(), click.y());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        moveTo(click.x(), click.y());
        return true;
    }

    private void moveTo(double mouseX, double mouseY) {
        int x = (int) Math.round(mouseX * 1000.0 / this.width);
        if (Math.abs(x - 500) <= SNAP_PER_MILLE) x = 500;
        perMilleX = Math.max(0, Math.min(1000, x));
        perMilleY = Math.max(0, Math.min(1000, (int) Math.round(mouseY * 1000.0 / this.height)));
    }

    @Override
    public void onClose() {
        Configs.General.HUD_X.set(perMilleX);
        Configs.General.HUD_Y.set(perMilleY);
        Configs.saveToFile();
        this.minecraft.setScreen(parent);
    }
}
