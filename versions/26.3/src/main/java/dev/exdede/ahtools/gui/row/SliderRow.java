package dev.exdede.ahtools.gui.row;

import dev.exdede.ahtools.config.IntSetting;
import dev.exdede.ahtools.gui.LayoutMath;
import dev.exdede.ahtools.gui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Label and value on one line, a thin track under it. Two lines rather than a
 * value inside the track, because tick counts run to four digits and would
 * cover the fill at the values people actually use.
 *
 * Left click drags the track; right click switches to typing an exact number,
 * since a track a couple hundred ticks wide cannot be dragged to a precise
 * value.
 */
public final class SliderRow extends BaseRow {
    private static final int TRACK_INSET = 6;
    private static final int TRACK_HEIGHT = 3;
    private static final int ERROR_FRAMES = 20;

    private final IntSetting setting;
    private final Runnable onChange;
    private boolean dragging;
    private boolean editing;
    private String buffer = "";
    private int errorFrames;

    public SliderRow(String label, IntSetting setting, Runnable onChange) {
        super(label);
        this.setting = setting;
        this.onChange = onChange;
    }

    @Override
    public int height() { return ROW_HEIGHT + 6; }

    @Override
    public boolean capturingInput() { return editing; }

    @Override
    public void render(GuiGraphicsExtractor context, Font font, int x, int y, int width, int mouseX, int mouseY) {
        renderBackground(context, x, y, width, mouseX, mouseY);
        context.text(font, label(), x + 6, y + 3, Theme.TEXT, false);
        String value = editing ? buffer + "_" : String.valueOf(setting.get());
        int color = errorFrames > 0 ? Theme.PROFIT_NEGATIVE : (editing ? Theme.accent() : Theme.TEXT_MUTED);
        if (errorFrames > 0) errorFrames--;
        context.text(font, value, x + width - 14 - font.width(value), y + 3, color, false);

        int trackX = x + TRACK_INSET;
        int trackWidth = width - TRACK_INSET * 2;
        int trackY = y + height() - TRACK_HEIGHT - 3;
        context.fill(trackX, trackY, trackX + trackWidth, trackY + TRACK_HEIGHT, Theme.TRACK);
        int fill = LayoutMath.sliderFillWidth(setting.get(), setting.min(), setting.max(), trackWidth);
        context.fill(trackX, trackY, trackX + fill, trackY + TRACK_HEIGHT, Theme.accent());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (!hovered((int) mouseX, (int) mouseY, x, y, width)) return false;
        if (button == 1) {
            editing = true;
            buffer = String.valueOf(setting.get());
            return true;
        }
        if (button != 0) return false;
        editing = false;
        dragging = true;
        apply(mouseX, x, width);
        return true;
    }

    @Override
    public void mouseDragged(double mouseX, int x, int width) {
        if (dragging) apply(mouseX, x, width);
    }

    @Override
    public void mouseReleased() { dragging = false; }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!editing) return false;
        switch (keyCode) {
            case InputConstants.KEY_ESCAPE -> { editing = false; return true; }
            case InputConstants.KEY_BACKSPACE -> {
                if (!buffer.isEmpty()) buffer = buffer.substring(0, buffer.length() - 1);
                return true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                try {
                    setting.set(Integer.parseInt(buffer.trim()));
                    onChange.run();
                    editing = false;
                }
                catch (NumberFormatException e) {
                    errorFrames = ERROR_FRAMES;
                }
                return true;
            }
            default -> { return true; }
        }
    }

    @Override
    public boolean charTyped(char chr) {
        if (!editing) return false;
        if (Character.isDigit(chr) || (chr == '-' && buffer.isEmpty())) buffer += chr;
        return true;
    }

    private void apply(double mouseX, int x, int width) {
        setting.set(LayoutMath.sliderValueAt(mouseX, x + TRACK_INSET, width - TRACK_INSET * 2,
            setting.min(), setting.max()));
        onChange.run();
    }
}
