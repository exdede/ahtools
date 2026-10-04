package dev.exdede.ahtools.seller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.config.JsonFiles;
import dev.exdede.ahtools.core.DelayRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes presets.json. Fields are read one at a time with explicit
 * defaults rather than reflected onto the record, so an older or hand edited
 * file loses at most the fields it is missing instead of failing to load at
 * all. A file that cannot be parsed is copied aside and the store starts
 * empty: losing presets is annoying, refusing to start the mod is worse. A file
 * from a newer build is read but never saved over (see JsonFiles).
 */
public final class PresetStore {
    private final Path file;
    private final List<Preset> presets = new ArrayList<>();
    private String selectedName = "";
    /** Set when the file came from a newer build, or was unreadable and could not be copied aside. */
    private boolean readOnly;

    public PresetStore(Path file) {
        this.file = file;
    }

    public List<Preset> presets() { return List.copyOf(presets); }
    public String selectedName() { return selectedName; }

    public Preset selected() {
        return find(selectedName);
    }

    /** The preset with exactly this name, or null. */
    public Preset find(String name) {
        for (Preset preset : presets) {
            if (preset.name().equals(name)) return preset;
        }
        return null;
    }

    public void select(String name) {
        this.selectedName = name == null ? "" : name;
    }

    /** Adds the preset, replacing any existing one with the same name. Loading and tests only; the GUI uses add and rename. */
    public void put(Preset preset) {
        presets.removeIf(p -> p.name().equals(preset.name()));
        presets.add(preset);
    }

    /**
     * Adds a new preset. A name that is already taken is refused rather than
     * overwritten: replacing a tuned preset with defaults because of a typo
     * in the add field cannot be undone.
     *
     * @return false when the name is taken.
     */
    public boolean add(Preset preset) {
        if (find(preset.name()) != null) return false;
        presets.add(preset);
        return true;
    }

    /**
     * Stores an edited preset that used to be called oldName, which also
     * covers an edit that keeps the name. The selection follows a rename.
     *
     * @return false when the new name belongs to a different preset.
     */
    public boolean rename(String oldName, Preset edited) {
        if (!edited.name().equals(oldName) && find(edited.name()) != null) return false;
        int index = -1;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).name().equals(oldName)) index = i;
        }
        if (index < 0) presets.add(edited);
        else presets.set(index, edited);
        if (selectedName.equals(oldName)) selectedName = edited.name();
        return true;
    }

    public void remove(String name) {
        presets.removeIf(p -> p.name().equals(name));
        if (selectedName.equals(name)) selectedName = "";
    }

    public void load() {
        presets.clear();
        selectedName = "";
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
            if (object.has("selected")) selectedName = object.get("selected").getAsString();
            if (!object.has("presets")) return;
            JsonArray array = object.getAsJsonArray("presets");
            for (JsonElement element : array) {
                if (element.isJsonObject()) presets.add(readPreset(element.getAsJsonObject()));
            }
        }
        catch (RuntimeException | IOException e) {
            AhToolsMod.LOGGER.warn("failed to read {}, starting with no presets", file, e);
            presets.clear();
            selectedName = "";
            readOnly |= !JsonFiles.backupCorrupt(file);
        }
    }

    public void save() {
        if (readOnly) return;
        JsonObject root = new JsonObject();
        JsonFiles.stamp(root);
        root.addProperty("selected", selectedName);
        JsonArray array = new JsonArray();
        for (Preset preset : presets) array.add(writePreset(preset));
        root.add("presets", array);
        try {
            JsonFiles.writeAtomic(file, root.toString());
        }
        catch (IOException e) {
            AhToolsMod.LOGGER.warn("failed to write {}", file, e);
        }
    }

    private static Preset readPreset(JsonObject o) {
        Preset base = Preset.defaults(string(o, "name", "unnamed"));
        return new Preset(
            base.name(),
            string(o, "expectedItem", base.expectedItem()),
            enumValue(Preset.PricingMode.class, string(o, "pricingMode", base.pricingMode().name()), base.pricingMode()),
            number(o, "fixedPrice", base.fixedPrice()),
            number(o, "minPrice", base.minPrice()),
            number(o, "maxPrice", base.maxPrice()),
            number(o, "step", base.step()),
            number(o, "maxPriceCap", base.maxPriceCap()),
            enumValue(DelayRule.Mode.class, string(o, "delayMode", base.delayMode().name()), base.delayMode()),
            (int) number(o, "fixedDelayTicks", base.fixedDelayTicks()),
            (int) number(o, "minDelayTicks", base.minDelayTicks()),
            (int) number(o, "maxDelayTicks", base.maxDelayTicks()),
            number(o, "costPerItem", base.costPerItem()),
            bool(o, "validateHotbar", base.validateHotbar()));
    }

    private static JsonObject writePreset(Preset p) {
        JsonObject o = new JsonObject();
        o.addProperty("name", p.name());
        o.addProperty("expectedItem", p.expectedItem());
        o.addProperty("pricingMode", p.pricingMode().name());
        o.addProperty("fixedPrice", p.fixedPrice());
        o.addProperty("minPrice", p.minPrice());
        o.addProperty("maxPrice", p.maxPrice());
        o.addProperty("step", p.step());
        o.addProperty("maxPriceCap", p.maxPriceCap());
        o.addProperty("delayMode", p.delayMode().name());
        o.addProperty("fixedDelayTicks", p.fixedDelayTicks());
        o.addProperty("minDelayTicks", p.minDelayTicks());
        o.addProperty("maxDelayTicks", p.maxDelayTicks());
        o.addProperty("costPerItem", p.costPerItem());
        o.addProperty("validateHotbar", p.validateHotbar());
        return o;
    }

    private static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback;
    }

    private static long number(JsonObject o, String key, long fallback) {
        try {
            return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsLong() : fallback;
        }
        catch (RuntimeException e) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        try {
            return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsBoolean() : fallback;
        }
        catch (RuntimeException e) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, E fallback) {
        try {
            return Enum.valueOf(type, raw);
        }
        catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
