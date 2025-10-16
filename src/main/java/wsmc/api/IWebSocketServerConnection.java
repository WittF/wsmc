package wsmc.api;

import javax.annotation.Nullable;

import io.netty.handler.codec.http.HttpRequest;
import net.minecraft.network.Connection;
import wsmc.proxy.ProxyInfo;

/**
 * This interface is mixined to {@link net.minecraft.network.Connection}.
 * So you can cast between {@link net.minecraft.network.Connection} and this interface.
 * <p>
 * On the server side, you can use this interface to retrieve the HTTP header during the
 * WebSocket handshake and parsed proxy information.
 * <p>
 * For instance, the server can get the real client IP from proxy information:
 * <pre>
 * MinecraftServer server = ...;
 * // Go through all established connections
 * for (Connection conn: server.getConnection().getConnections()) {
 * 	ProxyInfo proxyInfo = IWebSocketServerConnection.of(conn).getProxyInfo();
 * 	if (proxyInfo != null && proxyInfo.isBehindProxy()) {
 * 		String clientRealIP = proxyInfo.getClientIp();
 * 		ProxyInfo.GeoInfo geoInfo = proxyInfo.getGeoInfo();
 * 		// Your logic
 * 	}
 * }
 * </pre>
 * This is extremely useful if the client connects to the server via a proxy or CDN.
 */
public interface IWebSocketServerConnection {
	/**
	 * Only available on the server side.
	 * @return the http request of the WebSocket handshake.
	 */
	@Nullable
	HttpRequest getWsHandshakeRequest();

	/**
	 * Get parsed proxy information from HTTP headers or PROXY protocol.
	 * Only available on the server side for WebSocket connections.
	 *
	 * @return ProxyInfo containing client IP, proxy chain, and geo information,
	 *         or null if not available
	 */
	@Nullable
	ProxyInfo getProxyInfo();

	@Nullable
	public static IWebSocketServerConnection of(Connection connection) {
		return (IWebSocketServerConnection)(Object)connection;
	}
}
