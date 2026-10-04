package dev.exdede.ahtools.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskConsentTest {
    @Test
    void neverAcceptedIsNotAccepted() {
        assertFalse(RiskConsent.accepted(0));
    }

    @Test
    void currentVersionIsAccepted() {
        assertTrue(RiskConsent.accepted(RiskConsent.CURRENT_VERSION));
    }

    @Test
    void olderConsentIsNotAccepted() {
        assertFalse(RiskConsent.accepted(RiskConsent.CURRENT_VERSION - 1));
    }
}
