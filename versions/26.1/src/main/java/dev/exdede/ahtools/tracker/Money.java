package dev.exdede.ahtools.tracker;

/**
 * Parses and renders money strings in integer units. Money never touches a
 * float outside this class: every stored figure is a whole-unit long.
 *
 * The accepted grammar is the one the game's own chat and tooltips use: an
 * optional leading "~" (an estimate marker), an optional "$" with optional
 * spacing, comma group separators, an optional decimal part, and an optional
 * K/M/B suffix.
 */
public final class Money {
    private Money() {}

    public static long parse(String raw) {
        Long value = parseOrNull(raw);
        if (value == null) throw new IllegalArgumentException("not a money value: " + raw);
        return value;
    }

    /** Same as {@link #parse} but returns null instead of throwing, for chat lines that may not be money at all. */
    public static Long parseOrNull(String raw) {
        if (raw == null) return null;
        String s = raw.replace("~", "").replace("$", "").replace(",", "").replace(" ", "").trim();
        if (s.isEmpty()) return null;

        long multiplier = 1L;
        char last = s.charAt(s.length() - 1);
        switch (last) {
            case 'k', 'K' -> multiplier = 1_000L;
            case 'm', 'M' -> multiplier = 1_000_000L;
            case 'b', 'B' -> multiplier = 1_000_000_000L;
            default -> { }
        }
        if (multiplier > 1L) s = s.substring(0, s.length() - 1).trim();
        if (s.isEmpty()) return null;

        try {
            if (multiplier > 1L || s.indexOf('.') >= 0) {
                return Math.round(Double.parseDouble(s) * multiplier);
            }
            return Long.parseLong(s);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Renders a value the way chat does: one decimal place of K/M/B shorthand,
     * with a trailing ".0" trimmed. Display only; never round-trip a formatted
     * value back into stored state, since the shorthand loses precision.
     */
    public static String format(long value) {
        String sign = value < 0 ? "-" : "";
        long abs = Math.abs(value);
        if (abs >= 1_000_000_000L) return sign + "$" + trim(abs / 1_000_000_000.0) + "B";
        if (abs >= 1_000_000L) return sign + "$" + trim(abs / 1_000_000.0) + "M";
        if (abs >= 1_000L) return sign + "$" + trim(abs / 1_000.0) + "K";
        return sign + "$" + abs;
    }

    private static String trim(double scaled) {
        String s = String.format(java.util.Locale.ROOT, "%.1f", scaled);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
