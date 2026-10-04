package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.gui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Click to edit in place, Enter to commit, Escape to cancel. Text editing is
 * hand rolled rather than a vanilla TextFieldWidget because these rows live
 * inside a scrolled, scissored region, where a real widget's own focus and
 * click handling fights the panel's.
 *
 * A commit that throws IllegalArgumentException keeps the row in edit mode and
 * flashes the text red, which is the contract PresetField.apply already has.
 */
public final class TextRow extends BaseRow {
    private static final int ERROR_FRAMES = 20;

    private final Supplier<String> current;
    private final Consumer<String> onCommit;

    private boolean editing;
    private String buffer = "";
    private int errorFrames;

    public TextRow(String label, Supplier<String> current, Consumer<String> onCommit) {
        super(label);
        this.current = current;
        this.onCommit = onCommit;
    }

    @Override
    public boolean capturingInput() { return editing; }

    @Override
    public void render(GuiGraphicsExtractor context, Font font, int x, int y, int width, int mouseX, int mouseY) {
        renderBackground(context, x, y, width, mouseX, mouseY);
        renderLabel(context, font, x, y);

        String value = editing ? buffer + "_" : current.get();
        int color = errorFrames > 0 ? Theme.PROFIT_NEGATIVE : (editing ? Theme.accent() : Theme.TEXT_MUTED);
        if (errorFrames > 0) errorFrames--;
        renderValue(context, font, value, x, y, width, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (button != 0 || !hovered((int) mouseX, (int) mouseY, x, y, width)) return false;
        editing = true;
        buffer = current.get();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!editing) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> { editing = false; return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!buffer.isEmpty()) buffer = buffer.substring(0, buffer.length() - 1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                try {
                    onCommit.accept(buffer);
                    editing = false;
                }
                catch (IllegalArgumentException e) {
                    errorFrames = ERROR_FRAMES;
                }
                return true;
            }
            default -> { return true; }
        }
    }

    @Override
    public boolean charTyped(char chr) {
        if (!editing || chr < 32) return false;
        buffer += chr;
        return true;
    }
}
