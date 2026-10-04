package dev.exdede.ahtools.tracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies one chat line into a money event. Every pattern here was captured
 * verbatim from real server output on 2026-08-16 and is deliberately narrow:
 * an unrecognized line is worth nothing, but a wrongly recognized one puts
 * fake money in the ledger.
 *
 *   "Lawes_gaming bought your Map for $9K"   per-listing sale
 *   ".splxy67 bought your Map for $12K"      names may start with a dot
 *   "You earned $23K from auction"           offline aggregate, no item
 *   "You earned $23K from auction got delivered 3 items"   same, with a suffix
 *   "You listed 8 Map for $ 20K"             listing confirmation
 *
 * Note what the sale line does NOT carry: a quantity. One sale line is one
 * listing sold at its listed total, and a listing may hold 1 or 64 items. Only
 * the LISTED line knows the count, which is why the ledger has to remember it.
 *
 * Every pattern is anchored at both ends and the buyer must look like a real
 * player name. Strangers can whisper anything and public chat prefixes the
 * sender, so "Olrun: bought your Map for $1B" must not read as a sale; a loose
 * "\S+" for the buyer would have accepted "Olrun:" as a name.
 */
public final class SaleParser {
    public enum Kind { SALE, AUCTION, LISTED }

    /** quantity is 0 for SALE and AUCTION; itemName is null for AUCTION. */
    public record ParsedLine(Kind kind, String itemName, int quantity, long money, String raw) {}

    private static final Pattern COLOR_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
    private static final String MONEY = "(\\$\\s?[\\d,.]+\\s*[KMB]?)";
    /** Java names, plus the leading dot Bedrock players get through Geyser. */
    private static final String PLAYER = "\\.?[A-Za-z0-9_]{1,16}";
    private static final Pattern SALE_LINE =
        Pattern.compile("^" + PLAYER + " bought your (.+?) for " + MONEY + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUCTION_LINE =
        Pattern.compile("^You earned " + MONEY + " from auction(?: got delivered \\d+ items?)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LISTED_LINE =
        Pattern.compile("^You listed (\\d+) (.+?) for " + MONEY + "$", Pattern.CASE_INSENSITIVE);

    private SaleParser() {}

    /** Removes color codes and surrounding whitespace. Public so callers can log the clean form. */
    public static String strip(String raw) {
        if (raw == null) return "";
        return COLOR_CODES.matcher(raw).replaceAll("").trim();
    }

    public static ParsedLine parse(String raw) {
        if (raw == null) return null;
        String line = strip(raw);
        if (line.isEmpty()) return null;

        Matcher sale = SALE_LINE.matcher(line);
        if (sale.matches()) {
            Long money = Money.parseOrNull(sale.group(2));
            if (money == null || money <= 0) return null;
            return new ParsedLine(Kind.SALE, sale.group(1).trim(), 0, money, line);
        }

        Matcher auction = AUCTION_LINE.matcher(line);
        if (auction.matches()) {
            Long money = Money.parseOrNull(auction.group(1));
            if (money == null || money <= 0) return null;
            return new ParsedLine(Kind.AUCTION, null, 0, money, line);
        }

        Matcher listed = LISTED_LINE.matcher(line);
        if (listed.matches()) {
            Long money = Money.parseOrNull(listed.group(3));
            if (money == null || money <= 0) return null;
            int quantity;
            try {
                quantity = Integer.parseInt(listed.group(1));
            }
            catch (NumberFormatException e) {
                return null;
            }
            if (quantity <= 0) return null;
            return new ParsedLine(Kind.LISTED, listed.group(2).trim(), quantity, money, line);
        }

        return null;
    }
}
