package dev.exdede.ahtools.seller;

import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.core.HeldStack;

import java.util.Map;

/**
 * Everything the sell cycle can do to the outside world. The cycle only ever
 * acts through this interface, so the real Minecraft wiring and a test fake
 * are interchangeable and every transition below is testable without a client.
 */
public interface SellEffects {
    /** Hotbar snapshot keyed by index 0 to 8. Empty slots are absent. */
    Map<Integer, HeldStack> readHotbar();

    /** Selects this hotbar index as the held item. */
    void switchToSlot(int hotbarSlot);

    /** Ticks since the shared gate last sent anything, from either feature. */
    long ticksSinceLastSend(long tick);

    /** Queues a command on the shared send gate. False means the gate is busy; try again next tick. */
    boolean submit(String command, DelayRule rule);

    void log(String kind, String message);
}
