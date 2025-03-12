package com.veroud.ChoEazyTab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Plugin(id = "choeazytab", name = "ChoEazyTab", version = "0.1.0-SNAPSHOT",
        url = "https://veroud.com", description = "Simple Eazy TAB for proxies! By a Aussie...", authors = {"Aidan Heaslip", "Veroud Division 1"})
public class ChoEazyTab {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Map<UUID, String> playerServerMap = new HashMap<>();

    @Inject
    public ChoEazyTab(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        logger.info("Loading ChoEazyTab.");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, this);
        logger.info("ChoEazyTab initialized successfully!");
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        playerServerMap.put(player.getUniqueId(), "Unknown");
        updateTabList();
    }

    @Subscribe
    public void onServerChange(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        playerServerMap.put(player.getUniqueId(), serverName);
        updateTabList();
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        playerServerMap.remove(player.getUniqueId());
        updateTabList();
    }

    private void updateTabList() {
        logger.info("Updating Tab List for all players...");

        for (Player player : server.getAllPlayers()) {
            // Clear the tab list
            player.getTabList().getEntries().forEach(entry ->
                    player.getTabList().removeEntry(entry.getProfile().getId())
            );

            // Rebuild the tab list with updated player data
            for (UUID uuid : playerServerMap.keySet()) {
                Optional<Player> optionalPlayer = server.getPlayer(uuid);
                optionalPlayer.ifPresent(p -> {
                    String serverName = playerServerMap.getOrDefault(uuid, "Unknown");

                    logger.info("Adding {} to tab list with server: {}", p.getUsername(), serverName);

                    // Create a new TabListEntry using the builder
                    TabListEntry entry = TabListEntry.builder()
                            .tabList(player.getTabList())
                            .profile(p.getGameProfile())
                            .displayName(Component.text(p.getUsername() + " §7[" + serverName + "]"))
                            .latency((int) Math.min(p.getPing(), Integer.MAX_VALUE)) // Ensure no overflow
                            .gameMode(3) // Spectator mode for example
                            .build();

                    player.getTabList().addEntry(entry);
                });
            }
        }
    }
}
