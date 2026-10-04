package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.tracker.SaleParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads chat for the signals the sell cycle needs: listings are full (park),
 * something sold (a slot freed), the listing went up, the price was refused,
 * the hand was empty, the server wants a pause, and /ah does not exist here.
 *
 * Every line below was copied from real server output: the price refusals on
 * 2026-08-12, the rest from real chat logs of 2026-07 to 2026-09. Nothing here
 * is guessed, and nothing should be widened without a fresh capture, since a
 * false positive silently changes what the cycle does next.
 *
 * Every pattern is anchored at the start of the line. Scam bots whisper every
 * player on DonutSMP all day ("Name whispers to you: ..."), so a pattern that
 * matched anywhere in a line would hand any stranger a remote control: one
 * whisper of "price is too high" used to stop the seller outright. Sale and
 * listing lines go through SaleParser, which anchors both ends and checks the
 * buyer is a real player name.
 */
public final class ChatSignals {
    public enum Kind {
        LISTINGS_FULL, SALE, LISTED_OK, INVALID_PRICE,
        CANNOT_SELL_AIR, COOLDOWN, COMMAND_UNAVAILABLE, NONE
    }

    /** Used when the server says "on cooldown" without saying for how long. */
    public static final long DEFAULT_COOLDOWN_TICKS = 60L;

    private static final Pattern LISTINGS_FULL =
        Pattern.compile("^You have too many listed items\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVALID_PRICE =
        Pattern.compile("^(?:That is not a valid number|That price is too high)\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CANNOT_SELL_AIR =
        Pattern.compile("^You cannot sell air\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COOLDOWN_SECONDS =
        Pattern.compile("^You need to wait another (\\d+(?:\\.\\d+)?) seconds to execute a command\\b.*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COOLDOWN_PLAIN =
        Pattern.compile("^You cannot do this right now, on cooldown\\b.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_UNAVAILABLE =
        Pattern.compile("^This command does not exist\\b.*$", Pattern.CASE_INSENSITIVE);

    private ChatSignals() {}

    public static Kind classify(String line) {
        if (line == null) return Kind.NONE;
        String clean = SaleParser.strip(line);
        if (clean.isEmpty()) return Kind.NONE;
        if (LISTINGS_FULL.matcher(clean).matches()) return Kind.LISTINGS_FULL;

        SaleParser.ParsedLine parsed = SaleParser.parse(clean);
        if (parsed != null) {
            return parsed.kind() == SaleParser.Kind.LISTED ? Kind.LISTED_OK : Kind.SALE;
        }

        if (INVALID_PRICE.matcher(clean).matches()) return Kind.INVALID_PRICE;
        if (CANNOT_SELL_AIR.matcher(clean).matches()) return Kind.CANNOT_SELL_AIR;
        if (COOLDOWN_SECONDS.matcher(clean).matches() || COOLDOWN_PLAIN.matcher(clean).matches()) {
            return Kind.COOLDOWN;
        }
        if (COMMAND_UNAVAILABLE.matcher(clean).matches()) return Kind.COMMAND_UNAVAILABLE;
        return Kind.NONE;
    }

    /** How long a COOLDOWN line asks the client to wait, rounded up to whole ticks. */
    public static long cooldownTicks(String line) {
        Matcher matcher = COOLDOWN_SECONDS.matcher(SaleParser.strip(line));
        if (!matcher.matches()) return DEFAULT_COOLDOWN_TICKS;
        try {
            return (long) Math.ceil(Double.parseDouble(matcher.group(1)) * 20.0);
        }
        catch (NumberFormatException e) {
            return DEFAULT_COOLDOWN_TICKS;
        }
    }
}
