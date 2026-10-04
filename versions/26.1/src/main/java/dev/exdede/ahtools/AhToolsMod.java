package dev.exdede.ahtools;

import dev.exdede.ahtools.config.Configs;
import dev.exdede.ahtools.config.Keybinds;
import dev.exdede.ahtools.hud.HudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class AhToolsMod implements ClientModInitializer {
    public static final String MOD_ID = "ahtools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("AHTools initializing");

        Configs.loadFromFile();
        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        AhTools.INSTANCE = new AhTools(dataDir);
        Keybinds.register();
        HudRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Keybinds.poll(client);
            AhTools.INSTANCE.onClientTick();
        });

        // Only game (system) messages. Server plugins send their sale and
        // listing lines this way, and player chat is exactly the feed a
        // stranger controls, so it never reaches the seller or the tracker.
        ClientReceiveMessageEvents.GAME.register(
            (message, overlay) -> { if (!overlay) AhTools.INSTANCE.onChatLine(message.getString()); });

        ClientPlayConnectionEvents.DISCONNECT.register(
            (handler, client) -> AhTools.INSTANCE.onDisconnect());
        ClientPlayConnectionEvents.JOIN.register(
            (handler, sender, client) -> AhTools.INSTANCE.onWorldChange());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(
            (client, world) -> AhTools.INSTANCE.onWorldChange());
    }
}
