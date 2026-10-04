package dev.exdede.ahtools.gui;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.core.RiskConsent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * Shown the first time the player tries to start the seller or the spammer.
 * Automation stays refused (AutomationRunner) until "I understand" is clicked,
 * so this screen is the explanation, not the enforcement.
 *
 * Accepting returns to whatever screen was open and then runs the action the
 * player originally asked for, so they do not have to press the key twice.
 */
public class RiskWarningScreen extends Screen {
    private static final int TEXT_WIDTH = 300;
    private static final int LINE_HEIGHT = 11;

    private final Screen parent;
    private final Runnable onAccept;

    public RiskWarningScreen(Screen parent, Runnable onAccept) {
        super(Text.translatable("ahtools.risk.title"));
        this.parent = parent;
        this.onAccept = onAccept;
    }

    @Override
    protected void init() {
        int y = this.height / 2 + 60;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ahtools.risk.accept"), button -> accept())
            .dimensions(this.width / 2 - 154, y, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ahtools.risk.cancel"), button -> close())
            .dimensions(this.width / 2 + 4, y, 150, 20).build());
    }

    private void accept() {
        Configs.General.RISK_ACCEPTED_VERSION.set(RiskConsent.CURRENT_VERSION);
        Configs.saveToFile();
        this.client.setScreen(parent);
        onAccept.run();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, Theme.OVERLAY_DIM);
        super.render(context, mouseX, mouseY, delta);
        int y = this.height / 2 - 90;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, y, Theme.PROFIT_NEGATIVE);
        y += 20;
        for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("ahtools.risk.body"), TEXT_WIDTH)) {
            context.drawCenteredTextWithShadow(this.textRenderer, line, this.width / 2, y, Theme.TEXT);
            y += LINE_HEIGHT;
        }
    }
}
