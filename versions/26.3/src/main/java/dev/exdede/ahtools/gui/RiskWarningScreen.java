package dev.exdede.ahtools.gui;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.core.RiskConsent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

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
        super(Component.translatable("ahtools.risk.title"));
        this.parent = parent;
        this.onAccept = onAccept;
    }

    @Override
    protected void init() {
        int y = this.height / 2 + 60;
        addRenderableWidget(Button.builder(Component.translatable("ahtools.risk.accept"), button -> accept())
            .bounds(this.width / 2 - 154, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ahtools.risk.cancel"), button -> onClose())
            .bounds(this.width / 2 + 4, y, 150, 20).build());
    }

    private void accept() {
        Configs.General.RISK_ACCEPTED_VERSION.set(RiskConsent.CURRENT_VERSION);
        Configs.saveToFile();
        this.minecraft.gui.setScreen(parent);
        onAccept.run();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, Theme.OVERLAY_DIM);
        super.extractRenderState(context, mouseX, mouseY, delta);
        int y = this.height / 2 - 90;
        context.centeredText(this.font, this.title, this.width / 2, y, Theme.PROFIT_NEGATIVE);
        y += 20;
        for (FormattedCharSequence line : this.font.split(Component.translatable("ahtools.risk.body"), TEXT_WIDTH)) {
            context.centeredText(this.font, line, this.width / 2, y, Theme.TEXT);
            y += LINE_HEIGHT;
        }
    }
}
