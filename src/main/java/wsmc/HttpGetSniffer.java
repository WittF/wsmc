package wsmc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;

import wsmc.exception.WsmcProtocolException;

/**
 * Protocol detector that sniffs the first few bytes of incoming connections
 * to distinguish between WebSocket (HTTP GET) and vanilla TCP Minecraft protocol.
 * <p>
 * This handler is inserted at the beginning of the server's Netty pipeline.
 * After detection, it either:
 * <ul>
 *   <li>Replaces itself with HTTP handlers for WebSocket connections</li>
 *   <li>Removes itself to allow vanilla TCP passthrough</li>
 *   <li>Throws an exception if vanilla TCP is disabled</li>
 * </ul>
 */
public class HttpGetSniffer extends ByteToMessageDecoder {
	private static final boolean disableVanillaTCP = ConfigHelper.isVanillaTcpDisabled();

	private final Consumer<HttpRequest> onWsmcHandshake;

	public HttpGetSniffer(Consumer<HttpRequest> onWsmcHandshake) {
		this.onWsmcHandshake = onWsmcHandshake;
	}

	/**
	 * Decode incoming bytes to detect protocol type.
	 * Inspects the first {@link WsmcConstants#HTTP_METHOD_DETECTION_BYTES} bytes
	 * to determine if this is an HTTP request (GET method for WebSocket handshake)
	 * or vanilla TCP Minecraft protocol.
	 *
	 * @param ctx the channel handler context
	 * @param in the incoming byte buffer
	 * @param out the output list (not used by this decoder)
	 * @throws WsmcProtocolException if vanilla TCP is attempted when disabled
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() > WsmcConstants.HTTP_METHOD_DETECTION_BYTES) {
			byte[] byteBuffer = new byte[WsmcConstants.HTTP_METHOD_DETECTION_BYTES];
			in.markReaderIndex();
			in.readBytes(byteBuffer, 0, WsmcConstants.HTTP_METHOD_DETECTION_BYTES);
			in.resetReaderIndex();
			String methodString = new String(byteBuffer, StandardCharsets.US_ASCII);

			if (methodString.equalsIgnoreCase("GET")) {
				WSMC.debug("Websocket Minecraft");
				ctx.pipeline().replace(this, "WsmcHttpCodec", new HttpServerCodec());
				ctx.pipeline().addAfter("WsmcHttpCodec", "WsmcHttpHandler", new HttpServerHandler(this.onWsmcHandshake));
			} else {
				if (disableVanillaTCP) {
					String remoteAddr = ctx.channel().remoteAddress().toString();
					WSMC.warn("Rejected vanilla TCP connection from {} (vanilla TCP is disabled)", remoteAddr);
					throw new WsmcProtocolException("Vanilla TCP connections are disabled. " +
							"Please connect using WebSocket protocol (ws:// or wss://)");
				}

				WSMC.debug("TCP Minecraft");
				ctx.pipeline().remove(this);
			}
		}
	}
}
