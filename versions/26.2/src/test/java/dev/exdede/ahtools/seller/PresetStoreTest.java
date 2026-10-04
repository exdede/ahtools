package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.config.CorruptCopies;
import dev.exdede.ahtools.core.DelayRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PresetStoreTest {
    @TempDir Path dir;

    private Preset observer() {
        return new Preset("Observer", "minecraft:observer", Preset.PricingMode.RANDOM_RANGE,
            10000L, 3000L, 20000L, 1000L, 100_000_000L,
            DelayRule.Mode.RANDOM, 25, 25, 40, 1500L, true);
    }

    @Test
    void defaultsAreSafeAndAboveTheDelayFloor() {
        Preset preset = Preset.defaults("New preset");
        assertEquals("New preset", preset.name());
        assertEquals(Preset.PricingMode.FIXED, preset.pricingMode());
        assertTrue(preset.fixedPrice() > 0);
        assertTrue(preset.delayRule().minTicks() >= DelayRule.MIN_DELAY_TICKS);
        assertFalse(preset.validateHotbar());
    }

    @Test
    void delayRuleFollowsTheMode() {
        Preset random = observer();
        assertEquals(DelayRule.Mode.RANDOM, random.delayRule().mode());
        assertEquals(25, random.delayRule().minTicks());
        assertEquals(40, random.delayRule().maxTicks());

        Preset fixed = new Preset("F", "", Preset.PricingMode.FIXED, 5000L, 0L, 0L, 0L, 1_000_000L,
            DelayRule.Mode.FIXED, 30, 20, 20, 0L, false);
        assertEquals(DelayRule.Mode.FIXED, fixed.delayRule().mode());
        assertEquals(30, fixed.delayRule().fixedTicks());
    }

    @Test
    void delayRuleStillClampsAHandEditedValue() {
        Preset cheater = new Preset("F", "", Preset.PricingMode.FIXED, 5000L, 0L, 0L, 0L, 1_000_000L,
            DelayRule.Mode.FIXED, 2, 1, 3, 0L, false);
        assertEquals(DelayRule.MIN_DELAY_TICKS, cheater.delayRule().fixedTicks());
    }

    @Test
    void roundTripsThroughDisk() {
        Path file = dir.resolve("presets.json");
        PresetStore store = new PresetStore(file);
        store.put(observer());
        store.put(Preset.defaults("Dirt"));
        store.select("Observer");
        store.save();

        PresetStore reloaded = new PresetStore(file);
        reloaded.load();
        assertEquals(2, reloaded.presets().size());
        assertEquals("Observer", reloaded.selectedName());
        assertEquals(observer(), reloaded.selected());
    }

    @Test
    void putReplacesByName() {
        PresetStore store = new PresetStore(dir.resolve("presets.json"));
        store.put(observer());
        store.put(observer().withName("Observer"));
        assertEquals(1, store.presets().size());
    }

    @Test
    void removeClearsTheSelectionWhenItWasSelected() {
        PresetStore store = new PresetStore(dir.resolve("presets.json"));
        store.put(observer());
        store.select("Observer");
        store.remove("Observer");
        assertTrue(store.presets().isEmpty());
        assertNull(store.selected());
    }

    @Test
    void missingFileLoadsEmpty() {
        PresetStore store = new PresetStore(dir.resolve("nope.json"));
        store.load();
        assertTrue(store.presets().isEmpty());
        assertNull(store.selected());
    }

    @Test
    void malformedFileFallsBackToEmptyInsteadOfThrowing() throws Exception {
        Path file = dir.resolve("presets.json");
        Files.writeString(file, "{ this is not json");
        PresetStore store = new PresetStore(file);
        store.load();
        assertTrue(store.presets().isEmpty());
    }

    @Test
    void addingATakenNameIsRefusedAndKeepsTheOriginal() {
        PresetStore store = new PresetStore(dir.resolve("presets.json"));
        assertTrue(store.add(observer()));
        assertFalse(store.add(Preset.defaults("Observer")));
        assertEquals(1500L, store.presets().get(0).costPerItem(), "the original was overwritten");
    }

    @Test
    void renamingOntoATakenNameIsRefused() {
        PresetStore store = new PresetStore(dir.resolve("presets.json"));
        store.add(observer());
        store.add(Preset.defaults("Maps"));
        assertFalse(store.rename("Maps", Preset.defaults("Observer")));
        assertEquals(2, store.presets().size());
        assertEquals(1500L, store.find("Observer").costPerItem());
    }

    @Test
    void renamingMovesTheSelectionAlong() {
        PresetStore store = new PresetStore(dir.resolve("presets.json"));
        store.add(observer());
        store.select("Observer");
        assertTrue(store.rename("Observer", observer().withName("Observers")));
        assertEquals("Observers", store.selectedName());
        assertEquals(1, store.presets().size());
        // Saving an edit under the same name is not a rename onto a taken name.
        assertTrue(store.rename("Observers", observer().withName("Observers")));
    }

    @Test
    void aFileFromANewerBuildIsNeverOverwritten() throws IOException {
        Path file = dir.resolve("presets.json");
        String original = "{\"schemaVersion\":99,\"presets\":[]}";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        PresetStore store = new PresetStore(file);
        store.load();
        store.add(Preset.defaults("New"));
        store.save();
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void aCorruptFileIsCopiedAsideAndSavingCarriesOn() throws IOException {
        Path file = dir.resolve("presets.json");
        Files.writeString(file, "{\"presets\":[{\"name\":", StandardCharsets.UTF_8);
        PresetStore store = new PresetStore(file);
        store.load();
        assertEquals(1, CorruptCopies.count(dir, "presets"));
        store.add(Preset.defaults("New"));
        store.save();
        PresetStore reloaded = new PresetStore(file);
        reloaded.load();
        assertNotNull(reloaded.find("New"));
    }
}
