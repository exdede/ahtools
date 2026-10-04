package dev.exdede.ahtools.gui;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.gui.row.Row;
import dev.exdede.ahtools.gui.tabs.Tabs;
import dev.exdede.ahtools.mc.Links;
import dev.exdede.ahtools.mc.Tr;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * One floating panel: a title bar to drag it by, a tab rail down the left, and
 * a scrolled column of rows. Everything is drawn by hand rather than built out
 * of vanilla widgets, because vanilla widgets cannot be nested inside a
 * scrolled region without fighting the screen for clicks.
 *
 * Rows are rebuilt on every tab switch instead of being cached, since most of
 * them read live state (the seller's phase, the preset list, the statistics)
 * and a cached row would show a stale value.
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 190;
    private static final int TITLE_HEIGHT = 16;
    private static final int RAIL_WIDTH = 38;
    private static final int TAB_HEIGHT = 18;
    private static final int SCROLL_STEP = 12;
    /** Hover this long before a tooltip appears, so sweeping the mouse across rows stays quiet. */
    private static final long TOOLTIP_DELAY_MS = 400L;
    /** Square hit box of a link icon at the bottom of the tab rail; the 16 pixel icon sits centered in it. */
    private static final int LINK_BOX = 22;
    private static final int LINK_GAP = 3;

    /** The link icons at the bottom of the tab rail, top to bottom. */
    private enum LinkIcon {
        GITHUB(PixelIcons.GITHUB, Links.GITHUB, "ahtools.tip.github"),
        WEBSITE(PixelIcons.GLOBE, Links.WEBSITE, "ahtools.tip.website");

        final String[] pixels;
        final String url;
        final String tooltipKey;

        LinkIcon(String[] pixels, String url, String tooltipKey) {
            this.pixels = pixels;
            this.url = url;
            this.tooltipKey = tooltipKey;
        }
    }

    /** Survives closing the screen, so reopening lands on the tab you left. */
    private static Tab tab = Tab.GENERAL;

    /**
     * Set by a tab builder after it changes which rows exist (add/rename/
     * delete a preset, add/remove a spam command) rather than just a field
     * inside one. Row values are read live through suppliers, but the row
     * list itself is only ever regenerated on tab switch, so a structural
     * change needs this to show up before the player closes and reopens the
     * screen.
     */
    private static boolean dirty = false;

    public static void markDirty() {
        dirty = true;
    }

    private List<Row> rows = List.of();
    private int panelX;
    private int panelY;
    private int scroll;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private Object hoveredTarget;
    private long hoveredSinceMs;

    public ClickGuiScreen() {
        super(Component.literal("AHTools"));
    }

    @Override
    protected void init() {
        panelX = LayoutMath.clampPanelX(Configs.General.GUI_X.get(), PANEL_WIDTH, this.width);
        panelY = LayoutMath.clampPanelY(Configs.General.GUI_Y.get(), PANEL_HEIGHT, this.height);
        rebuild();
    }

    private void rebuild() {
        rows = Tabs.build(tab);
        scroll = 0;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Configs.General.GUI_X.set(panelX);
        Configs.General.GUI_Y.set(panelY);
        Configs.saveToFile();
        super.onClose();
    }

    // Rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (dirty) {
            dirty = false;
            rebuild();
        }
        super.extractRenderState(context, mouseX, mouseY, delta);

        int contentX = panelX + RAIL_WIDTH;
        int contentY = panelY + TITLE_HEIGHT;
        int contentWidth = PANEL_WIDTH - RAIL_WIDTH;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + TITLE_HEIGHT, Theme.TITLE_BG);
        context.fill(panelX, panelY + TITLE_HEIGHT, panelX + RAIL_WIDTH, panelY + PANEL_HEIGHT, Theme.TAB_RAIL_BG);
        context.fill(contentX, contentY, contentX + contentWidth, panelY + PANEL_HEIGHT, Theme.PANEL_BG);
        context.fill(panelX, panelY + TITLE_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + TITLE_HEIGHT, Theme.accent());

        context.text(this.font, "AHTools", panelX + 6, panelY + 4, Theme.TEXT, false);
        context.text(this.font, tab.title(),
            panelX + PANEL_WIDTH - 6 - this.font.width(tab.title()),
            panelY + 4, Theme.TEXT_DIM, false);

        renderRail(context, mouseX, mouseY);
        renderLinkIcons(context, mouseX, mouseY);

        context.enableScissor(contentX, contentY, contentX + contentWidth, panelY + PANEL_HEIGHT);
        int y = contentY + 2 - scroll;
        for (Row row : flatten()) {
            row.render(context, this.font, contentX, y, contentWidth, mouseX, mouseY);
            y += row.height();
        }
        context.disableScissor();

        renderTooltip(context, mouseX, mouseY);
    }

    /**
     * Drawn last and outside the scissor, so it sits over the panel and may
     * spill past it, but LayoutMath keeps every pixel of it on the screen.
     * Nothing shows while dragging or typing, when it would only be in the way.
     */
    private void renderTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        LinkIcon icon = linkAt(mouseX, mouseY);
        Row row = icon == null ? rowAt(mouseX, mouseY) : null;
        Object target = icon != null ? icon : row;
        String tooltip = icon != null ? Tr.t(icon.tooltipKey) : (row == null ? null : row.tooltip());
        long now = System.currentTimeMillis();
        if (target != hoveredTarget) {
            hoveredTarget = target;
            hoveredSinceMs = now;
        }
        if (tooltip == null || dragging || anyRowCapturing()) return;
        if (now - hoveredSinceMs < TOOLTIP_DELAY_MS) return;

        int pad = LayoutMath.TOOLTIP_PADDING;
        List<FormattedCharSequence> lines = this.font.split(Component.literal(tooltip),
            LayoutMath.tooltipWrapWidth(this.width));
        int textWidth = 0;
        for (FormattedCharSequence line : lines) textWidth = Math.max(textWidth, this.font.width(line));
        int lineHeight = this.font.lineHeight + 2;
        int boxWidth = textWidth + pad * 2 + 2;
        int boxHeight = lines.size() * lineHeight - 2 + pad * 2;
        int[] at = LayoutMath.tooltipPosition(mouseX, mouseY, boxWidth, boxHeight, this.width, this.height);
        int x = at[0];
        int y = at[1];

        context.fill(x, y, x + boxWidth, y + boxHeight, Theme.TOOLTIP_BG);
        context.fill(x, y, x + boxWidth, y + 1, Theme.SEPARATOR);
        context.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, Theme.SEPARATOR);
        context.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, Theme.SEPARATOR);
        context.fill(x, y, x + 2, y + boxHeight, Theme.accent());
        int textY = y + pad;
        for (FormattedCharSequence line : lines) {
            context.text(this.font, line, x + 2 + pad, textY, Theme.TEXT, false);
            textY += lineHeight;
        }
    }

    /**
     * The link icons sit in the empty bottom of the tab rail, each drawn pixel
     * by pixel from PixelIcons, muted until hovered and accent colored then.
     */
    private void renderLinkIcons(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        for (LinkIcon icon : LinkIcon.values()) {
            int boxX = linkBoxX();
            int boxY = linkBoxY(icon);
            boolean hovered = icon == linkAt(mouseX, mouseY);
            if (hovered) context.fill(boxX, boxY, boxX + LINK_BOX, boxY + LINK_BOX, Theme.ROW_HOVER);
            int color = hovered ? Theme.accent() : Theme.TEXT_MUTED;
            int iconX = boxX + (LINK_BOX - PixelIcons.width(icon.pixels)) / 2;
            int iconY = boxY + (LINK_BOX - PixelIcons.height(icon.pixels)) / 2;
            for (int py = 0; py < PixelIcons.height(icon.pixels); py++) {
                for (int px = 0; px < PixelIcons.width(icon.pixels); px++) {
                    if (PixelIcons.lit(icon.pixels, px, py)) {
                        context.fill(iconX + px, iconY + py, iconX + px + 1, iconY + py + 1, color);
                    }
                }
            }
        }
    }

    private int linkBoxX() {
        return panelX + (RAIL_WIDTH - LINK_BOX) / 2;
    }

    /** Stacked up from the bottom of the rail, last icon lowest. */
    private int linkBoxY(LinkIcon icon) {
        int fromBottom = LinkIcon.values().length - icon.ordinal();
        return panelY + PANEL_HEIGHT - LINK_GAP - fromBottom * (LINK_BOX + LINK_GAP) + LINK_GAP;
    }

    private LinkIcon linkAt(double mouseX, double mouseY) {
        for (LinkIcon icon : LinkIcon.values()) {
            int boxX = linkBoxX();
            int boxY = linkBoxY(icon);
            if (mouseX >= boxX && mouseX < boxX + LINK_BOX && mouseY >= boxY && mouseY < boxY + LINK_BOX) return icon;
        }
        return null;
    }

    private Row rowAt(double mouseX, double mouseY) {
        int contentX = panelX + RAIL_WIDTH;
        int contentY = panelY + TITLE_HEIGHT;
        if (mouseX < contentX || mouseX >= panelX + PANEL_WIDTH) return null;
        if (mouseY < contentY || mouseY >= panelY + PANEL_HEIGHT) return null;
        int y = contentY + 2 - scroll;
        for (Row row : flatten()) {
            if (mouseY >= y && mouseY < y + row.height()) return row;
            y += row.height();
        }
        return null;
    }

    private boolean anyRowCapturing() {
        for (Row row : flatten()) {
            if (row.capturingInput()) return true;
        }
        return false;
    }

    private void renderRail(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int y = panelY + TITLE_HEIGHT + 2;
        for (Tab value : Tab.values()) {
            boolean selected = value == tab;
            boolean hovered = mouseX >= panelX && mouseX < panelX + RAIL_WIDTH
                && mouseY >= y && mouseY < y + TAB_HEIGHT;
            if (selected) context.fill(panelX, y, panelX + 2, y + TAB_HEIGHT, Theme.accent());
            if (hovered && !selected) context.fill(panelX, y, panelX + RAIL_WIDTH, y + TAB_HEIGHT, Theme.ROW_HOVER);
            context.text(this.font, value.shortLabel(), panelX + 7,
                y + (TAB_HEIGHT - this.font.lineHeight) / 2 + 1,
                selected ? Theme.accent() : Theme.TEXT_MUTED, false);
            y += TAB_HEIGHT;
        }
    }

    /** The visible row order: every top level row, each followed by its children while expanded. */
    private List<Row> flatten() {
        List<Row> visible = new ArrayList<>();
        for (Row row : rows) {
            visible.add(row);
            if (row.expanded()) visible.addAll(row.children());
        }
        return visible;
    }

    private int contentHeight() {
        int total = 4;
        for (Row row : flatten()) total += row.height();
        return total;
    }

    // Input

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (inTitleBar(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (int) mouseX - panelX;
            dragOffsetY = (int) mouseY - panelY;
            return true;
        }
        LinkIcon link = linkAt(mouseX, mouseY);
        if (link != null) {
            if (button == 0) Links.open(link.url);
            return true;
        }
        if (clickedRail(mouseX, mouseY)) return true;

        int contentX = panelX + RAIL_WIDTH;
        int contentWidth = PANEL_WIDTH - RAIL_WIDTH;
        int y = panelY + TITLE_HEIGHT + 2 - scroll;
        for (Row row : flatten()) {
            if (mouseY >= y && mouseY < y + row.height()
                && mouseX >= contentX && mouseX < contentX + contentWidth) {
                if (row.mouseClicked(mouseX, mouseY, button, contentX, y, contentWidth)) return true;
                if (button == 0 && row.expandable()) {
                    row.setExpanded(!row.expanded());
                    return true;
                }
            }
            y += row.height();
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean inTitleBar(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
            && mouseY >= panelY && mouseY < panelY + TITLE_HEIGHT;
    }

    private boolean clickedRail(double mouseX, double mouseY) {
        if (mouseX < panelX || mouseX >= panelX + RAIL_WIDTH) return false;
        int y = panelY + TITLE_HEIGHT + 2;
        for (Tab value : Tab.values()) {
            if (mouseY >= y && mouseY < y + TAB_HEIGHT) {
                tab = value;
                rebuild();
                return true;
            }
            y += TAB_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (dragging) {
            panelX = LayoutMath.clampPanelX((int) click.x() - dragOffsetX, PANEL_WIDTH, this.width);
            panelY = LayoutMath.clampPanelY((int) click.y() - dragOffsetY, PANEL_HEIGHT, this.height);
            return true;
        }
        int contentX = panelX + RAIL_WIDTH;
        for (Row row : flatten()) row.mouseDragged(click.x(), contentX, PANEL_WIDTH - RAIL_WIDTH);
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = false;
        for (Row row : flatten()) row.mouseReleased();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = LayoutMath.clampScroll(scroll - (int) (vertical * SCROLL_STEP),
            contentHeight(), PANEL_HEIGHT - TITLE_HEIGHT);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        for (Row row : flatten()) {
            if (row.capturingInput() && row.keyPressed(input.key())) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        for (Row row : flatten()) {
            if (row.capturingInput() && row.charTyped((char) input.codepoint())) return true;
        }
        return super.charTyped(input);
    }
}
