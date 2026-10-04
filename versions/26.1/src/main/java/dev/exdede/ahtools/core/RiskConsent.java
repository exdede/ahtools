package dev.exdede.ahtools.core;

/**
 * Whether the player has acknowledged that automating /ah on DonutSMP can get
 * them banned. Bump CURRENT_VERSION when the warning text changes in a way
 * that matters, and every player sees it again before automation runs.
 */
public final class RiskConsent {
    public static final int CURRENT_VERSION = 1;

    private RiskConsent() {}

    public static boolean accepted(int storedVersion) {
        return storedVersion >= CURRENT_VERSION;
    }
}
