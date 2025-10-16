package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import net.minecraft.network.Connection;

import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;

import wsmc.ConfigHelper;
import wsmc.HttpGetSniffer;
import wsmc.IConnectionEx;
import wsmc.WSMC;
import wsmc.proxy.ProxyHeaderParser;
import wsmc.proxy.ProxyInfo;
import wsmc.proxy.ProxyProtocolHandler;

@Debug(export = true)
@Mixin(targets="net.minecraft.server.network.ServerConnectionListener$1")
public class MixinServerConnectionListener {
	@Inject(at = @At("RETURN"), method = "initChannel", require = 1)
	private void initChannel(CallbackInfo callback,
			@Local(ordinal = 0, argsOnly = true) Channel channel,
			@Local(ordinal = 0, argsOnly = false) Connection connection) {
		ChannelPipeline pipeline = channel.pipeline();
		IConnectionEx connectionEx = (IConnectionEx) connection;

		// Add PROXY Protocol support if enabled
		if (ConfigHelper.isProxyProtocolEnabled()) {
			WSMC.debug("PROXY Protocol support enabled");
			pipeline.addAfter("timeout", "WsmcProxyProtocolDecoder", new HAProxyMessageDecoder());
			pipeline.addAfter("WsmcProxyProtocolDecoder", "WsmcProxyProtocolHandler", new ProxyProtocolHandler(connection));
		}

		// Server side
		// Create callback that stores handshake request and parses proxy information
		HttpGetSniffer sniffer = new HttpGetSniffer(httpRequest -> {
			// Store the WebSocket handshake request
			connectionEx.setWsHandshakeRequest(httpRequest);

			// Parse and store proxy information from HTTP headers
			ProxyInfo proxyInfo = ProxyHeaderParser.parse(httpRequest.headers());

			// Only set if not already set by PROXY Protocol handler
			if (connectionEx.getProxyInfo() == null) {
				connectionEx.setProxyInfo(proxyInfo);
			}
		});

		// Determine the best position to inject HttpGetSniffer
		String preferredHandler = ConfigHelper.isProxyProtocolEnabled() ? "WsmcProxyProtocolHandler" : "timeout";

		if (pipeline.get(preferredHandler) != null) {
			// Preferred handler exists, add after it
			pipeline.addAfter(preferredHandler, "WsmcHttpGetSniffer", sniffer);
		} else if (pipeline.get("timeout") != null) {
			// Fallback to timeout handler if preferred handler doesn't exist
			WSMC.debug("Handler '{}' not found in pipeline, falling back to 'timeout'", preferredHandler);
			pipeline.addAfter("timeout", "WsmcHttpGetSniffer", sniffer);
		} else {
			// Last resort: add at the beginning of the pipeline
			WSMC.warn("Neither '{}' nor 'timeout' found in pipeline, adding HttpGetSniffer at first position", preferredHandler);
			pipeline.addFirst("WsmcHttpGetSniffer", sniffer);
		}
	}
}
