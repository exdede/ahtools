package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.gui.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/** A one-shot button drawn as a row: emergency stop, reset session, add preset. */
public final class ActionRow extends BaseRow {
    private final Runnable action;

    public ActionRow(String label, Runnable action) {
        super(label);
        this.action = action;
    }

    /** For a button whose text reports state, so it updates without reopening the screen. */
    public ActionRow(Supplier<String> label, Runnable action) {
        super(label);
        this.action = action;
    }

    @Override
    public void render(DrawContext context, TextRenderer font, int x, int y, int width, int mouseX, int mouseY) {
        renderBackground(context, x, y, width, mouseX, mouseY);
        context.drawText(font, label(), x + 6, y + (height() - font.fontHeight) / 2 + 1,
            hovered(mouseX, mouseY, x, y, width) ? Theme.accent() : Theme.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (button != 0 || !hovered((int) mouseX, (int) mouseY, x, y, width)) return false;
        action.run();
        return true;
    }
}
