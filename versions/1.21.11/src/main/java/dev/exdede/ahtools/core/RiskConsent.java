package dev.exdede.ahtools.core;

/**
 * Whether the player has read the notice shown before the seller or the
 * spammer first runs. Bump CURRENT_VERSION when the notice text changes in a
 * way that matters, and every player sees it again before automation runs.
 */
public final class RiskConsent {
    public static final int CURRENT_VERSION = 1;

    private RiskConsent() {}

    public static boolean accepted(int storedVersion) {
        return storedVersion >= CURRENT_VERSION;
    }
}
