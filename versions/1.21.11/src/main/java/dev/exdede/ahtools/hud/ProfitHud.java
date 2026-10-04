package dev.exdede.ahtools.hud;

import dev.exdede.ahtools.gui.Theme;
import dev.exdede.ahtools.tracker.Money;
import dev.exdede.ahtools.tracker.StatBucket;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the profit HUD decides, with no Minecraft on the classpath, so
 * the formatting and the fades have real tests. The renderer beside this only
 * turns segments into draw calls.
 *
 * The rate is hidden for the first minute of a session on purpose: one sale
 * ten seconds in reports an hourly rate in the hundreds of millions, which
 * reads as a bug rather than as a projection.
 */
public final class ProfitHud {
    public static final long RATE_MIN_ELAPSED_MS = 60_000L;
    public static final long POP_TICKS = 8L;
    public static final long DELTA_TICKS = 40L;
    private static final float POP_PEAK = 1.10f;
    private static final int DELTA_RISE_PIXELS = 10;

    /** One drawn piece of the line. */
    public record Segment(String text, int color) {}

    /** The floating number above the line. riseY is negative, since it drifts upward. */
    public record Delta(String text, float alpha, int riseY) {}

    /** The words on the line, injected so this class stays testable without the game's translations. */
    public interface Labels {
        String sales(long count);
        String perHourSuffix();

        Labels ENGLISH = new Labels() {
            @Override public String sales(long count) { return count + (count == 1L ? " sale" : " sales"); }
            @Override public String perHourSuffix() { return "/hr"; }
        };
    }

    private final Labels labels;

    private long lastSaleTick = Long.MIN_VALUE;
    private long lastDelta;

    public ProfitHud() {
        this(Labels.ENGLISH);
    }

    public ProfitHud(Labels labels) {
        this.labels = labels;
    }

    public void onSale(long profitDelta, long tick) {
        this.lastSaleTick = tick;
        this.lastDelta = profitDelta;
    }

    public List<Segment> segments(StatBucket bucket, long elapsedMs, boolean showRate, boolean showSales) {
        List<Segment> segments = new ArrayList<>(3);
        long profit = bucket.profit();
        segments.add(new Segment(signed(profit),
            profit < 0 ? Theme.PROFIT_NEGATIVE : Theme.PROFIT_POSITIVE));

        if (showSales) {
            segments.add(new Segment(labels.sales(bucket.sales()), Theme.TEXT_MUTED));
        }
        if (showRate) {
            String rate = rateText(profit, elapsedMs, labels.perHourSuffix());
            if (rate != null) segments.add(new Segment(rate, Theme.TEXT_MUTED));
        }
        return segments;
    }

    /** @return null while the session is too young for the number to mean anything. */
    public static String rateText(long profit, long elapsedMs) {
        return rateText(profit, elapsedMs, Labels.ENGLISH.perHourSuffix());
    }

    public static String rateText(long profit, long elapsedMs, String suffix) {
        if (elapsedMs < RATE_MIN_ELAPSED_MS) return null;
        double hours = elapsedMs / 3_600_000.0;
        return Money.format(Math.round(profit / hours)) + suffix;
    }

    public float popScale(long tick) {
        long age = tick - lastSaleTick;
        if (lastSaleTick == Long.MIN_VALUE || age < 0 || age >= POP_TICKS) return 1.0f;
        float remaining = 1.0f - (float) age / POP_TICKS;
        return 1.0f + (POP_PEAK - 1.0f) * remaining;
    }

    /** @return null once the floating number has finished its rise. */
    public Delta delta(long tick) {
        long age = tick - lastSaleTick;
        if (lastSaleTick == Long.MIN_VALUE || age < 0 || age >= DELTA_TICKS) return null;
        float progress = (float) age / DELTA_TICKS;
        return new Delta(signed(lastDelta), 1.0f - progress,
            -(int) Math.round(progress * DELTA_RISE_PIXELS));
    }

    /** Money.format already signs a negative, so only a positive needs the plus. */
    private static String signed(long value) {
        String text = Money.format(value);
        return value > 0 ? "+" + text : text;
    }
}
