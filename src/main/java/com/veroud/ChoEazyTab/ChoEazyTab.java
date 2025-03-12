package com.veroud.ChoEazyTab;

import com.electronwill.nightconfig.core.file.FileConfig;
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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.UserManager;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "choeazytab", name = "ChoEazyTab", version = "0.1.0-SNAPSHOT",
        url = "https://veroud.com", description = "Simple Eazy TAB for proxies! By a Aussie...", authors = {"Aidan Heaslip", "Veroud Division 1"})
public class ChoEazyTab {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Map<UUID, String> playerServerMap = new HashMap<>();
    private final LuckPerms luckPerms;
    private boolean luckPermsEnabled;

    @Inject
    public ChoEazyTab(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        logger.info("Loading ChoEazyTab.");
        loadConfig();

        if (luckPermsEnabled && server.getPluginManager().getPlugin("luckperms").isPresent()) {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("LuckPerms detected and enabled via config.");
        } else {
            this.luckPerms = null;
        }
    }

    private void loadConfig() {
        Path configPath = dataDirectory.resolve("config.toml");
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();
            this.luckPermsEnabled = config.contains("modules.luckperms") ? config.get("modules.luckperms") : false;
        } catch (Exception e) {
            logger.error("Failed to load config file!", e);
            this.luckPermsEnabled = false; // Default to enabled
        }
    }

    private void createDefaultConfig(Path path) {
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(path, "enable_luckperms = true\n");
        } catch (IOException e) {
            logger.error("Could not create default config!", e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getScheduler().buildTask(this, this::updateLatency)
                .repeat(java.time.Duration.ofSeconds(5))
                .schedule();
        logger.info("ChoEazyTab initialized successfully!");
    }

    private void updateLatency() {
        for (Player player : server.getAllPlayers()) {
            for (TabListEntry entry : player.getTabList().getEntries()) {
                UUID playerId = entry.getProfile().getId();
                server.getPlayer(playerId).ifPresent(p -> entry.setLatency((int) p.getPing()));
            }
        }
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
            player.getTabList().getEntries().forEach(entry ->
                    player.getTabList().removeEntry(entry.getProfile().getId())
            );

            for (UUID uuid : playerServerMap.keySet()) {
                Optional<Player> optionalPlayer = server.getPlayer(uuid);
                optionalPlayer.ifPresent(p -> {
                    String serverName = playerServerMap.getOrDefault(uuid, "Unknown");
                    getPlayerPrefix(uuid).thenAccept(prefix -> {
                        TabListEntry entry = TabListEntry.builder()
                                .tabList(player.getTabList())
                                .profile(p.getGameProfile())
                                .displayName(Component.text(prefix + p.getUsername() + " §7[" + serverName + "]"))
                                .latency((int) Math.min(p.getPing(), Integer.MAX_VALUE))
                                .gameMode(3)
                                .build();
                        player.getTabList().addEntry(entry);
                    });
                });
            }
        }
    }

    private CompletableFuture<String> getPlayerPrefix(UUID uuid) {
        if (!luckPermsEnabled || luckPerms == null) {
            return CompletableFuture.completedFuture(""); // LuckPerms disabled
        }

        UserManager userManager = luckPerms.getUserManager();
        return userManager.loadUser(uuid).thenApply(user -> {
            if (user == null) {
                return "";
            }
            CachedMetaData metaData = user.getCachedData().getMetaData();
            return metaData.getPrefix() != null ? metaData.getPrefix() : "";
        });
    }
}