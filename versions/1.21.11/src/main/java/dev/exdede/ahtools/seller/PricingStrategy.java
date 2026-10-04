package dev.exdede.ahtools.seller;

import java.util.Random;

/**
 * Resolves one listing price from a preset. Returns null rather than guessing
 * when the preset cannot produce a valid price, and the cycle then skips that
 * tick: a bad price is a real command with real money attached, so refusing is
 * always cheaper than improvising.
 *
 * Neither mode looks at the held stack size. The sell command prices the whole
 * held stack as one listing, so multiplying by the count would list a full
 * stack for 64 times the intended price.
 */
public final class PricingStrategy {
    private PricingStrategy() {}

    public static Long resolve(Preset preset, Random rng) {
        return switch (preset.pricingMode()) {
            case FIXED -> preset.fixedPrice() > 0 ? preset.fixedPrice() : null;
            case RANDOM_RANGE -> resolveRange(preset, rng);
        };
    }

    private static Long resolveRange(Preset preset, Random rng) {
        long min = preset.minPrice();
        long max = preset.maxPrice();
        long step = preset.step();
        if (min <= 0 || step <= 0 || max < min) return null;
        long steps = (max - min) / step;
        long roll = steps <= 0 ? 0 : (long) rng.nextInt((int) Math.min(steps, Integer.MAX_VALUE - 1) + 1);
        return min + roll * step;
    }
}
