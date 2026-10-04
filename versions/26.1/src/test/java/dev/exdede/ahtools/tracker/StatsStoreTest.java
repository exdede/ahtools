package dev.exdede.ahtools.tracker;

import dev.exdede.ahtools.config.CorruptCopies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StatsStoreTest {
    @TempDir Path dir;

    private final AtomicReference<String> day = new AtomicReference<>("2026-08-17");

    private StatsStore store(Path file) {
        return new StatsStore(file, day::get);
    }

    @Test
    void profitIsRevenueMinusCost() {
        StatBucket bucket = StatBucket.EMPTY.plus(20_000L, 7_500L, 5L);
        assertEquals(20_000L, bucket.revenue());
        assertEquals(7_500L, bucket.cost());
        assertEquals(12_500L, bucket.profit());
        assertEquals(5L, bucket.items());
        assertEquals(1L, bucket.sales());
    }

    @Test
    void recordsIntoAllThreeHorizons() {
        StatsStore store = store(dir.resolve("stats.json"));
        store.recordSale("Observer", 20_000L, 7_500L, 5L);

        assertEquals(20_000L, store.session("Observer").revenue());
        assertEquals(20_000L, store.today("Observer").revenue());
        assertEquals(20_000L, store.lifetime("Observer").revenue());
        assertEquals(12_500L, store.lifetime("Observer").profit());
    }

    @Test
    void keepsPresetsApartAndSumsTheGlobalTotal() {
        StatsStore store = store(dir.resolve("stats.json"));
        store.recordSale("Observer", 20_000L, 7_500L, 5L);
        store.recordSale("Map", 9_000L, 1_000L, 1L);

        assertEquals(20_000L, store.lifetime("Observer").revenue());
        assertEquals(9_000L, store.lifetime("Map").revenue());
        assertEquals(29_000L, store.lifetime(null).revenue());
        assertEquals(2L, store.lifetime(null).sales());
        assertEquals(6L, store.lifetime(null).items());
    }

    @Test
    void unattributedSalesLandInTheirOwnBucket() {
        StatsStore store = store(dir.resolve("stats.json"));
        store.recordSale(StatsStore.UNATTRIBUTED, 23_000L, 0L, 0L);
        assertEquals(23_000L, store.lifetime(StatsStore.UNATTRIBUTED).revenue());
        assertEquals(0L, store.lifetime(StatsStore.UNATTRIBUTED).cost());
        assertEquals(23_000L, store.lifetime(null).revenue());
    }

    @Test
    void todayRollsOverOnADateChangeButLifetimeDoesNot() {
        StatsStore store = store(dir.resolve("stats.json"));
        store.recordSale("Observer", 20_000L, 7_500L, 5L);

        day.set("2026-08-18");
        assertEquals(0L, store.today("Observer").revenue());
        assertEquals(20_000L, store.lifetime("Observer").revenue());

        store.recordSale("Observer", 1_000L, 0L, 1L);
        assertEquals(1_000L, store.today("Observer").revenue());
        assertEquals(21_000L, store.lifetime("Observer").revenue());
    }

    @Test
    void sessionIsNotPersistedButLifetimeAndTodayAre() {
        Path file = dir.resolve("stats.json");
        StatsStore store = store(file);
        store.recordSale("Observer", 20_000L, 7_500L, 5L);
        store.save();

        StatsStore reloaded = store(file);
        reloaded.load();
        assertEquals(0L, reloaded.session("Observer").revenue());
        assertEquals(20_000L, reloaded.today("Observer").revenue());
        assertEquals(20_000L, reloaded.lifetime("Observer").revenue());
    }

    @Test
    void aStaleStoredDayIsNotCountedAsToday() {
        Path file = dir.resolve("stats.json");
        StatsStore store = store(file);
        store.recordSale("Observer", 20_000L, 7_500L, 5L);
        store.save();

        day.set("2026-08-19");
        StatsStore reloaded = store(file);
        reloaded.load();
        assertEquals(0L, reloaded.today("Observer").revenue());
        assertEquals(20_000L, reloaded.lifetime("Observer").revenue());
    }

    @Test
    void resetsClearOnlyWhatTheyName() {
        StatsStore store = store(dir.resolve("stats.json"));
        store.recordSale("Observer", 20_000L, 7_500L, 5L);

        store.resetSession();
        assertEquals(0L, store.session("Observer").revenue());
        assertEquals(20_000L, store.today("Observer").revenue());

        store.resetToday();
        assertEquals(0L, store.today("Observer").revenue());
        assertEquals(20_000L, store.lifetime("Observer").revenue());

        store.recordSale("Map", 5_000L, 0L, 1L);
        store.resetPreset("Map");
        assertEquals(0L, store.lifetime("Map").revenue());
        assertEquals(20_000L, store.lifetime("Observer").revenue());
    }

    @Test
    void lifetimeResetBacksUpTheFileFirst() throws Exception {
        Path file = dir.resolve("stats.json");
        StatsStore store = store(file);
        store.recordSale("Observer", 20_000L, 7_500L, 5L);
        store.save();

        store.resetLifetime();
        assertEquals(0L, store.lifetime("Observer").revenue());
        assertEquals(0L, store.today("Observer").revenue());

        long backups = Files.list(dir)
            .filter(p -> p.getFileName().toString().startsWith("stats-backup-"))
            .count();
        assertEquals(1L, backups, "lifetime reset did not write a backup");
    }

    @Test
    void listsEveryPresetItHasSeen() {
        StatsStore store = store(dir.resolve("stats.json"));
        store.recordSale("Observer", 1L, 0L, 1L);
        store.recordSale("Map", 1L, 0L, 1L);
        assertTrue(store.presetNames().containsAll(java.util.List.of("Map", "Observer")));
    }

    @Test
    void malformedFileLoadsEmpty() throws Exception {
        Path file = dir.resolve("stats.json");
        Files.writeString(file, "]not json[");
        StatsStore store = store(file);
        store.load();
        assertEquals(0L, store.lifetime(null).revenue());
    }

    @Test
    void aFileFromANewerBuildIsNeverOverwritten() throws IOException {
        Path file = dir.resolve("stats.json");
        String original = "{\"schemaVersion\":99,\"day\":\"2026-08-17\",\"lifetime\":{}}";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        StatsStore store = store(file);
        store.load();
        store.recordSale("p", 100L, 10L, 1L);
        store.save();
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void aCorruptFileIsCopiedAsideBeforeAnythingOverwritesIt() throws IOException {
        Path file = dir.resolve("stats.json");
        String original = "{\"lifetime\":{\"p\":{\"revenue\":";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        StatsStore store = store(file);
        store.load();
        assertEquals(original, CorruptCopies.content(dir, "stats"));
        store.recordSale("p", 100L, 10L, 1L);
        store.save();
        assertEquals(100L, lifetimeRevenueAfterReload(file, day.get()));
    }

    @Test
    void aSavedFileCarriesItsSchemaVersion() throws IOException {
        Path file = dir.resolve("stats.json");
        StatsStore store = store(file);
        store.recordSale("p", 100L, 10L, 1L);
        store.save();
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"schemaVersion\":1"));
    }

    /** Reloads a file in a fresh store, the way the next launch would. */
    private static long lifetimeRevenueAfterReload(Path file, String today) {
        StatsStore fresh = new StatsStore(file, () -> today);
        fresh.load();
        return fresh.lifetime(null).revenue();
    }
}
