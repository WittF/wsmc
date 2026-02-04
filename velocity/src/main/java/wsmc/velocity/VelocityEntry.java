package wsmc.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

/**
 * WSMC Velocity plugin entry point.
 * <p>
 * Enables WebSocket connections to Velocity proxy, allowing Minecraft clients
 * to connect through CDN services like Cloudflare.
 */
@Plugin(
    id = "wsmc",
    name = "WSMC",
    version = "0.7.0",
    description = "WebSocket support for Velocity proxy",
    authors = {"wsmc"}
)
public class VelocityEntry {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public VelocityEntry(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("WSMC: Initializing WebSocket support for Velocity...");

        try {
            // Inject WebSocket handlers into Velocity's Netty pipeline
            VelocityChannelInjector.inject(server, logger);

            // Register /wsmc command
            VelocityCommand.register(server, logger);

            logger.info("WSMC: WebSocket support enabled successfully!");
        } catch (Exception e) {
            logger.error("WSMC: Failed to initialize WebSocket support", e);
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
