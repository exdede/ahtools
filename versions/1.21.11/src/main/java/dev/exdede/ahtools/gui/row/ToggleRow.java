package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.gui.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;

/**
 * A pill on the right that fills with the accent when on. The state is read
 * through a supplier rather than stored, so a toggle backed by something other
 * than a setting (the seller running, say) stays correct without anyone having
 * to remember to refresh the row.
 */
public final class ToggleRow extends BaseRow {
    private static final int PILL_WIDTH = 18;
    private static final int PILL_HEIGHT = 8;

    private final BooleanSupplier state;
    private final Runnable onToggle;

    public ToggleRow(String label, BooleanSupplier state, Runnable onToggle) {
        super(label);
        this.state = state;
        this.onToggle = onToggle;
    }

    @Override
    public void render(DrawContext context, TextRenderer font, int x, int y, int width, int mouseX, int mouseY) {
        renderBackground(context, x, y, width, mouseX, mouseY);
        renderLabel(context, font, x, y);

        boolean on = state.getAsBoolean();
        int pillX = x + width - 14 - PILL_WIDTH;
        int pillY = y + (height() - PILL_HEIGHT) / 2;
        context.fill(pillX, pillY, pillX + PILL_WIDTH, pillY + PILL_HEIGHT, on ? Theme.accent() : Theme.TRACK);
        int knobX = on ? pillX + PILL_WIDTH - PILL_HEIGHT : pillX;
        context.fill(knobX, pillY, knobX + PILL_HEIGHT, pillY + PILL_HEIGHT, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (button != 0 || !hovered((int) mouseX, (int) mouseY, x, y, width)) return false;
        onToggle.run();
        return true;
    }
}
