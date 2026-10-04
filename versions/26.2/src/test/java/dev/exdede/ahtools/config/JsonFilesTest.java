package dev.exdede.ahtools.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonFilesTest {
    @Test
    void missingSchemaFieldMeansVersionOne() {
        JsonObject root = JsonParser.parseString("{\"General\":{}}").getAsJsonObject();
        assertEquals(1, JsonFiles.schemaOf(root));
        assertFalse(JsonFiles.isTooNew(root));
    }

    @Test
    void newerSchemaIsTooNew() {
        JsonObject root = JsonParser.parseString("{\"schemaVersion\":99}").getAsJsonObject();
        assertTrue(JsonFiles.isTooNew(root));
    }

    @Test
    void garbageSchemaFieldCountsAsVersionOne() {
        JsonObject root = JsonParser.parseString("{\"schemaVersion\":\"abc\"}").getAsJsonObject();
        assertEquals(1, JsonFiles.schemaOf(root));
    }

    @Test
    void stampWritesCurrentVersion() {
        JsonObject root = new JsonObject();
        JsonFiles.stamp(root);
        assertEquals(JsonFiles.CURRENT_SCHEMA, root.get("schemaVersion").getAsInt());
    }

    @Test
    void writeAtomicReplacesContentAndLeavesNoTempFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sub/stats.json");
        JsonFiles.writeAtomic(file, "{\"a\":1}");
        JsonFiles.writeAtomic(file, "{\"a\":2}");
        assertEquals("{\"a\":2}", Files.readString(file, StandardCharsets.UTF_8));
        try (var listing = Files.list(file.getParent())) {
            assertEquals(1, listing.count());
        }
    }

    @Test
    void backupCorruptKeepsACopyNextToTheFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("stats.json");
        Files.writeString(file, "{\"broken\":", StandardCharsets.UTF_8);
        JsonFiles.backupCorrupt(file);
        assertEquals(1, CorruptCopies.count(dir, "stats"));
        assertEquals("{\"broken\":", CorruptCopies.content(dir, "stats"));
    }
}
