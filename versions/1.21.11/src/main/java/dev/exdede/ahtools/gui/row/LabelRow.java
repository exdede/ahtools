package dev.exdede.ahtools.gui.row;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Read-only text. Statistics and status lines are these and nothing else. */
public final class LabelRow extends BaseRow {
    private final IntSupplier color;

    public LabelRow(String label, int color) {
        this(label, () -> color);
    }

    public LabelRow(String label, IntSupplier color) {
        super(label);
        this.color = color;
    }

    /** Text and colour both live, for a label whose value changes while the screen is open. */
    public LabelRow(Supplier<String> label, IntSupplier color) {
        super(label);
        this.color = color;
    }

    @Override
    public void render(DrawContext context, TextRenderer font, int x, int y, int width, int mouseX, int mouseY) {
        renderBackground(context, x, y, width, mouseX, mouseY);
        context.drawText(font, label(), x + 6, y + (height() - font.fontHeight) / 2 + 1, color.getAsInt(), false);
    }
}
