[简体中文](README.md) | **English**

# WSMC
Enable Websocket support for Minecraft Java.
Since most CDN providers(at least for their free tier) do not support raw TCP proxy, with the help of this mod, the owner can now hide the server behind a CDN and let the players connect via WebSocket, thus preventing DDoS attacks.

For Minecraft Forge, Neoforge and Fabric:
* **1.20.6** - 1.21.4 (Forge, Neoforge, Fabric)
* **1.20.2** - 1.20.4 (All loaders)
* **1.20.1** (All loaders)
* **1.18.2** - 1.20 (Older branch)

**Note**: Minecraft 1.20.5 and 1.21.2 do not have Forge support, only Neoforge and Fabric are available.

This branch is for 1.20.5 and above.

This mod runs standalone and does not have any dependency.

## When this mod is installed on a server:
* The server would allow players to connect via WebSocket.
* Players can still join using vanilla TCP.
* The server accepts and handles TCP and WebSocket connections on the same listening port.
* Without installing this mod on the client side, a player can still join a server that has this mod using vanilla TCP.
* The server can acquire client statistics (e.g., real IP) from the WebSocket handshake..

## When this mod is installed on a client:
* The client can join WebSocket-enabled servers using URI like `ws://hostname.com:port/path_to_minecraft_endpoint`.
* The client can join any servers using vanilla TCP using the old syntax, e.g. `hostname_or_ip:port`.

## Note
* This mod does not affect any gameplay.
* This mod does not modify any GUI.
* Vanilla clients can join your server even if you install this mod, note that other mods you have may prevent vanilla clients from joining.
* Installing this mod on your client does not prevent you from joining other vanilla or mod servers.
* The server can still get the real IP of the players who joined via CDN-proxied WebSocket.
* This mod is compatible with other TCP-WebSocket proxies, such as websocat.

## Client Options
Sometimes the DNS returns a slow IP for the HTTP hostname (ws) or the SNI (wss). The client may want to control how to resolve the IP address.

The client can optionally control the HTTP hostname and the SNI used during WebSocket handshake:
```
Insecure WebSocket connection with http hostname specified:
ws://host.com@ip.ip.ip.ip

Specify sni and http hostname to the same value(sni-host.com), resolve server IP from ip.ip.ip.ip:
wss://sni-host.com@ip.ip.ip.ip

Set sni and http hostname differently, resolve server IP from host.com:
wss://sni.com:@host.com[:port]

Set sni and http hostname differently, resolve server IP from sni.com:
wss://:host.com@sni.com[:port]

Set sni, http hostname, and the server address seperately
wss://sni.com:host.com@ip.ip.ip.ip
```

Port and path specification can be appended at the same time.

## Configuration
The configuration of this mod is passed in the "system properties". You can use `-D` in the JVM command line to pass such options.

| Property Key               | Type     | Usage                                                                                                                                                                                        | Side          | Default | Example  |
|----------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|---------|----------|
| wsmc.disableVanillaTCP     | boolean  | Disable vanilla TCP login and server status.                                                                                                                                                 | Server        | false   | true     |
| wsmc.wsmcEndpoint          | string   | Set the WebSocket Endpoint for Minecraft login and server status. If this property does not exist, a client can join the game via ANY WebSocket Endpoint. Must start with /, case-sensitive. | Server        | Not set | /mc      |
| wsmc.debug                 | boolean  | Show debug logs.                                                                                                                                                                             | Server Client | false   | true     |
| wsmc.dumpBytes             | boolean  | Dump raw WebSocket binary frames. Work only if `wsmc.debug` is set to `true`.                                                                                                                | Server Client | false   | true     |
| wsmc.maxFramePayloadLength | integer  | Maximum allowable frame payload length. Setting this value to your modpack's requirement else Netty will throw error "Max frame length of x has been exceeded".                              | Server Client | 65536   | 65536    |
| wsmc.enableProxyProtocol   | boolean  | Enable HAProxy PROXY Protocol v1/v2 support. Used to get real client IP from load balancers like HAProxy or nginx.                                                                              | Server        | false   | true     |

## Advanced Proxy Features

WSMC provides powerful proxy information parsing capabilities, supporting multiple proxy protocols and HTTP header standards.

### Supported Proxy Protocols

1. **HTTP Proxy Headers** (automatically enabled)
   - `X-Real-IP` (highest priority)
   - `CF-Connecting-IP` (Cloudflare)
   - `True-Client-IP` (Akamai/Cloudflare Enterprise)
   - `X-Forwarded-For` (standard proxy chain)
   - `Forwarded` (RFC 7239)
   - Geographic information: `CF-IPCountry`, `CF-Region`, `CF-City`

