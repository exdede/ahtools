package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.gui.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/**
 * Left click advances, right click goes back. Backed by callbacks rather than
 * a setting so the same row serves an enum setting and the selected preset,
 * which is a list that changes under it.
 */
public final class CycleRow extends BaseRow {
    private final Supplier<String> current;
    private final Runnable next;
    private final Runnable previous;

    public CycleRow(String label, Supplier<String> current, Runnable next, Runnable previous) {
        super(label);
        this.current = current;
        this.next = next;
        this.previous = previous;
    }

    @Override
    public void render(DrawContext context, TextRenderer font, int x, int y, int width, int mouseX, int mouseY) {
        renderBackground(context, x, y, width, mouseX, mouseY);
        renderLabel(context, font, x, y);
        renderValue(context, font, current.get(), x, y, width, Theme.accent());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (!hovered((int) mouseX, (int) mouseY, x, y, width)) return false;
        if (button == 0) { next.run(); return true; }
        if (button == 1) { previous.run(); return true; }
        return false;
    }
}
