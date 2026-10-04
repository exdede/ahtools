package dev.exdede.ahtools.gui.row;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * One line of the panel. Rows are drawn and clicked through the same rectangle
 * the screen hands them, so none of them has to know where the panel is.
 */
public interface Row {
    int ROW_HEIGHT = 16;

    /** Height of this row alone, excluding children. */
    int height();

    /** Rows revealed under this one while it is expanded. Empty when it has none. */
    List<Row> children();

    boolean expandable();
    boolean expanded();
    void setExpanded(boolean expanded);

    void render(GuiGraphicsExtractor context, Font font, int x, int y, int width, int mouseX, int mouseY);

    boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width);
    void mouseDragged(double mouseX, int x, int width);
    void mouseReleased();
    boolean keyPressed(int keyCode);
    boolean charTyped(char chr);

    /** True while this row wants every key press, so the screen does not close on Escape mid-edit. */
    boolean capturingInput();

    /** What hovering this row explains, already translated, or null for no tooltip. */
    default String tooltip() { return null; }
}
