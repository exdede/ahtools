package dev.exdede.ahtools.config.gui;

import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.seller.Preset;
import dev.exdede.ahtools.tracker.Money;

/**
 * One editable preset field, kept out of the screen class so the parsing and
 * clamping rules have real tests. The screen only decides which kind to open a
 * dialog for.
 *
 * Money fields accept whatever the game itself prints ("5k", "$25.9K",
 * "1,500"), because that is the form the player is reading prices in. Delays
 * clamp rather than refuse: a too-small delay is a mistake with an obvious
 * correct answer, while a price typo has none.
 */
public final class PresetField {
    public enum Kind {
        NAME, EXPECTED_ITEM, PRICING_MODE, FIXED_PRICE, MIN_PRICE, MAX_PRICE, STEP,
        MAX_PRICE_CAP, DELAY_MODE, FIXED_DELAY, MIN_DELAY, MAX_DELAY, COST_PER_ITEM, VALIDATE_HOTBAR
    }

    private PresetField() {}

    /** True when this kind toggles on click instead of opening a text dialog. */
    public static boolean isToggle(Kind kind) {
        return kind == Kind.PRICING_MODE || kind == Kind.DELAY_MODE || kind == Kind.VALIDATE_HOTBAR;
    }

    /** The lang key for this field's label, e.g. "ahtools.preset.fixed_price". */
    public static String labelKey(Kind kind) {
        return "ahtools.preset." + kind.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The lang key for this field's hover tooltip, e.g. "ahtools.tip.preset.fixed_price". */
    public static String tooltipKey(Kind kind) {
        return "ahtools.tip.preset." + kind.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static String display(Preset p, Kind kind) {
        return switch (kind) {
            case NAME -> p.name();
            case EXPECTED_ITEM -> p.expectedItem().isBlank() ? "any" : p.expectedItem();
            case PRICING_MODE -> p.pricingMode().name();
            case FIXED_PRICE -> Money.format(p.fixedPrice());
            case MIN_PRICE -> Money.format(p.minPrice());
            case MAX_PRICE -> Money.format(p.maxPrice());
            case STEP -> Money.format(p.step());
            case MAX_PRICE_CAP -> Money.format(p.maxPriceCap());
            case DELAY_MODE -> p.delayMode().name();
            case FIXED_DELAY -> String.valueOf(p.fixedDelayTicks());
            case MIN_DELAY -> String.valueOf(p.minDelayTicks());
            case MAX_DELAY -> String.valueOf(p.maxDelayTicks());
            case COST_PER_ITEM -> Money.format(p.costPerItem());
            case VALIDATE_HOTBAR -> p.validateHotbar() ? "on" : "off";
        };
    }

    /** @throws IllegalArgumentException when the text cannot become a usable value. */
    public static Preset apply(Preset p, Kind kind, String raw) {
        return switch (kind) {
            case NAME -> {
                String name = raw == null ? "" : raw.trim();
                if (name.isEmpty()) throw new IllegalArgumentException("a preset needs a name");
                yield p.withName(name);
            }
            case EXPECTED_ITEM -> rebuild(p, p.name(), raw == null ? "" : raw.trim(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case PRICING_MODE -> rebuild(p, p.name(), p.expectedItem(),
                p.pricingMode() == Preset.PricingMode.FIXED
                    ? Preset.PricingMode.RANDOM_RANGE : Preset.PricingMode.FIXED,
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case DELAY_MODE -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode() == DelayRule.Mode.FIXED ? DelayRule.Mode.RANDOM : DelayRule.Mode.FIXED,
                p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case VALIDATE_HOTBAR -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), !p.validateHotbar());
            case FIXED_PRICE -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                money(raw), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case MIN_PRICE -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), money(raw), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case MAX_PRICE -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), money(raw), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case STEP -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), money(raw), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case MAX_PRICE_CAP -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), money(raw),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case COST_PER_ITEM -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), p.maxDelayTicks(),
                money(raw), p.validateHotbar());
            case FIXED_DELAY -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), ticks(raw), p.minDelayTicks(), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case MIN_DELAY -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), ticks(raw), p.maxDelayTicks(),
                p.costPerItem(), p.validateHotbar());
            case MAX_DELAY -> rebuild(p, p.name(), p.expectedItem(), p.pricingMode(),
                p.fixedPrice(), p.minPrice(), p.maxPrice(), p.step(), p.maxPriceCap(),
                p.delayMode(), p.fixedDelayTicks(), p.minDelayTicks(), ticks(raw),
                p.costPerItem(), p.validateHotbar());
        };
    }

    private static long money(String raw) {
        Long value = Money.parseOrNull(raw);
        if (value == null || value < 0) throw new IllegalArgumentException("not a usable amount: " + raw);
        return value;
    }

    private static int ticks(String raw) {
        Long value = Money.parseOrNull(raw);
        if (value == null) throw new IllegalArgumentException("not a tick count: " + raw);
        return Math.max(DelayRule.MIN_DELAY_TICKS, (int) Math.min(value, 1200L));
    }

    private static Preset rebuild(Preset ignored, String name, String expectedItem, Preset.PricingMode pricingMode,
                                  long fixedPrice, long minPrice, long maxPrice, long step, long maxPriceCap,
                                  DelayRule.Mode delayMode, int fixedDelay, int minDelay, int maxDelay,
                                  long costPerItem, boolean validateHotbar) {
        return new Preset(name, expectedItem, pricingMode, fixedPrice, minPrice, maxPrice, step,
            maxPriceCap, delayMode, fixedDelay, minDelay, maxDelay, costPerItem, validateHotbar);
    }
}
