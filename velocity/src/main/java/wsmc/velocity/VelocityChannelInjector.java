package wsmc.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import org.slf4j.Logger;
import wsmc.ConfigHelper;
import wsmc.HttpGetSniffer;
import wsmc.proxy.ProxyHeaderParser;
import wsmc.proxy.ProxyInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Injects WebSocket support into Velocity's Netty pipeline using reflection.
 * <p>
 * This class accesses Velocity's internal ConnectionManager and wraps the
 * ServerChannelInitializer to add WebSocket protocol detection handlers.
 * <p>
 * Reflection path:
 * <pre>
 * VelocityServer
 *   └─ cm (ConnectionManager)
 *        └─ serverChannelInitializer (ServerChannelInitializerHolder)
 *             └─ get() -> ChannelInitializer
 *             └─ set(ChannelInitializer)
 * </pre>
 */
public class VelocityChannelInjector {
    private static Logger logger;

    /**
     * Inject WebSocket handlers into Velocity's Netty pipeline.
     *
     * @param server The Velocity proxy server instance
     * @param log    Logger for debug output
     * @throws Exception if reflection fails
     */
    public static void inject(ProxyServer server, Logger log) throws Exception {
        logger = log;

        // Step 1: Get the VelocityServer implementation class
        Object velocityServer = server;
        Class<?> velocityServerClass = velocityServer.getClass();

        // Step 2: Get ConnectionManager field (named "cm")
        Field cmField = findField(velocityServerClass, "cm");
        if (cmField == null) {
            throw new IllegalStateException("Cannot find 'cm' field in VelocityServer. " +
                "Velocity internal API may have changed.");
        }
        cmField.setAccessible(true);
        Object connectionManager = cmField.get(velocityServer);

        // Step 3: Get ServerChannelInitializerHolder field
        Field holderField = findField(connectionManager.getClass(), "serverChannelInitializer");
        if (holderField == null) {
            throw new IllegalStateException("Cannot find 'serverChannelInitializer' field in ConnectionManager. " +
                "Velocity internal API may have changed.");
        }
        holderField.setAccessible(true);
        Object holder = holderField.get(connectionManager);

        // Step 4: Get the original ChannelInitializer
        Method getMethod = holder.getClass().getMethod("get");
        @SuppressWarnings("unchecked")
        ChannelInitializer<Channel> original = (ChannelInitializer<Channel>) getMethod.invoke(holder);

        // Step 5: Create wrapped ChannelInitializer that adds WebSocket support
        ChannelInitializer<Channel> wrapped = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                // First, call the original initializer
                Method initMethod = findMethod(original.getClass(), "initChannel", Channel.class);
                if (initMethod == null) {
                    throw new IllegalStateException("Cannot find initChannel method");
                }
                initMethod.setAccessible(true);
                initMethod.invoke(original, ch);

                // Now inject our WebSocket handlers
                injectWsmcHandlers(ch);
            }
        };

        // Step 6: Set the wrapped ChannelInitializer
        Method setMethod = holder.getClass().getMethod("set", ChannelInitializer.class);
        setMethod.invoke(holder, wrapped);

        logger.info("WSMC: Successfully injected WebSocket handlers into Velocity pipeline!");
    }

    /**
     * Inject WSMC handlers into a channel's pipeline.
     *
     * @param ch The channel to inject handlers into
     */
    private static void injectWsmcHandlers(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // Store connection metadata using channel attributes
        VelocityConnectionInfo connInfo = new VelocityConnectionInfo();
        ch.attr(VelocityConnectionInfo.KEY).set(connInfo);

        // Add PROXY Protocol support if enabled
        if (ConfigHelper.isProxyProtocolEnabled()) {
            pipeline.addFirst("WsmcProxyProtocolHandler", new VelocityProxyProtocolHandler(connInfo));
            pipeline.addFirst("WsmcProxyProtocolDecoder", new HAProxyMessageDecoder());
        }

        // Create HTTP GET sniffer with callback for WebSocket handshake
        HttpGetSniffer sniffer = new HttpGetSniffer(httpRequest -> {
            // Store the WebSocket handshake request
            connInfo.setWsHandshakeRequest(httpRequest);

            // Parse and store proxy information from HTTP headers
            ProxyInfo proxyInfo = ProxyHeaderParser.parse(httpRequest.headers());

            // Only set if not already set by PROXY Protocol handler
            if (connInfo.getProxyInfo() == null) {
                connInfo.setProxyInfo(proxyInfo);
            }
        });

        // Add HttpGetSniffer at the very beginning of the pipeline
        // This ensures it sees raw bytes before any other decoder
        pipeline.addFirst("WsmcHttpGetSniffer", sniffer);
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
     * Find a method by name and parameter types in a class or its superclasses.
     */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Find the first existing handler from the given names.
     */
    private static String findHandlerPosition(ChannelPipeline pipeline, String... handlerNames) {
        for (String name : handlerNames) {
            if (pipeline.get(name) != null) {
                return name;
            }
        }
        return null;
    }
}
