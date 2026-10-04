package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.gui.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shared row plumbing: the label, the hover highlight, the expand arrow, and a
 * no-op for every input method. A concrete row overrides only the parts it
 * actually uses, which keeps each one short enough to read at a glance.
 */
public abstract class BaseRow implements Row {
    /**
     * A supplier rather than a string: a row is built once per tab switch but
     * drawn every frame, so any label carrying a value in it (a preset's
     * selected marker, a live total) has to be recomputed at render time or it
     * shows the value the row was built with.
     */
    private final Supplier<String> label;
    private final List<Row> children = new ArrayList<>();
    private boolean expanded;
    private String tooltip;

    protected BaseRow(String label) {
        this(() -> label);
    }

    protected BaseRow(Supplier<String> label) {
        this.label = label;
    }

    protected String label() {
        return label.get();
    }

    public BaseRow withChild(Row child) {
        children.add(child);
        return this;
    }

    public BaseRow withTooltip(String text) {
        this.tooltip = text == null || text.isBlank() ? null : text;
        return this;
    }

    @Override public String tooltip() { return tooltip; }

    @Override public int height() { return ROW_HEIGHT; }
    @Override public List<Row> children() { return children; }
    @Override public boolean expandable() { return !children.isEmpty(); }
    @Override public boolean expanded() { return expanded; }
    @Override public void setExpanded(boolean value) { this.expanded = value; }
    @Override public void mouseDragged(double mouseX, int x, int width) { }
    @Override public void mouseReleased() { }
    @Override public boolean keyPressed(int keyCode) { return false; }
    @Override public boolean charTyped(char chr) { return false; }
    @Override public boolean capturingInput() { return false; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        return false;
    }

    /** Hover fill plus the expand arrow, drawn under whatever the subclass adds. */
    protected void renderBackground(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        if (hovered(mouseX, mouseY, x, y, width)) {
            context.fill(x, y, x + width, y + height(), Theme.ROW_HOVER);
        }
        if (expandable()) {
            context.fill(x + width - 8, y + height() / 2, x + width - 4, y + height() / 2 + 1,
                expanded ? Theme.accent() : Theme.TEXT_DIM);
        }
    }

    protected void renderLabel(DrawContext context, TextRenderer font, int x, int y) {
        context.drawText(font, label(), x + 6, y + (height() - font.fontHeight) / 2 + 1, Theme.TEXT, false);
    }

    protected void renderValue(DrawContext context, TextRenderer font, String value,
                               int x, int y, int width, int color) {
        int valueX = x + width - 14 - font.getWidth(value);
        context.drawText(font, value, valueX, y + (height() - font.fontHeight) / 2 + 1, color, false);
    }

    protected boolean hovered(int mouseX, int mouseY, int x, int y, int width) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height();
    }
}
