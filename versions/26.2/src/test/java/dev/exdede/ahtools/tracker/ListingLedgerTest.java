package dev.exdede.ahtools.tracker;

import dev.exdede.ahtools.config.CorruptCopies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ListingLedgerTest {
    @TempDir Path dir;

    @Test
    void claimsTheOldestMatchingListingFirst() {
        ListingLedger ledger = new ListingLedger(dir.resolve("ledger.json"));
        ledger.record("Map", 4, "Maps", 1000L, 1_000L);
        ledger.record("Map", 16, "Maps", 1000L, 2_000L);

        ListingLedger.Entry first = ledger.claim("Map", 3_000L);
        assertNotNull(first);
        assertEquals(4, first.quantity());
        assertEquals("Maps", first.presetName());

        ListingLedger.Entry second = ledger.claim("Map", 3_000L);
        assertEquals(16, second.quantity());
        assertNull(ledger.claim("Map", 3_000L));
    }

    @Test
    void matchesItemNamesLooselyOnCaseAndSpacing() {
        ListingLedger ledger = new ListingLedger(dir.resolve("ledger.json"));
        ledger.record("Totem of Undying", 1, "Totems", 500L, 1_000L);
        assertNotNull(ledger.claim("  totem of undying ", 2_000L));
    }

    @Test
    void doesNotMatchADifferentItem() {
        ListingLedger ledger = new ListingLedger(dir.resolve("ledger.json"));
        ledger.record("Map", 4, "Maps", 1000L, 1_000L);
        assertNull(ledger.claim("Observer", 2_000L));
        assertEquals(1, ledger.size());
    }

    @Test
    void expiresEntriesOlderThanTheMaxAge() {
        ListingLedger ledger = new ListingLedger(dir.resolve("ledger.json"));
        ledger.record("Map", 4, "Maps", 1000L, 0L);
        assertNull(ledger.claim("Map", ListingLedger.MAX_AGE_MS + 1));
        assertEquals(0, ledger.size());
    }

    @Test
    void remembersTheLastListingPresetWithinTheWindow() {
        ListingLedger ledger = new ListingLedger(dir.resolve("ledger.json"));
        ledger.record("Map", 4, "Maps", 1000L, 10_000L);
        assertEquals("Maps", ledger.lastPresetWithin(30_000L, 20_000L));
        assertNull(ledger.lastPresetWithin(5_000L, 20_000L));
    }

    @Test
    void theLastListingPresetSurvivesClaimingTheEntry() {
        ListingLedger ledger = new ListingLedger(dir.resolve("ledger.json"));
        ledger.record("Map", 4, "Maps", 1000L, 10_000L);
        ledger.claim("Map", 11_000L);
        assertEquals("Maps", ledger.lastPresetWithin(30_000L, 20_000L));
    }

    @Test
    void roundTripsThroughDisk() {
        Path file = dir.resolve("ledger.json");
        ListingLedger ledger = new ListingLedger(file);
        ledger.record("Map", 8, "Maps", 750L, 5_000L);
        ledger.save();

        ListingLedger reloaded = new ListingLedger(file);
        reloaded.load();
        assertEquals(1, reloaded.size());
        ListingLedger.Entry entry = reloaded.claim("Map", 6_000L);
        assertEquals(8, entry.quantity());
        assertEquals(750L, entry.costPerItem());
        assertEquals("Maps", reloaded.lastPresetWithin(30_000L, 6_000L));
    }

    @Test
    void loadDropsExpiredEntries() throws Exception {
        Path file = dir.resolve("ledger.json");
        ListingLedger ledger = new ListingLedger(file);
        ledger.record("Map", 8, "Maps", 750L, 0L);
        ledger.save();

        ListingLedger reloaded = new ListingLedger(file);
        reloaded.load();
        reloaded.pruneExpired(ListingLedger.MAX_AGE_MS + 1);
        assertEquals(0, reloaded.size());
    }

    @Test
    void malformedFileLoadsEmpty() throws Exception {
        Path file = dir.resolve("ledger.json");
        Files.writeString(file, "nonsense");
        ListingLedger ledger = new ListingLedger(file);
        ledger.load();
        assertEquals(0, ledger.size());
    }

    @Test
    void aFileFromANewerBuildIsNeverOverwritten() throws IOException {
        Path file = dir.resolve("ledger.json");
        String original = "{\"schemaVersion\":99,\"entries\":[]}";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        ListingLedger ledger = new ListingLedger(file);
        ledger.load();
        ledger.record("Map", 1, "Maps", 0L, 1_000L);
        ledger.save();
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void aCorruptFileIsCopiedAsideAndSavingCarriesOn() throws IOException {
        Path file = dir.resolve("ledger.json");
        Files.writeString(file, "[[[", StandardCharsets.UTF_8);
        ListingLedger ledger = new ListingLedger(file);
        ledger.load();
        assertEquals(1, CorruptCopies.count(dir, "ledger"));
        ledger.record("Map", 1, "Maps", 0L, 1_000L);
        ledger.save();
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"schemaVersion\":1"));
    }
}
