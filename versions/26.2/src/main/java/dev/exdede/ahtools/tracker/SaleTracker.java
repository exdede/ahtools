package dev.exdede.ahtools.tracker;

/**
 * Turns one chat line into ledger and statistics updates.
 *
 * This runs on every chat line for as long as the mod is loaded, whether or
 * not the seller is running, because most listings sell long after they were
 * posted. Watching chat only while a sell is in flight would miss nearly
 * everything that actually sells.
 *
 * What each line can and cannot fund:
 *   listing line  -> remembers quantity, preset and cost, nothing counted yet
 *   sale line     -> exact revenue, plus exact cost and items when the ledger
 *                    still holds the matching listing
 *   aggregate line-> revenue only, credited to the recently active preset;
 *                    it names no item and carries no count, so cost stays zero
 * A sale the ledger cannot match funds revenue only and lands under
 * UNATTRIBUTED, so the size of the gap in cost data stays visible instead of
 * quietly inflating profit.
 */
public final class SaleTracker {
    /** What the tracker needs to know about the seller without depending on it. */
    public interface Context {
        /** The running seller's preset name, or null when the seller is not running. */
        String activePresetName();

        /** The running preset's cost per item; ignored when there is no active preset. */
        long costPerItem();
    }

    private final ListingLedger ledger;
    private final StatsStore stats;
    private final Context context;

    public SaleTracker(ListingLedger ledger, StatsStore stats, Context context) {
        this.ledger = ledger;
        this.stats = stats;
        this.context = context;
    }

    /** @return the parsed line when it was a money line, otherwise null. */
    public SaleParser.ParsedLine onChatLine(String line, long nowMs) {
        SaleParser.ParsedLine parsed = SaleParser.parse(line);
        if (parsed == null) return null;

        switch (parsed.kind()) {
            case LISTED -> {
                String preset = context.activePresetName();
                // Only a listing this mod posted can be costed, so a listing
                // made by hand is deliberately not remembered.
                if (preset != null && !preset.isBlank()) {
                    ledger.record(parsed.itemName(), parsed.quantity(), preset, context.costPerItem(), nowMs);
                }
            }
            case SALE -> {
                ListingLedger.Entry entry = ledger.claim(parsed.itemName(), nowMs);
                if (entry == null) {
                    stats.recordSale(StatsStore.UNATTRIBUTED, parsed.money(), 0L, 0L);
                }
                else {
                    stats.recordSale(entry.presetName(), parsed.money(),
                        entry.costPerItem() * entry.quantity(), entry.quantity());
                }
            }
            case AUCTION -> {
                String preset = ledger.lastPresetWithin(ListingLedger.ATTRIBUTION_WINDOW_MS, nowMs);
                stats.recordSale(preset == null ? StatsStore.UNATTRIBUTED : preset, parsed.money(), 0L, 0L);
            }
        }
        return parsed;
    }
}
