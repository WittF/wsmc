package wsmc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.handler.codec.http.HttpRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import wsmc.api.IWebSocketServerConnection;
import wsmc.mixin.ServerCommonPacketListenerAccessor;
import wsmc.proxy.ProxyInfo;

/**
 * WSMC command for querying WebSocket connection information.
 * <p>
 * Usage: /wsmc info [player]
 * <p>
 * Displays connection details including:
 * <ul>
 *   <li>Connection type (WebSocket or Vanilla TCP)</li>
 *   <li>Real client IP address</li>
 *   <li>Proxy chain (if behind CDN/proxy)</li>
 *   <li>Geographic information (country, region, city)</li>
 *   <li>Proxy source (HTTP headers or PROXY protocol)</li>
 * </ul>
 */
public class WsmcCommand {
    /**
     * Register the WSMC command with the given dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wsmc")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.literal("info")
                    // /wsmc info - Shows own connection info
                    .executes(WsmcCommand::showOwnInfo)
                    // /wsmc info <player> - Shows target player's connection info
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(WsmcCommand::showPlayerInfo)
                    )
                )
        );
    }

    /**
     * Show connection info for the command sender.
     */
    private static int showOwnInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showConnectionInfo(context.getSource(), player);
    }

    /**
     * Show connection info for the specified player.
     */
    private static int showPlayerInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        return showConnectionInfo(context.getSource(), targetPlayer);
    }

    /**
     * Get the connection object from a ServerPlayer using Mixin accessor.
     * <p>
     * Although all mod loaders (Forge, NeoForge, Fabric) use official Mojang mappings
     * since Minecraft 1.20.2+, Fabric's compile-time environment doesn't expose the
     * getConnection() method. We use a Mixin @Accessor to access the protected field
     * in a clean, platform-independent way without reflection.
     *
     * @param player The player whose connection to retrieve
     * @return The network connection, or null if the player's connection is not available
     */
    private static Connection getPlayerConnection(ServerPlayer player) {
        if (player.connection == null) {
            return null;
        }
        return ((ServerCommonPacketListenerAccessor) player.connection).getConnection();
    }

    /**
     * Display detailed connection information for a player.
     *
     * @param source The command source (for sending feedback)
     * @param player The player whose connection info to display
     * @return 1 if successful, 0 if connection is unavailable
     */
    private static int showConnectionInfo(CommandSourceStack source, ServerPlayer player) {
        Connection connection = getPlayerConnection(player);

        // Check if connection is available
        if (connection == null) {
            source.sendFailure(Component.literal("§c无法获取玩家连接信息（玩家可能正在断开连接）"));
            return 0;
        }

        IWebSocketServerConnection wsConn = IWebSocketServerConnection.of(connection);

        // Start building the message
        StringBuilder message = new StringBuilder();
        message.append("§e=== ").append(player.getName().getString()).append(" 的连接信息 ===§r\n");

        // Get proxy info
        ProxyInfo proxyInfo = wsConn.getProxyInfo();
        HttpRequest handshake = wsConn.getWsHandshakeRequest();

        // Determine connection type
        if (handshake != null) {
            message.append("§a连接类型: §fWebSocket\n");

            // Show WebSocket endpoint
            String uri = handshake.uri();
            if (uri != null && !uri.isEmpty()) {
                message.append("§a握手路径: §f").append(uri).append("\n");
            }
        } else {
            message.append("§a连接类型: §fVanilla TCP\n");
        }

        // Show real IP and proxy information
        if (proxyInfo != null && proxyInfo.isBehindProxy()) {
            String clientIp = proxyInfo.getClientIp();
            if (clientIp != null && !clientIp.isEmpty()) {
                message.append("§a真实IP: §f").append(clientIp).append("\n");
            }

            // Show proxy chain
            if (!proxyInfo.getProxyChain().isEmpty()) {
                message.append("§a代理链: §f");
                message.append(String.join(" → ", proxyInfo.getProxyChain()));
                message.append("\n");
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
                    message.append("§a地理位置: §f").append(geoStr).append("\n");
                }
            }

            // Show proxy source
            ProxyInfo.ProxySource source_type = proxyInfo.getSource();
            if (source_type != null && source_type != ProxyInfo.ProxySource.NONE) {
                String sourceName = switch (source_type) {
                    case HTTP_HEADERS -> "HTTP Headers";
                    case PROXY_PROTOCOL_V1 -> "PROXY Protocol v1";
                    case PROXY_PROTOCOL_V2 -> "PROXY Protocol v2";
                    default -> source_type.toString();
                };
                message.append("§a代理来源: §f").append(sourceName).append("\n");
            }

            // Show protocol metadata if available
            ProxyInfo.ProxyMetadata metadata = proxyInfo.getMetadata();
            if (metadata != null) {
                if (metadata.getProtocol() != null) {
                    message.append("§a原始协议: §f").append(metadata.getProtocol()).append("\n");
                }
                if (metadata.getHost() != null) {
                    message.append("§a原始主机: §f").append(metadata.getHost());
                    if (metadata.getPort() != null) {
                        message.append(":").append(metadata.getPort());
                    }
                    message.append("\n");
                }
            }
        } else {
            // No proxy information available
            String remoteAddress = connection.getRemoteAddress().toString();
            message.append("§a连接地址: §f").append(remoteAddress).append("\n");
            message.append("§7(未检测到代理信息)\n");
        }

        // Footer separator for readability
        message.append("§e==================§r");

        // Send the formatted message
        source.sendSuccess(() -> Component.literal(message.toString()), false);
        return 1;
    }
}
