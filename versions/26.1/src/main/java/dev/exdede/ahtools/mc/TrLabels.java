package dev.exdede.ahtools.mc;

import dev.exdede.ahtools.hud.PluralForm;
import dev.exdede.ahtools.hud.ProfitHud;

/** The HUD's words from the lang files, with the plural form picked by count. */
public final class TrLabels implements ProfitHud.Labels {
    @Override
    public String sales(long count) {
        String key = switch (PluralForm.of(count)) {
            case ONE -> "ahtools.hud.sales.one";
            case FEW -> "ahtools.hud.sales.few";
            case MANY -> "ahtools.hud.sales.many";
        };
        return Tr.t(key, count);
    }

    @Override
    public String perHourSuffix() {
        return Tr.t("ahtools.hud.per_hour");
    }
}
