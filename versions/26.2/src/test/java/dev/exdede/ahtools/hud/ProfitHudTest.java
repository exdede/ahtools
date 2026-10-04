package dev.exdede.ahtools.hud;

import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.tracker.StatBucket;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfitHudTest {
    private static final long HOUR_MS = 3_600_000L;

    private StatBucket bucket(long revenue, long cost, long sales) {
        return new StatBucket(revenue, cost, 0L, sales);
    }

    @Test
    void profitIsSignedAndGreenWhenPositive() {
        ProfitHud hud = new ProfitHud();
        List<ProfitHud.Segment> segments = hud.segments(bucket(5_200_000L, 0L, 3L), HOUR_MS, false, false);

        assertEquals("+$5.2M", segments.get(0).text());
        assertEquals(Theme.PROFIT_POSITIVE, segments.get(0).color());
    }

    @Test
    void profitIsRedWhenNegative() {
        ProfitHud hud = new ProfitHud();
        List<ProfitHud.Segment> segments = hud.segments(bucket(1_000L, 500_000L, 1L), HOUR_MS, false, false);

        assertEquals("-$499K", segments.get(0).text());
        assertEquals(Theme.PROFIT_NEGATIVE, segments.get(0).color());
    }

    @Test
    void rateIsProfitPerHour() {
        assertEquals("$2M/hr", ProfitHud.rateText(1_000_000L, HOUR_MS / 2));
        assertEquals("$500K/hr", ProfitHud.rateText(1_000_000L, HOUR_MS * 2));
    }

    @Test
    void rateIsSuppressedForTheFirstMinute() {
        assertNull(ProfitHud.rateText(1_000_000L, 10_000L));
        assertNull(ProfitHud.rateText(1_000_000L, ProfitHud.RATE_MIN_ELAPSED_MS - 1));
        assertNotNull(ProfitHud.rateText(1_000_000L, ProfitHud.RATE_MIN_ELAPSED_MS));
    }

    @Test
    void aSuppressedRateIsLeftOutOfTheSegmentsEntirely() {
        ProfitHud hud = new ProfitHud();
        List<ProfitHud.Segment> early = hud.segments(bucket(1_000_000L, 0L, 1L), 5_000L, true, true);
        List<ProfitHud.Segment> later = hud.segments(bucket(1_000_000L, 0L, 1L), HOUR_MS, true, true);

        assertEquals(2, early.size());
        assertEquals(3, later.size());
    }

    @Test
    void salesSegmentCountsListingsNotItems() {
        ProfitHud hud = new ProfitHud();
        List<ProfitHud.Segment> segments = hud.segments(bucket(1L, 0L, 37L), 10_000L, false, true);

        assertEquals("37 sales", segments.get(1).text());
    }

    @Test
    void oneSaleIsSingular() {
        ProfitHud hud = new ProfitHud();
        List<ProfitHud.Segment> segments = hud.segments(bucket(1L, 0L, 1L), 10_000L, false, true);

        assertEquals("1 sale", segments.get(1).text());
    }

    @Test
    void popScaleStartsAboveOneAndDecaysBackToIt() {
        ProfitHud hud = new ProfitHud();
        hud.onSale(250_000L, 100L);

        assertTrue(hud.popScale(100L) > 1.0f);
        assertTrue(hud.popScale(104L) < hud.popScale(100L));
        assertEquals(1.0f, hud.popScale(100L + ProfitHud.POP_TICKS), 0.0001f);
        assertEquals(1.0f, hud.popScale(9999L), 0.0001f);
    }

    @Test
    void popScaleIsOneBeforeAnySale() {
        assertEquals(1.0f, new ProfitHud().popScale(0L), 0.0001f);
    }

    @Test
    void deltaFadesOutAndThenDisappears() {
        ProfitHud hud = new ProfitHud();
        hud.onSale(250_000L, 100L);

        ProfitHud.Delta fresh = hud.delta(100L);
        assertNotNull(fresh);
        assertEquals("+$250K", fresh.text());
        assertEquals(1.0f, fresh.alpha(), 0.0001f);
        assertEquals(0, fresh.riseY());

        ProfitHud.Delta halfway = hud.delta(100L + ProfitHud.DELTA_TICKS / 2);
        assertNotNull(halfway);
        assertTrue(halfway.alpha() < 1.0f);
        assertTrue(halfway.riseY() < 0);

        assertNull(hud.delta(100L + ProfitHud.DELTA_TICKS));
    }

    @Test
    void aSaleThatLosesMoneyShowsASignedNegativeDelta() {
        ProfitHud hud = new ProfitHud();
        hud.onSale(-4_000L, 10L);

        assertEquals("-$4K", hud.delta(10L).text());
    }

    @Test
    void labelsAreInjected() {
        ProfitHud.Labels labels = new ProfitHud.Labels() {
            @Override public String sales(long count) { return count + " sprzedaży"; }
            @Override public String perHourSuffix() { return "/h"; }
        };
        ProfitHud hud = new ProfitHud(labels);
        List<ProfitHud.Segment> segments = hud.segments(bucket(1_000_000L, 0L, 37L), HOUR_MS, true, true);
        assertEquals("37 sprzedaży", segments.get(1).text());
        assertEquals("$1M/h", segments.get(2).text());
    }
}
