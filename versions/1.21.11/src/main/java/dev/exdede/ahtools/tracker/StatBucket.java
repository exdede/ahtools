package dev.exdede.ahtools.tracker;

/**
 * One set of totals. Profit is derived rather than stored, so it can never
 * drift away from the revenue and cost it is made of.
 *
 * "sales" counts listings sold, not items: one sale line is one listing, and
 * a listing may hold any number of items. "items" comes from the listing
 * confirmation the ledger remembered, and is zero for any sale the ledger
 * could not attribute.
 */
public record StatBucket(long revenue, long cost, long items, long sales) {
    public static final StatBucket EMPTY = new StatBucket(0L, 0L, 0L, 0L);

    /** Adds one sale. */
    public StatBucket plus(long addedRevenue, long addedCost, long addedItems) {
        return new StatBucket(revenue + addedRevenue, cost + addedCost, items + addedItems, sales + 1L);
    }

    public StatBucket merge(StatBucket other) {
        return new StatBucket(revenue + other.revenue, cost + other.cost,
            items + other.items, sales + other.sales);
    }

    public long profit() {
        return revenue - cost;
    }
}
