package com.veroud.ChoEazyTab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "choeazytab", name = "ChoEazyTab", version = "0.1.0-SNAPSHOT",
        url = "https://veroud.com", description = "I did it!", authors = {"Me"})
public class ChoEazyTab {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public ChoEazyTab(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        logger.info("MrAteyo was here!");
        logger.info("Loading ChoEazyTab.");
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Do some operation demanding access to the Velocity API here.
        // For instance, we could register an event:
        server.getEventManager().register(this, new ChoEazyTabListener());
    }
    public class ChoEazyTabListener {

        @Subscribe(order = PostOrder.EARLY)
        public void onPlayerChat(PlayerChatEvent event) {
            // do something here
        }

    }
}