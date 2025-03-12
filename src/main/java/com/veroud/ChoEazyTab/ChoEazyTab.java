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
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "choeazytab", name = "ChoEazyTab", version = "0.1.0-SNAPSHOT",
        url = "https://veroud.com", description = "Simple Eazy TAB for proxies! By a Aussie...", authors = {"Aidan Heaslip", "Veroud Division 1"})
public class ChoEazyTab {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Map<UUID, String> playerServerMap = new HashMap<>();
    private LuckPerms luckPerms;
    private boolean luckPermsEnabled;

    @Inject
    public ChoEazyTab(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        logger.info("Loading ChoEazyTab.");
        loadConfig();
    }

    private void loadConfig() {
        Path configPath = dataDirectory.resolve("config.toml");
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();
            this.luckPermsEnabled = config.getOrElse("modules.luckperms", false);
        } catch (Exception e) {
            logger.error("Failed to load config file!", e);
            this.luckPermsEnabled = false;
        }
    }

    private void createDefaultConfig(Path path) {
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(path, "modules.luckperms = true\n");
        } catch (IOException e) {
            logger.error("Could not create default config!", e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getScheduler().buildTask(this, this::updateTabList)
                .repeat(java.time.Duration.ofSeconds(5))
                .schedule();
        logger.info("ChoEazyTab initialized successfully!");
        if (luckPermsEnabled && server.getPluginManager().getPlugin("luckperms").isPresent()) {
            try {
                this.luckPerms = LuckPermsProvider.get();
                logger.info("LuckPerms detected and enabled via config.");
            } catch (Exception e) {
                logger.error("Failed to load LuckPerms API!", e);
                this.luckPerms = null;
            }
        } else {
            this.luckPerms = null;
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
        playerServerMap.remove(event.getPlayer().getUniqueId());
        updateTabList();
    }

    private void updateTabList() {
        logger.info("Updating Tab List for all players...");
        for (Player player : server.getAllPlayers()) {
            // Remove all existing entries manually
            player.getTabList().getEntries().forEach(entry -> player.getTabList().removeEntry(entry.getProfile().getId()));

            for (UUID uuid : playerServerMap.keySet()) {
                server.getPlayer(uuid).ifPresent(p -> {
                    String serverName = playerServerMap.getOrDefault(uuid, "Unknown");
                    getPlayerPrefix(uuid).thenAccept(prefix -> {
                        TabListEntry entry = TabListEntry.builder()
                                .tabList(player.getTabList())
                                .profile(p.getGameProfile())
                                .displayName(Component.text(prefix + p.getUsername() + " §7[" + serverName + "]"))
                                .latency((int) Math.min(p.getPing(), Integer.MAX_VALUE))
                                .build();
                        player.getTabList().addEntry(entry);
                    });
                });
            }
        }
    }


    private CompletableFuture<String> getPlayerPrefix(UUID uuid) {
        if (!luckPermsEnabled || luckPerms == null) {
            return CompletableFuture.completedFuture("");
        }
        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) return "";
            CachedMetaData metaData = user.getCachedData().getMetaData();
            return metaData.getPrefix() != null ? metaData.getPrefix() : "";
        });
    }
}
