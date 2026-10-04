package dev.exdede.ahtools.tracker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SaleTrackerTest {
    @TempDir Path dir;

    private String activePreset;
    private long costPerItem;

    private final SaleTracker.Context context = new SaleTracker.Context() {
        @Override public String activePresetName() { return activePreset; }
        @Override public long costPerItem() { return costPerItem; }
    };

    private SaleTracker tracker(ListingLedger ledger, StatsStore stats) {
        return new SaleTracker(ledger, stats, context);
    }

    private StatsStore stats() {
        return new StatsStore(dir.resolve("stats.json"), () -> "2026-08-17");
    }

    private ListingLedger ledger() {
        return new ListingLedger(dir.resolve("ledger.json"));
    }

    @Test
    void aListingFollowedByASaleProducesExactCostAndItems() {
        ListingLedger ledger = ledger();
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger, stats);
        activePreset = "Observer";
        costPerItem = 1_500L;

        tracker.onChatLine("You listed 5 Observer for $ 20K", 1_000L);
        tracker.onChatLine("Olrun bought your Observer for $20K", 2_000L);

        StatBucket bucket = stats.lifetime("Observer");
        assertEquals(20_000L, bucket.revenue());
        assertEquals(7_500L, bucket.cost());
        assertEquals(12_500L, bucket.profit());
        assertEquals(5L, bucket.items());
        assertEquals(1L, bucket.sales());
    }

    @Test
    void listingsAreOnlyRecordedWhileASellerPresetIsActive() {
        ListingLedger ledger = ledger();
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger, stats);
        activePreset = null;

        tracker.onChatLine("You listed 5 Observer for $ 20K", 1_000L);
        assertEquals(0, ledger.size());
    }

    @Test
    void aSaleWithNoMatchingListingCountsRevenueOnly() {
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger(), stats);
        activePreset = "Observer";

        tracker.onChatLine("Olrun bought your Map for $9K", 2_000L);

        StatBucket bucket = stats.lifetime(StatsStore.UNATTRIBUTED);
        assertEquals(9_000L, bucket.revenue());
        assertEquals(0L, bucket.cost());
        assertEquals(0L, bucket.items());
        assertEquals(1L, bucket.sales());
        assertEquals(0L, stats.lifetime("Observer").revenue());
    }

    @Test
    void anAggregatePayoutIsCreditedToTheRecentPreset() {
        ListingLedger ledger = ledger();
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger, stats);
        activePreset = "Observer";
        costPerItem = 1_500L;

        tracker.onChatLine("You listed 5 Observer for $ 20K", 1_000L);
        tracker.onChatLine("You earned $23K from auction", 2_000L);

        StatBucket bucket = stats.lifetime("Observer");
        assertEquals(23_000L, bucket.revenue());
        assertEquals(0L, bucket.cost(), "an aggregate payout carries no quantity, so it must add no cost");
        assertEquals(0L, bucket.items());
    }

    @Test
    void anAggregatePayoutOutsideTheWindowIsUnattributed() {
        ListingLedger ledger = ledger();
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger, stats);
        activePreset = "Observer";

        tracker.onChatLine("You listed 5 Observer for $ 20K", 1_000L);
        tracker.onChatLine("You earned $23K from auction", 1_000L + ListingLedger.ATTRIBUTION_WINDOW_MS + 1);

        assertEquals(23_000L, stats.lifetime(StatsStore.UNATTRIBUTED).revenue());
        assertEquals(0L, stats.lifetime("Observer").revenue());
    }

    @Test
    void ordinaryChatChangesNothing() {
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger(), stats);
        assertNull(tracker.onChatLine("Olrun: anyone selling observers", 1_000L));
        assertEquals(0L, stats.lifetime(null).revenue());
    }

    @Test
    void trackingContinuesAfterTheSellerStops() {
        ListingLedger ledger = ledger();
        StatsStore stats = stats();
        SaleTracker tracker = tracker(ledger, stats);
        activePreset = "Observer";
        costPerItem = 1_500L;
        tracker.onChatLine("You listed 5 Observer for $ 20K", 1_000L);

        activePreset = null;
        tracker.onChatLine("Olrun bought your Observer for $20K", 2_000L);
        assertEquals(20_000L, stats.lifetime("Observer").revenue());
        assertEquals(7_500L, stats.lifetime("Observer").cost());
    }
}
