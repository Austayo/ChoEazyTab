package net.veroud.ChoEazyTab;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "myfirstplugin", name = "My First Plugin", version = "0.1.0-SNAPSHOT",
        url = "https://example.org", description = "I did it!", authors = {"Me"})
public class ChoEazyTab {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public ChoEazyTab(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        logger.info("MrAteyo was here!");
        logger.info("Loading ChoEazyTab.");
    }
}