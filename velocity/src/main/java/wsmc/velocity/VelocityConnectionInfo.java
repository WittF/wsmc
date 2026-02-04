package wsmc.velocity;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import wsmc.proxy.ProxyInfo;

import javax.annotation.Nullable;

/**
 * Stores WebSocket connection information for Velocity connections.
 * <p>
 * This class is attached to Netty channels via an AttributeKey to store
 * WebSocket handshake requests and proxy information parsed from HTTP headers
 * or PROXY protocol.
 */
public class VelocityConnectionInfo {
    /**
     * AttributeKey for storing connection info in Netty channel attributes.
     */
    public static final AttributeKey<VelocityConnectionInfo> KEY =
        AttributeKey.valueOf("wsmc_connection_info");

    private volatile HttpRequest wsHandshakeRequest;
    private volatile ProxyInfo proxyInfo;

    /**
     * Get the WebSocket handshake HTTP request.
     *
     * @return the HTTP request from WebSocket handshake, or null if not a WebSocket connection
     */
    @Nullable
    public HttpRequest getWsHandshakeRequest() {
        return wsHandshakeRequest;
    }

    /**
     * Set the WebSocket handshake HTTP request.
     *
     * @param wsHandshakeRequest the HTTP request from WebSocket handshake
     */
    public void setWsHandshakeRequest(HttpRequest wsHandshakeRequest) {
        this.wsHandshakeRequest = wsHandshakeRequest;
    }

    /**
     * Get parsed proxy information.
     *
     * @return proxy info containing client IP, proxy chain, and geo information,
     *         or null if not available
     */
    @Nullable
    public ProxyInfo getProxyInfo() {
        return proxyInfo;
    }

    /**
     * Set parsed proxy information.
     *
     * @param proxyInfo the proxy information
     */
    public void setProxyInfo(ProxyInfo proxyInfo) {
        this.proxyInfo = proxyInfo;
    }

    /**
     * Check if this is a WebSocket connection.
     *
     * @return true if this connection was established via WebSocket
     */
    public boolean isWebSocket() {
        return wsHandshakeRequest != null;
    }

    /**
     * Check if this connection is behind a proxy.
     *
     * @return true if proxy information is available and indicates a proxy
     */
    public boolean isBehindProxy() {
        return proxyInfo != null && proxyInfo.isBehindProxy();
    }

    /**
     * Get the real client IP address.
     *
     * @return the client IP from proxy info, or null if not available
     */
    @Nullable
    public String getClientIp() {
        return proxyInfo != null ? proxyInfo.getClientIp() : null;
    }
}
