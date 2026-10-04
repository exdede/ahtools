package dev.exdede.ahtools.gui;

/**
 * Small pixel art icons, drawn one fill per lit pixel rather than from a
 * texture, so they look the same on every Minecraft version without any of
 * the texture API that changes between them. '#' is lit, anything else is
 * transparent. Every row of an icon has the same length.
 */
public final class PixelIcons {
    public static final String[] GITHUB = {
        ".....######.....",
        "...##########...",
        "..############..",
        ".###..####..###.",
        ".###........###.",
        "####........####",
        "####........####",
        "####........####",
        "####........####",
        "#####......#####",
        "######....######",
        ".###.#....#####.",
        ".###......#####.",
        "..####....####..",
        "...###....###...",
        ".....#....#.....",
    };

    public static final String[] GLOBE = {
        "......####......",
        "....##.##.##....",
        "...#..#..#..#...",
        "..#..#....#..#..",
        ".#...#....#...#.",
        ".##############.",
        "#....#....#....#",
        "#....#....#....#",
        "#....#....#....#",
        "#....#....#....#",
        ".##############.",
        ".#...#....#...#.",
        "..#..#....#..#..",
        "...#..#..#..#...",
        "....##.##.##....",
        "......####......",
    };

    private PixelIcons() {}

    public static int width(String[] icon) { return icon.length == 0 ? 0 : icon[0].length(); }
    public static int height(String[] icon) { return icon.length; }

    public static boolean lit(String[] icon, int x, int y) {
        return y >= 0 && y < icon.length && x >= 0 && x < icon[y].length() && icon[y].charAt(x) == '#';
    }
}
