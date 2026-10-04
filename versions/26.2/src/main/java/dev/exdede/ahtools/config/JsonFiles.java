package dev.exdede.ahtools.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.exdede.ahtools.AhToolsMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Shared plumbing for every JSON file the mod owns.
 *
 * Writes go to a sibling temp file and are moved over the real one, so a crash
 * or a full disk mid-write leaves the previous file intact instead of an empty
 * one.
 *
 * Every file carries a schemaVersion. A file written by a newer build is read
 * for what it is worth but never overwritten, so trying an older jar cannot
 * destroy data the newer one wrote. A file that does not parse at all is
 * copied aside before anything is allowed to replace it: the copy keeps
 * whatever can still be rescued by hand, and the mod keeps saving instead of
 * silently losing every session after it.
 */
public final class JsonFiles {
    public static final String SCHEMA_FIELD = "schemaVersion";
    /** Files from 0.2.0 carry no field and are version 1. Bump together with a migration. */
    public static final int CURRENT_SCHEMA = 1;

    private JsonFiles() {}

    public static int schemaOf(JsonObject root) {
        JsonElement element = root.get(SCHEMA_FIELD);
        if (element == null || !element.isJsonPrimitive()) return 1;
        try {
            return element.getAsInt();
        }
        catch (RuntimeException e) {
            return 1;
        }
    }

    public static boolean isTooNew(JsonObject root) {
        return schemaOf(root) > CURRENT_SCHEMA;
    }

    public static void stamp(JsonObject root) {
        root.addProperty(SCHEMA_FIELD, CURRENT_SCHEMA);
    }

    public static void writeAtomic(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Copies an unreadable file to "name.corrupt-millis.json" beside it. Never throws.
     *
     * @return false when the copy failed, in which case the caller must not overwrite the original.
     */
    public static boolean backupCorrupt(Path file) {
        String name = file.getFileName().toString();
        String base = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        Path copy = file.resolveSibling(base + ".corrupt-" + System.currentTimeMillis() + ".json");
        try {
            Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
            AhToolsMod.LOGGER.warn("{} could not be read, kept a copy at {}", file, copy);
            return true;
        }
        catch (IOException e) {
            AhToolsMod.LOGGER.warn("{} could not be read, and copying it to {} failed", file, copy, e);
            return false;
        }
    }
}