2. **PROXY Protocol v1/v2** (requires configuration)
   - HAProxy PROXY Protocol v1
   - HAProxy PROXY Protocol v2
   - Works with both TCP and WebSocket connections

### API Usage Example

Server-side plugins can access parsed proxy information via the `IWebSocketServerConnection` API:

```java
import wsmc.api.IWebSocketServerConnection;
import wsmc.proxy.ProxyInfo;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;

MinecraftServer server = ...;

// Go through all established connections
for (Connection conn: server.getConnection().getConnections()) {
    ProxyInfo proxyInfo = IWebSocketServerConnection.of(conn).getProxyInfo();

    if (proxyInfo != null && proxyInfo.isBehindProxy()) {
        // Get client's real IP
        String clientRealIP = proxyInfo.getClientIp();

        // Get proxy chain (all proxies from client to server)
        List<String> proxyChain = proxyInfo.getProxyChain();

        // Get geographic information (if available)
        ProxyInfo.GeoInfo geoInfo = proxyInfo.getGeoInfo();
        if (geoInfo != null) {
            String country = geoInfo.getCountryCode();  // e.g., "US"
            String region = geoInfo.getRegionCode();    // e.g., "CA"
            String city = geoInfo.getCityName();        // e.g., "San Francisco"
        }

        // Get proxy metadata
        ProxyInfo.ProxyMetadata metadata = proxyInfo.getMetadata();
        if (metadata != null) {
            String protocol = metadata.getProtocol();   // e.g., "https"
            String host = metadata.getHost();           // original hostname
            Integer port = metadata.getPort();          // original port
        }

        // Get proxy information source
        ProxyInfo.ProxySource source = proxyInfo.getSource();
        // Possible values: HTTP_HEADERS, PROXY_PROTOCOL_V1, PROXY_PROTOCOL_V2
    }
}
```

### Configuration Examples

#### Enable PROXY Protocol (for HAProxy/nginx)

Add to server startup parameters:
```
-Dwsmc.enableProxyProtocol=true
```

#### HAProxy Configuration Example

```haproxy
frontend minecraft
    bind *:25565
    mode tcp
    default_backend minecraft_servers

backend minecraft_servers
    mode tcp
    server mc1 127.0.0.1:25566 send-proxy-v2
```

#### nginx Configuration Example (requires stream module)

```nginx
stream {
    upstream minecraft {
        server 127.0.0.1:25566;
    }

    server {
        listen 25565;
        proxy_pass minecraft;
        proxy_protocol on;
    }
}
```

### Header Parsing Priority

When multiple proxy headers are present, WSMC parses the client IP in the following priority:

1. `X-Real-IP`
2. `CF-Connecting-IP` (Cloudflare)
3. `True-Client-IP` (Akamai/Cloudflare Enterprise)
4. `X-Forwarded-For` (takes the first IP)
5. `Forwarded` (RFC 7239)

**Note**: PROXY Protocol has higher priority than HTTP headers. If both are enabled, information from PROXY Protocol will be used first.

## Dependencies
### Forge Version
* `netty-codec-http` for handling HTTP and WebSocket

### Fabric Version
You need to install Fabric Loader and then install this mod. Fabric API is optional but highly recommended.

## For developers
To modify and debug the code, first import the "forge" or "fabric" folder as a Gradle project in Eclipse IDE, and then run the gradle task `genEclipseRuns`.

Windows users need to replace `./` and `../` with `.\` and `..\`, respectively.

The codebase uses Minecraft official mapping.

On the server side, if a client joins via WebSocket, you can access the WebSocket handshake request to retrieve real IP and other information:

```java
import wsmc.api.IWebSocketServerConnection;
import net.minecraft.network.Connection;
import io.netty.handler.codec.http.HttpRequest;

Connection conn = // ... get connection from server
HttpRequest handshake = IWebSocketServerConnection.of(conn).getWsHandshakeRequest();
if (handshake != null) {
    String realIP = handshake.headers().get("X-Forwarded-For");
    String country = handshake.headers().get("CF-IPCountry");
}
```

This is useful for obtaining the client's real IP address and geolocation when the Minecraft server is behind a reverse proxy (e.g. a CDN).

### Compile the Project
```bash
git clone https://github.com/WittF/wsmc.git
cd wsmc

# Compile Fabric version
cd fabric && ./gradlew build

# Compile Forge version
cd forge && ./gradlew build

# Compile Neoforge version
cd neoforge && ./gradlew build
```

### To specify JRE path (Since 1.18.1, Minecraft requires Java 17):
```
./gradlew -Dorg.gradle.java.home=/path_to_jdk_directory <commands>
```
* Since 1.18.1, Minecraft requires Java 17
* Since 1.20.5, Minecraft requires Java 21
