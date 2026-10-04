package dev.exdede.ahtools.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsFileTest {
    enum Accent { PINK, PURPLE, CYAN }

    private Map<String, List<Setting<?>>> sections(Setting<?>... settings) {
        Map<String, List<Setting<?>>> map = new LinkedHashMap<>();
        map.put("General", List.of(settings));
        return map;
    }

    @Test
    void roundTripsEverySettingType(@TempDir Path dir) {
        Path file = dir.resolve("ahtools.json");
        BoolSetting flag = new BoolSetting("debugLogging", false, "");
        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        EnumSetting<Accent> accent = new EnumSetting<>("guiAccent", Accent.PINK, "");
        StringListSetting commands = new StringListSetting("spamCommands", List.of(), "");

        flag.set(true);
        delay.set(300);
        accent.set(Accent.CYAN);
        commands.set(List.of("/ah sell 1000", "/say hi"));
        SettingsFile.save(file, sections(flag, delay, accent, commands));

        BoolSetting flag2 = new BoolSetting("debugLogging", false, "");
        IntSetting delay2 = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        EnumSetting<Accent> accent2 = new EnumSetting<>("guiAccent", Accent.PINK, "");
        StringListSetting commands2 = new StringListSetting("spamCommands", List.of(), "");
        SettingsFile.load(file, sections(flag2, delay2, accent2, commands2));

        assertTrue(flag2.get());
        assertEquals(300, delay2.get());
        assertEquals(Accent.CYAN, accent2.get());
        assertEquals(List.of("/ah sell 1000", "/say hi"), commands2.get());
    }

    @Test
    void clampsOutOfRangeIntegersOnLoad(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        Files.writeString(file, "{\"General\":{\"spammerDelayTicks\":5}}", StandardCharsets.UTF_8);

        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        SettingsFile.load(file, sections(delay));

        assertEquals(20, delay.get());
    }

    @Test
    void unknownEnumNameFallsBackToTheDefault(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        Files.writeString(file, "{\"General\":{\"guiAccent\":\"CHARTREUSE\"}}", StandardCharsets.UTF_8);

        EnumSetting<Accent> accent = new EnumSetting<>("guiAccent", Accent.PINK, "");
        SettingsFile.load(file, sections(accent));

        assertEquals(Accent.PINK, accent.get());
    }

    @Test
    void readsAFileMalilibWroteAndIgnoresItsHotkeySection(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        Files.writeString(file, """
            {
              "General": {
                "debugLogging": true,
                "spamCommands": ["/ah sell 5000"],
                "spammerRandomDelay": false,
                "spammerDelayTicks": 40,
                "spammerMinDelayTicks": 25,
                "spammerMaxDelayTicks": 60
              },
              "Hotkeys": {
                "toggleSeller": "F6",
                "emergencyStop": "F7"
              }
            }
            """, StandardCharsets.UTF_8);

        BoolSetting debug = new BoolSetting("debugLogging", false, "");
        StringListSetting commands = new StringListSetting("spamCommands", List.of(), "");
        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        SettingsFile.load(file, sections(debug, commands, delay));

        assertTrue(debug.get());
        assertEquals(List.of("/ah sell 5000"), commands.get());
        assertEquals(40, delay.get());
    }

    @Test
    void missingFileLeavesDefaultsAlone(@TempDir Path dir) {
        BoolSetting debug = new BoolSetting("debugLogging", false, "");
        SettingsFile.load(dir.resolve("nope.json"), sections(debug));
        assertFalse(debug.get());
    }

    @Test
    void malformedFileLeavesDefaultsAlone(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        SettingsFile.load(file, sections(delay));

        assertEquals(25, delay.get());
    }

    @Test
    void boolSettingTogglesAndEnumSettingCycles() {
        BoolSetting flag = new BoolSetting("f", false, "");
        flag.toggle();
        assertTrue(flag.get());

        EnumSetting<Accent> accent = new EnumSetting<>("a", Accent.PINK, "");
        accent.cycle();
        assertEquals(Accent.PURPLE, accent.get());
        accent.cycle();
        accent.cycle();
        assertEquals(Accent.PINK, accent.get());
    }

    @Test
    void aFileFrom020WithoutASchemaFieldLoads(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        Files.writeString(file, "{\"General\":{\"spammerDelayTicks\":300}}", StandardCharsets.UTF_8);
        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        SettingsFile.load(file, sections(delay));
        assertEquals(300, delay.get());
    }

    @Test
    void aFileFromANewerBuildIsNeverOverwritten(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        String original = "{\"schemaVersion\":99,\"General\":{\"spammerDelayTicks\":300}}";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        SettingsFile.load(file, sections(delay));
        delay.set(500);
        SettingsFile.save(file, sections(delay));
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void aCorruptFileIsCopiedAsideAndSavingCarriesOn(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ahtools.json");
        Files.writeString(file, "{\"General\":", StandardCharsets.UTF_8);
        IntSetting delay = new IntSetting("spammerDelayTicks", 25, 20, 1200, "");
        SettingsFile.load(file, sections(delay));
        assertEquals(1, CorruptCopies.count(dir, "ahtools"));
        delay.set(500);
        SettingsFile.save(file, sections(delay));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"schemaVersion\": 1"));
    }
}
