package dev.exdede.ahtools.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.exdede.ahtools.AhToolsMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes the settings file: one JSON object per named section, the
 * same shape pre-1.0 builds wrote, so an existing ahtools.json loads as is.
 * Sections the caller does not know about are read and dropped, which is what
 * retires the old "Hotkeys" object now that hotkeys are vanilla key bindings.
 *
 * Same file rules as every other store (see JsonFiles): atomic writes, a
 * schemaVersion at the root, a file from a newer build never saved over, an
 * unreadable one copied aside first. The class is static, so the "never save
 * over" state is kept per path.
 */
public final class SettingsFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<Path> READ_ONLY = ConcurrentHashMap.newKeySet();

    private SettingsFile() {}

    public static void load(Path file, Map<String, List<Setting<?>>> sections) {
        Path key = file.toAbsolutePath();
        READ_ONLY.remove(key);
        if (!Files.isRegularFile(file)) return;
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalStateException("not a JSON object");
            root = parsed.getAsJsonObject();
        }
        catch (IOException | RuntimeException e) {
            AhToolsMod.LOGGER.warn("failed to read {}, using defaults", file, e);
            if (!JsonFiles.backupCorrupt(file)) READ_ONLY.add(key);
            return;
        }
        if (JsonFiles.isTooNew(root)) {
            AhToolsMod.LOGGER.warn("{} was written by a newer AHTools, reading it but never saving over it", file);
            READ_ONLY.add(key);
        }

        for (Map.Entry<String, List<Setting<?>>> section : sections.entrySet()) {
            JsonElement element = root.get(section.getKey());
            if (element == null || !element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            for (Setting<?> setting : section.getValue()) {
                setting.fromJson(object.get(setting.name()));
            }
        }
    }

    public static void save(Path file, Map<String, List<Setting<?>>> sections) {
        if (READ_ONLY.contains(file.toAbsolutePath())) return;
        JsonObject root = new JsonObject();
        JsonFiles.stamp(root);
        for (Map.Entry<String, List<Setting<?>>> section : sections.entrySet()) {
            JsonObject object = new JsonObject();
            for (Setting<?> setting : section.getValue()) {
                object.add(setting.name(), setting.toJson());
            }
            root.add(section.getKey(), object);
        }
        try {
            JsonFiles.writeAtomic(file, GSON.toJson(root));
        }
        catch (IOException e) {
            AhToolsMod.LOGGER.warn("failed to write {}, settings not saved", file, e);
        }
    }
}
