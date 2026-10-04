package dev.exdede.ahtools.seller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSignalsTest {
    @Test
    void classifiesListingsFull() {
        assertEquals(ChatSignals.Kind.LISTINGS_FULL,
            ChatSignals.classify("You have too many listed items!"));
    }

    @Test
    void classifiesSales() {
        assertEquals(ChatSignals.Kind.SALE, ChatSignals.classify("Olrun bought your Map for $25.9K"));
        assertEquals(ChatSignals.Kind.SALE, ChatSignals.classify("You earned $23K from auction"));
    }

    @Test
    void classifiesListedOk() {
        assertEquals(ChatSignals.Kind.LISTED_OK, ChatSignals.classify("You listed 1 Map for $ 3K"));
    }

    @Test
    void classifiesInvalidPrice() {
        assertEquals(ChatSignals.Kind.INVALID_PRICE, ChatSignals.classify("That is not a valid number."));
        assertEquals(ChatSignals.Kind.INVALID_PRICE, ChatSignals.classify("That price is too high"));
    }

    @Test
    void listingsFullWinsOverEverythingElse() {
        assertEquals(ChatSignals.Kind.LISTINGS_FULL,
            ChatSignals.classify("You have too many listed items, sell some first"));
    }

    @Test
    void ignoresNoise() {
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun: selling maps cheap"));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify(""));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify(null));
    }

    @Test
    void stripsColorCodesBeforeMatching() {
        assertEquals(ChatSignals.Kind.LISTED_OK, ChatSignals.classify("§aYou listed 4 Observer for $ 12K"));
    }

    @Test
    void classifiesRealServerRefusals() {
        // Lines copied from real server chat, 2026-07 to 2026-09.
        assertEquals(ChatSignals.Kind.CANNOT_SELL_AIR, ChatSignals.classify("You cannot sell air."));
        assertEquals(ChatSignals.Kind.COOLDOWN,
            ChatSignals.classify("You need to wait another 2.5 seconds to execute a command"));
        assertEquals(ChatSignals.Kind.COOLDOWN,
            ChatSignals.classify("You cannot do this right now, on cooldown"));
        assertEquals(ChatSignals.Kind.COMMAND_UNAVAILABLE, ChatSignals.classify("This command does not exist"));
        assertEquals(ChatSignals.Kind.SALE,
            ChatSignals.classify("You earned $23K from auction got delivered 3 items"));
    }

    @Test
    void cooldownLengthComesFromTheLine() {
        assertEquals(50L, ChatSignals.cooldownTicks("You need to wait another 2.5 seconds to execute a command"));
        assertEquals(18L, ChatSignals.cooldownTicks("You need to wait another 0.9 seconds to execute a command"));
        assertEquals(ChatSignals.DEFAULT_COOLDOWN_TICKS,
            ChatSignals.cooldownTicks("You cannot do this right now, on cooldown"));
    }

    @Test
    void whispersAndPlayerChatNeverDriveTheSeller() {
        // Scam bots whisper every player on DonutSMP all day, so anything a
        // stranger can type must classify as noise.
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun whispers to you: price is too high"));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun whispers to you: That price is too high"));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun whispers to you: bought your Map for $9K"));
        assertEquals(ChatSignals.Kind.NONE,
            ChatSignals.classify("Olrun whispers to you: You have too many listed items"));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun whispers to you: You cannot sell air."));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun: bought your Map for $1B"));
        assertEquals(ChatSignals.Kind.NONE, ChatSignals.classify("Olrun: not a valid number lol"));
    }
}
