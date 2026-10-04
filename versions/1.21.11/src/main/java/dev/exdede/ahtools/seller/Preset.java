package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.core.DelayRule;

/**
 * One named seller configuration: what to sell, how to price it, how fast to
 * send, and what each item cost to obtain.
 *
 * Prices are the total for a whole listing, never per item, because the sell
 * command prices whatever is in hand as one listing. A 64 stack and a single
 * item both list for exactly the configured price.
 *
 * costPerItem is the only per-item figure here, and it exists purely so the
 * tracker can turn a sale into a profit figure.
 */
public record Preset(
    String name,
    String expectedItem,
    PricingMode pricingMode,
    long fixedPrice,
    long minPrice,
    long maxPrice,
    long step,
    long maxPriceCap,
    DelayRule.Mode delayMode,
    int fixedDelayTicks,
    int minDelayTicks,
    int maxDelayTicks,
    long costPerItem,
    boolean validateHotbar
) {
    public enum PricingMode { FIXED, RANDOM_RANGE }

    /** A last-resort ceiling every preset starts with, independent of any price the user types. */
    public static final long DEFAULT_MAX_PRICE_CAP = 100_000_000L;

    public static Preset defaults(String name) {
        return new Preset(
            name, "", PricingMode.FIXED,
            10_000L, 3_000L, 20_000L, 1_000L, DEFAULT_MAX_PRICE_CAP,
            DelayRule.Mode.FIXED, 25, 25, 40, 0L, false);
    }

    public DelayRule delayRule() {
        return delayMode == DelayRule.Mode.RANDOM
            ? DelayRule.random(minDelayTicks, maxDelayTicks)
            : DelayRule.fixed(fixedDelayTicks);
    }

    public Preset withName(String newName) {
        return new Preset(newName, expectedItem, pricingMode, fixedPrice, minPrice, maxPrice, step,
            maxPriceCap, delayMode, fixedDelayTicks, minDelayTicks, maxDelayTicks, costPerItem, validateHotbar);
    }
}
