package dev.exdede.ahtools.seller;

/**
 * The one place in this mod that builds a sell command string. Price is a long
 * rather than text, so no untrusted string can ever reach a command: only an
 * in-range integer gets through, and the output is exactly "/ah sell <price>"
 * with no other tokens.
 *
 * Both bounds matter. The preset cap is the user's own ceiling for that item;
 * the absolute ceiling is the backstop for a cap that was itself mis-typed.
 */
public final class SellCommand {
    /** Last resort ceiling, independent of any per-preset cap. */
    public static final long ABSOLUTE_MAX_PRICE = 1_000_000_000_000L;

    private SellCommand() {}

    public static String sell(long price, long maxPriceCap) {
        if (price <= 0) {
            throw new IllegalArgumentException("price must be positive: " + price);
        }
        if (price > maxPriceCap) {
            throw new IllegalArgumentException("price " + price + " exceeds the preset cap " + maxPriceCap);
        }
        if (price > ABSOLUTE_MAX_PRICE) {
            throw new IllegalArgumentException("price " + price + " exceeds the absolute ceiling " + ABSOLUTE_MAX_PRICE);
        }
        return "/ah sell " + price;
    }
}
