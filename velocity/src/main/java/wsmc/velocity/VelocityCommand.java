package wsmc.velocity;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import wsmc.proxy.ProxyInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * WSMC command for Velocity.
 * <p>
 * Usage: /wsmc info [player]
 * <p>
 * Displays connection details including connection type, real IP, proxy chain,
 * and geographic information.
 */
public class VelocityCommand {
    private static ProxyServer server;
    private static Logger logger;

    /**
     * Register the WSMC command with Velocity.
     *
     * @param proxyServer The Velocity proxy server
     * @param log         Logger for debug output
     */
    public static void register(ProxyServer proxyServer, Logger log) {
        server = proxyServer;
        logger = log;

        LiteralArgumentBuilder<CommandSource> command = LiteralArgumentBuilder
            .<CommandSource>literal("wsmc")
            .then(LiteralArgumentBuilder.<CommandSource>literal("info")
                .executes(VelocityCommand::showOwnInfo)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                    .suggests(VelocityCommand::suggestPlayers)
                    .executes(VelocityCommand::showPlayerInfo)
                )
            );

        BrigadierCommand brigadierCommand = new BrigadierCommand(command);
        server.getCommandManager().register("wsmc", brigadierCommand);

        logger.info("WSMC: Registered /wsmc command");
    }

    /**
     * Suggest online player names for tab completion.
     */
    private static CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        server.getAllPlayers().stream()
            .map(Player::getUsername)
            .filter(name -> name.toLowerCase().startsWith(input))
            .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * Show connection info for the command sender.
     */
    private static int showOwnInfo(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("This command can only be used by players.")
                .color(NamedTextColor.RED));
            return 0;
        }

        return showConnectionInfo(source, (Player) source);
    }

    /**
     * Show connection info for the specified player.
     */
    private static int showPlayerInfo(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");

        Optional<Player> targetPlayer = server.getPlayer(playerName);
        if (targetPlayer.isEmpty()) {
            source.sendMessage(Component.text("Player not found: " + playerName)
                .color(NamedTextColor.RED));
            return 0;
        }

        return showConnectionInfo(source, targetPlayer.get());
    }

    /**
     * Display detailed connection information for a player.
     */
    private static int showConnectionInfo(CommandSource source, Player player) {
        // Try to get the channel from the player's connection
        Channel channel = getPlayerChannel(player);

        TextComponent.Builder message = Component.text();
        message.append(Component.text("=== " + player.getUsername() + " Connection Info ===")
            .color(NamedTextColor.YELLOW));
        message.append(Component.newline());

        VelocityConnectionInfo connInfo = null;
        if (channel != null) {
            connInfo = channel.attr(VelocityConnectionInfo.KEY).get();
        }

        // Connection type
        if (connInfo != null && connInfo.isWebSocket()) {
            message.append(Component.text("Connection Type: ").color(NamedTextColor.GREEN));
            message.append(Component.text("WebSocket").color(NamedTextColor.WHITE));
            message.append(Component.newline());

            // Show WebSocket endpoint
            if (connInfo.getWsHandshakeRequest() != null) {
                String uri = connInfo.getWsHandshakeRequest().uri();
                if (uri != null && !uri.isEmpty()) {
                    message.append(Component.text("Handshake Path: ").color(NamedTextColor.GREEN));
                    message.append(Component.text(uri).color(NamedTextColor.WHITE));
                    message.append(Component.newline());
                }
            }
        } else {
            message.append(Component.text("Connection Type: ").color(NamedTextColor.GREEN));
            message.append(Component.text("Vanilla TCP").color(NamedTextColor.WHITE));
            message.append(Component.newline());
        }

        // Show proxy information
        ProxyInfo proxyInfo = connInfo != null ? connInfo.getProxyInfo() : null;
        if (proxyInfo != null && proxyInfo.isBehindProxy()) {
            String clientIp = proxyInfo.getClientIp();
            if (clientIp != null && !clientIp.isEmpty()) {
                message.append(Component.text("Real IP: ").color(NamedTextColor.GREEN));
                message.append(Component.text(clientIp).color(NamedTextColor.WHITE));
                message.append(Component.newline());
            }

            // Show proxy chain
            if (!proxyInfo.getProxyChain().isEmpty()) {
                message.append(Component.text("Proxy Chain: ").color(NamedTextColor.GREEN));
                message.append(Component.text(String.join(" -> ", proxyInfo.getProxyChain()))
                    .color(NamedTextColor.WHITE));
                message.append(Component.newline());
            }

            // Show geographic information
            ProxyInfo.GeoInfo geoInfo = proxyInfo.getGeoInfo();
            if (geoInfo != null) {
                StringBuilder geoStr = new StringBuilder();
                if (geoInfo.getCountryCode() != null) {
                    geoStr.append(geoInfo.getCountryCode());
                }
                if (geoInfo.getRegionCode() != null) {
                    if (geoStr.length() > 0) geoStr.append(", ");
                    geoStr.append(geoInfo.getRegionCode());
                }
                if (geoInfo.getCityName() != null) {
                    if (geoStr.length() > 0) geoStr.append(", ");
                    geoStr.append(geoInfo.getCityName());
                }
                if (geoStr.length() > 0) {
                    message.append(Component.text("Location: ").color(NamedTextColor.GREEN));
                    message.append(Component.text(geoStr.toString()).color(NamedTextColor.WHITE));
                    message.append(Component.newline());
                }
            }

            // Show proxy source
            ProxyInfo.ProxySource proxySource = proxyInfo.getSource();
            if (proxySource != null && proxySource != ProxyInfo.ProxySource.NONE) {
                String sourceName = switch (proxySource) {
                    case HTTP_HEADERS -> "HTTP Headers";
                    case PROXY_PROTOCOL_V1 -> "PROXY Protocol v1";
                    case PROXY_PROTOCOL_V2 -> "PROXY Protocol v2";
                    default -> proxySource.toString();
                };
                message.append(Component.text("Proxy Source: ").color(NamedTextColor.GREEN));
                message.append(Component.text(sourceName).color(NamedTextColor.WHITE));
                message.append(Component.newline());
            }
        } else {
            // No proxy information
            message.append(Component.text("Remote Address: ").color(NamedTextColor.GREEN));
            message.append(Component.text(player.getRemoteAddress().toString()).color(NamedTextColor.WHITE));
            message.append(Component.newline());
            message.append(Component.text("(No proxy info detected)").color(NamedTextColor.GRAY));
            message.append(Component.newline());
        }

        message.append(Component.text("==================").color(NamedTextColor.YELLOW));

        source.sendMessage(message.build());
        return 1;
    }

    /**
     * Get the Netty channel from a Velocity player using reflection.
     */
    private static Channel getPlayerChannel(Player player) {
        try {
            // Get the ConnectedPlayer implementation
            Object connectedPlayer = player;

            // Try to find the connection field
            Field connectionField = findField(connectedPlayer.getClass(), "connection");
            if (connectionField == null) {
                // Try alternative field name
                connectionField = findField(connectedPlayer.getClass(), "minecraftConnection");
            }

            if (connectionField == null) {
                logger.debug("WSMC: Cannot find connection field in player class");
                return null;
            }

            connectionField.setAccessible(true);
            Object connection = connectionField.get(connectedPlayer);

            if (connection == null) {
                return null;
            }

            // Get the channel from the connection
            Method getChannelMethod = findMethod(connection.getClass(), "getChannel");
            if (getChannelMethod == null) {
                // Try field access
                Field channelField = findField(connection.getClass(), "channel");
                if (channelField != null) {
                    channelField.setAccessible(true);
                    return (Channel) channelField.get(connection);
                }
                return null;
            }

            getChannelMethod.setAccessible(true);
            return (Channel) getChannelMethod.invoke(connection);

        } catch (Exception e) {
            logger.debug("WSMC: Failed to get player channel via reflection: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Find a field by name in a class or its superclasses.
     */
    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Find a method by name in a class or its superclasses.
     */
    private static Method findMethod(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
