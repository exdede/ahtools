package dev.exdede.ahtools.mc;

import dev.exdede.ahtools.AhToolsMod;
import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.core.ChatSendGate;
import dev.exdede.ahtools.core.DelayRule;
import dev.exdede.ahtools.core.HeldStack;
import dev.exdede.ahtools.core.HotbarScan;
import dev.exdede.ahtools.seller.SellEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

/**
 * Real client wiring for the sell cycle: read the live hotbar, select the
 * held slot, and hand commands to the shared gate.
 *
 * Selecting a slot needs both halves. Setting the field alone changes what the
 * client thinks is held while the server still believes the old slot is in
 * hand, and the sell command would then price the wrong stack, so the sync
 * packet is not optional.
 */
public final class LiveSellEffects implements SellEffects {
    private final ChatSendGate gate;

    public LiveSellEffects(ChatSendGate gate) {
        this.gate = gate;
    }

    @Override
    public Map<Integer, HeldStack> readHotbar() {
        Map<Integer, HeldStack> hotbar = new HashMap<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return hotbar;
        PlayerInventory inventory = client.player.getInventory();
        for (int slot = HotbarScan.FIRST_SLOT; slot <= HotbarScan.LAST_SLOT; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            String itemKey = Registries.ITEM.getId(stack.getItem()).toString();
            hotbar.put(slot, new HeldStack(itemKey, stack.getName().getString(), stack.getCount()));
        }
        return hotbar;
    }

    @Override
    public void switchToSlot(int hotbarSlot) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (client.player == null || handler == null) return;
        client.player.getInventory().setSelectedSlot(hotbarSlot);
        // ClientPlayerInteractionManager.syncSelectedSlot() is private, so the
        // sync packet is sent here directly. Setting the local field alone
        // would leave the server believing the old slot is still held.
        handler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
    }

    @Override
    public long ticksSinceLastSend(long tick) {
        return gate.ticksSinceLastSend(tick);
    }

    @Override
    public boolean submit(String command, DelayRule rule) {
        return gate.submit(command, "seller", rule);
    }

    @Override
    public void log(String kind, String message) {
        if (Configs.General.DEBUG_LOGGING.get()) {
            AhToolsMod.LOGGER.info("[{}] {}", kind, message);
        }
    }
}
