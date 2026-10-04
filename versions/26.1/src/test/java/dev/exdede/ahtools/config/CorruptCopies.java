package dev.exdede.ahtools.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Test helper: finds the copies JsonFiles.backupCorrupt leaves beside a file. */
public final class CorruptCopies {
    private CorruptCopies() {}

    public static List<Path> find(Path dir, String baseName) throws IOException {
        try (var listing = Files.list(dir)) {
            return listing.filter(p -> p.getFileName().toString().startsWith(baseName + ".corrupt-")).toList();
        }
    }

    public static long count(Path dir, String baseName) throws IOException {
        return find(dir, baseName).size();
    }

    public static String content(Path dir, String baseName) throws IOException {
        return Files.readString(find(dir, baseName).get(0), StandardCharsets.UTF_8);
    }
}
