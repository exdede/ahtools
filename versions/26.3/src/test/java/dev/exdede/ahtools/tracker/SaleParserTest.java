package dev.exdede.ahtools.tracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SaleParserTest {
    @Test
    void parsesPerListingSale() {
        SaleParser.ParsedLine p = SaleParser.parse("Lawes_gaming bought your Map for $9K");
        assertNotNull(p);
        assertEquals(SaleParser.Kind.SALE, p.kind());
        assertEquals("Map", p.itemName());
        assertEquals(9000L, p.money());
        assertEquals(0, p.quantity());
    }

    @Test
    void acceptsDotPrefixedNamesAndMultiWordItems() {
        SaleParser.ParsedLine p = SaleParser.parse(".splxy67 bought your Totem of Undying for $25.9K");
        assertNotNull(p);
        assertEquals("Totem of Undying", p.itemName());
        assertEquals(25900L, p.money());
    }

    @Test
    void parsesAuctionAggregate() {
        SaleParser.ParsedLine p = SaleParser.parse("You earned $23K from auction");
        assertNotNull(p);
        assertEquals(SaleParser.Kind.AUCTION, p.kind());
        assertNull(p.itemName());
        assertEquals(23000L, p.money());
    }

    @Test
    void parsesListingConfirmationWithQuantity() {
        SaleParser.ParsedLine p = SaleParser.parse("You listed 16 Map for $ 20K");
        assertNotNull(p);
        assertEquals(SaleParser.Kind.LISTED, p.kind());
        assertEquals("Map", p.itemName());
        assertEquals(16, p.quantity());
        assertEquals(20000L, p.money());
    }

    @Test
    void stripsColorCodesAndWhitespace() {
        SaleParser.ParsedLine p = SaleParser.parse("  §aOlrun §fbought your §eMap§f for $25.9K  ");
        assertNotNull(p);
        assertEquals(SaleParser.Kind.SALE, p.kind());
        assertEquals("Map", p.itemName());
    }

    @Test
    void ignoresEverythingElse() {
        assertNull(SaleParser.parse("Olrun: hey are you selling maps"));
        assertNull(SaleParser.parse("You have too many listed items"));
        assertNull(SaleParser.parse(""));
        assertNull(SaleParser.parse(null));
    }

    @Test
    void rejectsNonPositiveMoney() {
        assertNull(SaleParser.parse("Olrun bought your Map for $0"));
    }

    @Test
    void parsesTheAuctionLineWithADeliverySuffix() {
        SaleParser.ParsedLine p = SaleParser.parse("You earned $23K from auction got delivered 3 items");
        assertNotNull(p);
        assertEquals(SaleParser.Kind.AUCTION, p.kind());
        assertEquals(23000L, p.money());
        assertNotNull(SaleParser.parse("You earned $1.2K from auction got delivered 1 item"));
    }

    @Test
    void playerChatCannotForgeASale() {
        // A "Name: message" chat line must not pass as "Name bought your ...".
        assertNull(SaleParser.parse("Olrun: bought your Map for $1B"));
        assertNull(SaleParser.parse("[VIP] Olrun bought your Map for $1B"));
        assertNull(SaleParser.parse("Olrun whispers to you: Lawes bought your Map for $1B"));
        assertNull(SaleParser.parse("Olrun whispers to you: You earned $1B from auction"));
    }
}
