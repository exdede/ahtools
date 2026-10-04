package dev.exdede.ahtools.hud;

import dev.exdede.ahtools.AhTools;
import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.gui.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The thin Minecraft side of the HUD: it asks ProfitHud what to draw and draws
 * it. Every decision worth arguing about lives in ProfitHud, which is why this
 * file has no tests and no branching beyond "is it on".
 *
 * Not gated on the seller running, because a stopped seller does not undo the
 * money it already made and blanking the number would read as a reset.
 */
public final class HudRenderer {
    private HudRenderer() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.MISC_OVERLAYS,
            Identifier.of(AhToolsMod.MOD_ID, "profit"),
            (context, tickCounter) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                draw(context, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            });
    }

    public static void draw(DrawContext context, int screenWidth, int screenHeight) {
        if (!Configs.General.HUD_ENABLED.get()) return;
        AhTools mod = AhTools.INSTANCE;
        if (mod == null) return;
        drawAt(context, screenWidth, screenHeight,
            Configs.General.HUD_X.get(), Configs.General.HUD_Y.get());
    }

    /**
     * Separate from draw so the reposition screen can render the line at a
     * position that is not the saved one yet.
     */
    public static void drawAt(DrawContext context, int screenWidth, int screenHeight,
                              int perMilleX, int perMilleY) {
        AhTools mod = AhTools.INSTANCE;
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        ProfitHud hud = mod.hud();

        long elapsed = System.currentTimeMillis() - mod.sessionStartMillis();
        List<ProfitHud.Segment> segments = hud.segments(mod.stats().session(null), elapsed,
            Configs.General.HUD_SHOW_RATE.get(), Configs.General.HUD_SHOW_SALES.get());
        if (segments.isEmpty()) return;

        String separator = "  |  ";
        int totalWidth = 0;
        for (int i = 0; i < segments.size(); i++) {
            totalWidth += font.getWidth(segments.get(i).text());
            if (i < segments.size() - 1) totalWidth += font.getWidth(separator);
        }

        int centerX = screenWidth * perMilleX / 1000;
        int x = centerX - totalWidth / 2;
        int y = screenHeight * perMilleY / 1000;

        // The pop is a scale about the center of the line, so a fresh sale
        // draws the eye without moving where the line sits.
        float scale = hud.popScale(mod.tick());
        boolean scaled = scale != 1.0f;
        if (scaled) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(centerX, y);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-centerX, -y);
        }
        for (int i = 0; i < segments.size(); i++) {
            ProfitHud.Segment segment = segments.get(i);
            context.drawTextWithShadow(font, segment.text(), x, y, segment.color());
            x += font.getWidth(segment.text());
            if (i < segments.size() - 1) {
                context.drawTextWithShadow(font, separator, x, y,
                    Theme.withAlpha(Theme.accent(), 0.6f));
                x += font.getWidth(separator);
            }
        }
        if (scaled) context.getMatrices().popMatrix();

        ProfitHud.Delta delta = hud.delta(mod.tick());
        if (delta != null) {
            int deltaX = centerX - font.getWidth(delta.text()) / 2;
            context.drawTextWithShadow(font, delta.text(), deltaX,
                y + delta.riseY() - font.fontHeight - 1,
                Theme.withAlpha(delta.text().startsWith("-")
                    ? Theme.PROFIT_NEGATIVE : Theme.PROFIT_POSITIVE, delta.alpha()));
        }
    }
}
