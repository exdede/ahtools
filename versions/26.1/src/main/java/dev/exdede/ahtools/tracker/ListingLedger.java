package dev.exdede.ahtools.tracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.config.JsonFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Remembers what was listed so a later sale line can be turned into a real
 * quantity and a real cost.
 *
 * This exists because of one structural fact about the chat: a sale line
 * carries no quantity. "bought your Map for $15K" is one listing sold at its
 * listed total, and that listing may have held 1 item or 64. The listing
 * confirmation line is the only place the count ever appears, so it has to be
 * captured when it happens and matched up later.
 *
 * Matching is FIFO by item display name, which is what both lines carry. Names
 * are compared case and space insensitively but never resolved against a
 * registry: the server names items however it likes, and an unrecognized name
 * should still count under its own name rather than be dropped.
 *
 * An entry that never sells would otherwise sit here forever and eventually
 * mis-match a much later sale of the same item, hence the expiry.
 */
public final class ListingLedger {
    /** How long an unsold listing stays claimable. */
    public static final long MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    /** How far back an unattributed aggregate payout looks for a preset to credit. */
    public static final long ATTRIBUTION_WINDOW_MS = 30L * 60L * 1000L;

    public record Entry(String itemName, int quantity, String presetName, long costPerItem, long listedAtMs) {}

    private final Path file;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private String lastPresetName = "";
    private long lastListedAtMs = Long.MIN_VALUE / 2;
    /** Set when the file came from a newer build, or was unreadable and could not be copied aside. */
    private boolean readOnly;

    public ListingLedger(Path file) {
        this.file = file;
    }

    public int size() { return entries.size(); }

    public void record(String itemName, int quantity, String presetName, long costPerItem, long nowMs) {
        pruneExpired(nowMs);
        entries.addLast(new Entry(itemName, quantity, presetName, costPerItem, nowMs));
        lastPresetName = presetName == null ? "" : presetName;
        lastListedAtMs = nowMs;
    }

    /** Removes and returns the oldest unsold listing of this item, or null when there is none. */
    public Entry claim(String itemName, long nowMs) {
        pruneExpired(nowMs);
        String wanted = normalize(itemName);
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) {
            Entry entry = it.next();
            if (normalize(entry.itemName()).equals(wanted)) {
                it.remove();
                return entry;
            }
        }
        return null;
    }

    /**
     * The preset that most recently listed anything inside the window, or null.
     * Tracked separately from the entry list so claiming an entry does not
     * erase the attribution an aggregate payout needs moments later.
     */
    public String lastPresetWithin(long windowMs, long nowMs) {
        if (lastPresetName.isEmpty()) return null;
        return nowMs - lastListedAtMs <= windowMs ? lastPresetName : null;
    }

    public void pruneExpired(long nowMs) {
        entries.removeIf(entry -> nowMs - entry.listedAtMs() > MAX_AGE_MS);
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public void load() {
        entries.clear();
        lastPresetName = "";
        lastListedAtMs = Long.MIN_VALUE / 2;
        readOnly = false;
        if (!Files.isRegularFile(file)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) throw new IllegalStateException("not a JSON object");
            JsonObject object = root.getAsJsonObject();
            if (JsonFiles.isTooNew(object)) {
                AhToolsMod.LOGGER.warn("{} was written by a newer AHTools, reading it but never saving over it", file);
                readOnly = true;
            }
            if (object.has("lastPreset")) lastPresetName = object.get("lastPreset").getAsString();
            if (object.has("lastListedAt")) lastListedAtMs = object.get("lastListedAt").getAsLong();
            if (!object.has("entries")) return;
            for (JsonElement element : object.getAsJsonArray("entries")) {
                if (!element.isJsonObject()) continue;
                JsonObject e = element.getAsJsonObject();
                entries.addLast(new Entry(
                    e.get("itemName").getAsString(),
                    e.get("quantity").getAsInt(),
                    e.get("presetName").getAsString(),
                    e.get("costPerItem").getAsLong(),
                    e.get("listedAt").getAsLong()));
            }
        }
        catch (RuntimeException | IOException e) {
            AhToolsMod.LOGGER.warn("failed to read {}, starting with an empty ledger", file, e);
            entries.clear();
            readOnly |= !JsonFiles.backupCorrupt(file);
        }
    }

    public void save() {
        if (readOnly) return;
        JsonObject root = new JsonObject();
        JsonFiles.stamp(root);
        root.addProperty("lastPreset", lastPresetName);
        root.addProperty("lastListedAt", lastListedAtMs);
        JsonArray array = new JsonArray();
        for (Entry entry : entries) {
            JsonObject e = new JsonObject();
            e.addProperty("itemName", entry.itemName());
            e.addProperty("quantity", entry.quantity());
            e.addProperty("presetName", entry.presetName());
            e.addProperty("costPerItem", entry.costPerItem());
            e.addProperty("listedAt", entry.listedAtMs());
            array.add(e);
        }
        root.add("entries", array);
        try {
            JsonFiles.writeAtomic(file, root.toString());
        }
        catch (IOException e) {
            AhToolsMod.LOGGER.warn("failed to write {}", file, e);
        }
    }
}
