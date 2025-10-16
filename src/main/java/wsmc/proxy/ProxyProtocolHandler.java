package wsmc.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import wsmc.IConnectionEx;
import wsmc.WSMC;

import net.minecraft.network.Connection;

/**
 * Handler for HAProxy PROXY Protocol v1/v2 messages.
 * <p>
 * This handler is inserted before {@link wsmc.HttpGetSniffer} to process
 * PROXY Protocol headers sent by load balancers like HAProxy or nginx.
 * <p>
 * After processing a PROXY Protocol message, it:
 * <ul>
 *   <li>Extracts client IP and proxy information</li>
 *   <li>Stores it in the {@link Connection} via {@link IConnectionEx}</li>
 *   <li>Removes itself from the pipeline</li>
 * </ul>
 */
public class ProxyProtocolHandler extends ChannelInboundHandlerAdapter {
	private final Connection connection;

	public ProxyProtocolHandler(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HAProxyMessage) {
			HAProxyMessage proxyMsg = (HAProxyMessage) msg;

			try {
				// Only process PROXY commands (not LOCAL)
				if (proxyMsg.command() == HAProxyCommand.PROXY) {
					ProxyInfo proxyInfo = parseProxyMessage(proxyMsg);
					IConnectionEx connectionEx = (IConnectionEx) connection;
					connectionEx.setProxyInfo(proxyInfo);

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
	 * Parse HAProxyMessage into ProxyInfo
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
			// Protocol is TCP/UDP from PROXY Protocol
			String protocol = msg.proxiedProtocol().name();
			builder.metadata(protocol, destAddress, destPort);
		}

		return builder.build();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		WSMC.warn("Error processing PROXY Protocol: {}", cause.getMessage(), cause);
		ctx.close();
	}
}
