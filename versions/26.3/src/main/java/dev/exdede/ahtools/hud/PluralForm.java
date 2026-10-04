package dev.exdede.ahtools.hud;

/**
 * Plural category for a count, by the Polish rule, which is the strictest of
 * the shipped languages. English lang files simply give FEW and MANY the same
 * text.
 */
public enum PluralForm {
    ONE, FEW, MANY;

    public static PluralForm of(long n) {
        long abs = Math.abs(n);
        if (abs == 1L) return ONE;
        long lastDigit = abs % 10L;
        long lastTwo = abs % 100L;
        if (lastDigit >= 2L && lastDigit <= 4L && (lastTwo < 12L || lastTwo > 14L)) return FEW;
        return MANY;
    }
}
