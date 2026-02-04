package wsmc.velocity;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import wsmc.WSMC;
import wsmc.proxy.ProxyInfo;

/**
 * Handler for HAProxy PROXY Protocol v1/v2 messages in Velocity.
 * <p>
 * This handler processes PROXY Protocol headers sent by load balancers
 * and stores the client IP information in VelocityConnectionInfo.
 */
public class VelocityProxyProtocolHandler extends ChannelInboundHandlerAdapter {
    private final VelocityConnectionInfo connectionInfo;

    public VelocityProxyProtocolHandler(VelocityConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HAProxyMessage) {
            HAProxyMessage proxyMsg = (HAProxyMessage) msg;

            try {
                // Only process PROXY commands (not LOCAL)
                if (proxyMsg.command() == HAProxyCommand.PROXY) {
                    ProxyInfo proxyInfo = parseProxyMessage(proxyMsg);
                    connectionInfo.setProxyInfo(proxyInfo);

                    WSMC.debug("PROXY Protocol: {}", proxyInfo);
                } else {
                    WSMC.debug("PROXY Protocol LOCAL command received (health check)");
                }
            } finally {
                // Always release the message and remove this handler
                proxyMsg.release();
                ctx.pipeline().remove(this);
            }
        } else {
            // Not a PROXY Protocol message, pass it through
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Parse HAProxyMessage into ProxyInfo.
     *
     * @param msg HAProxyMessage from decoder
     * @return ProxyInfo containing client IP and proxy metadata
     */
    private ProxyInfo parseProxyMessage(HAProxyMessage msg) {
        ProxyInfo.Builder builder = ProxyInfo.builder();

        // Set source based on protocol version
        ProxyInfo.ProxySource source = msg.protocolVersion() == HAProxyProtocolVersion.V1
            ? ProxyInfo.ProxySource.PROXY_PROTOCOL_V1
            : ProxyInfo.ProxySource.PROXY_PROTOCOL_V2;
        builder.source(source);

        // Extract client IP (source address)
        String clientIp = msg.sourceAddress();
        if (clientIp != null) {
            builder.clientIp(clientIp);
            builder.addProxyIp(clientIp);
        }

        // Extract destination address
        String destAddress = msg.destinationAddress();
        Integer destPort = msg.destinationPort();

        // Store metadata
        if (destAddress != null || destPort != null) {
            String protocol = msg.proxiedProtocol().name();
            builder.metadata(protocol, destAddress, destPort);
        }

        return builder.build();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        WSMC.warn("Error processing PROXY Protocol: {}", cause.getMessage());
        ctx.close();
    }
}
