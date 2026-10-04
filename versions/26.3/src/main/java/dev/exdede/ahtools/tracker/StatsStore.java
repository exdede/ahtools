package dev.exdede.ahtools.tracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.config.JsonFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Revenue, cost, items and sales across three horizons and any number of
 * presets. Session lives in memory only; today and lifetime persist.
 *
 * The day is supplied rather than read from the clock so the rollover is
 * testable. Rollover is checked lazily on every read and write, which means a
 * client left running past midnight moves to the new day the moment anything
 * touches the stats rather than at some scheduled instant.
 */
public final class StatsStore {
    /** Where a sale with no matching listing goes, so the gap in cost data stays visible. */
    public static final String UNATTRIBUTED = "(unattributed)";

    private final Path file;
    private final Supplier<String> todaySupplier;

    private final Map<String, StatBucket> session = new HashMap<>();
    private final Map<String, StatBucket> today = new HashMap<>();
    private final Map<String, StatBucket> lifetime = new HashMap<>();
    private String day = "";
    private boolean dirty;
    /** Set when the file came from a newer build, or was unreadable and could not be copied aside. */
    private boolean readOnly;

    public StatsStore(Path file, Supplier<String> todaySupplier) {
        this.file = file;
        this.todaySupplier = todaySupplier;
        this.day = todaySupplier.get();
    }

    public boolean dirty() { return dirty; }

    public void recordSale(String presetName, long revenue, long cost, long items) {
        String key = presetName == null || presetName.isBlank() ? UNATTRIBUTED : presetName;
        rollOverIfNeeded();
        session.merge(key, StatBucket.EMPTY.plus(revenue, cost, items),
            (existing, added) -> existing.merge(added));
        today.merge(key, StatBucket.EMPTY.plus(revenue, cost, items),
            (existing, added) -> existing.merge(added));
        lifetime.merge(key, StatBucket.EMPTY.plus(revenue, cost, items),
            (existing, added) -> existing.merge(added));
        dirty = true;
    }

    /** A null or blank preset name returns the total across every preset. */
    public StatBucket session(String presetName) { return read(session, presetName); }
    public StatBucket today(String presetName) { rollOverIfNeeded(); return read(today, presetName); }
    public StatBucket lifetime(String presetName) { return read(lifetime, presetName); }

    private StatBucket read(Map<String, StatBucket> map, String presetName) {
        if (presetName == null || presetName.isBlank()) {
            StatBucket total = StatBucket.EMPTY;
            for (StatBucket bucket : map.values()) total = total.merge(bucket);
            return total;
        }
        return map.getOrDefault(presetName, StatBucket.EMPTY);
    }

    /** Every preset name any horizon has seen, sorted, so the GUI can list them. */
    public List<String> presetNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(lifetime.keySet());
        names.addAll(today.keySet());
        names.addAll(session.keySet());
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String::compareTo);
        return sorted;
    }

    private void rollOverIfNeeded() {
        String current = todaySupplier.get();
        if (current.equals(day)) return;
        day = current;
        today.clear();
        dirty = true;
    }

    public void resetSession() {
        session.clear();
    }

    public void resetToday() {
        today.clear();
        day = todaySupplier.get();
        dirty = true;
    }

    public void resetPreset(String presetName) {
        session.remove(presetName);
        today.remove(presetName);
        lifetime.remove(presetName);
        dirty = true;
    }

    /**
     * Clears everything, including lifetime. A timestamped copy of the file is
     * written first: lifetime totals are the one thing here that cannot be
     * rebuilt from anywhere, so an accidental click must stay recoverable.
     */
    public void resetLifetime() {
        backup();
        session.clear();
        today.clear();
        lifetime.clear();
        day = todaySupplier.get();
        dirty = true;
        save();
    }

    private void backup() {
        if (!Files.isRegularFile(file)) return;
        Path backup = file.resolveSibling("stats-backup-" + System.currentTimeMillis() + ".json");
        try {
            Files.copy(file, backup);
            AhToolsMod.LOGGER.info("wrote a statistics backup to {}", backup);
        }
        catch (IOException e) {
            AhToolsMod.LOGGER.warn("failed to write the statistics backup {}", backup, e);
        }
    }

    public void load() {
        session.clear();
        today.clear();
        lifetime.clear();
        readOnly = false;
        day = todaySupplier.get();
        if (!Files.isRegularFile(file)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) throw new IllegalStateException("not a JSON object");
            JsonObject object = root.getAsJsonObject();
            if (JsonFiles.isTooNew(object)) {
                AhToolsMod.LOGGER.warn("{} was written by a newer AHTools, reading it but never saving over it", file);
                readOnly = true;
            }
            String storedDay = object.has("day") ? object.get("day").getAsString() : "";
            if (object.has("lifetime")) readInto(object.getAsJsonObject("lifetime"), lifetime);
            // Yesterday's totals are not today's. Only restore the daily map
            // when the stored day is still the current one.
            if (storedDay.equals(day) && object.has("today")) readInto(object.getAsJsonObject("today"), today);
            dirty = false;
        }
        catch (RuntimeException | IOException e) {
            AhToolsMod.LOGGER.warn("failed to read {}, starting with empty statistics", file, e);
            today.clear();
            lifetime.clear();
            readOnly |= !JsonFiles.backupCorrupt(file);
        }
    }

    private static void readInto(JsonObject source, Map<String, StatBucket> target) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject bucket = entry.getValue().getAsJsonObject();
            target.put(entry.getKey(), new StatBucket(
                bucket.get("revenue").getAsLong(),
                bucket.get("cost").getAsLong(),
                bucket.get("items").getAsLong(),
                bucket.get("sales").getAsLong()));
        }
    }

    public void save() {
        if (readOnly) return;
        JsonObject root = new JsonObject();
        JsonFiles.stamp(root);
        root.addProperty("day", day);
        root.add("today", write(today));
        root.add("lifetime", write(lifetime));
        try {
            JsonFiles.writeAtomic(file, root.toString());
            dirty = false;
        }
        catch (IOException e) {
            AhToolsMod.LOGGER.warn("failed to write {}", file, e);
        }
    }

    private static JsonObject write(Map<String, StatBucket> source) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, StatBucket> entry : source.entrySet()) {
            JsonObject bucket = new JsonObject();
            bucket.addProperty("revenue", entry.getValue().revenue());
            bucket.addProperty("cost", entry.getValue().cost());
            bucket.addProperty("items", entry.getValue().items());
            bucket.addProperty("sales", entry.getValue().sales());
            object.add(entry.getKey(), bucket);
        }
        return object;
    }
}
