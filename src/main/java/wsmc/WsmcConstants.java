package wsmc;

/**
 * Constants used throughout WSMC.
 */
public final class WsmcConstants {
	private WsmcConstants() {
		// Utility class, no instantiation
	}

	// Network Protocol Constants
	/**
	 * Number of bytes to inspect for HTTP method detection.
	 */
	public static final int HTTP_METHOD_DETECTION_BYTES = 3;

	/**
	 * Default maximum WebSocket frame payload length in bytes.
	 * Can be overridden via wsmc.maxFramePayloadLength system property.
	 */
	public static final int DEFAULT_MAX_FRAME_PAYLOAD_LENGTH = 65536;

	/**
	 * Maximum bytes per line when dumping byte arrays for debugging.
	 */
	public static final int DUMP_BYTES_PER_LINE = 32;

	/**
	 * HTTP object aggregator maximum content length (32KB).
	 */
	public static final int HTTP_AGGREGATOR_MAX_CONTENT_LENGTH = 8192 * 4;

	// Default Ports
	/**
	 * Default port for WebSocket (ws://) connections.
	 */
	public static final int DEFAULT_WS_PORT = 80;

	/**
	 * Default port for WebSocket Secure (wss://) connections.
	 */
	public static final int DEFAULT_WSS_PORT = 443;

	/**
	 * Default Minecraft server port.
	 */
	public static final int DEFAULT_MINECRAFT_PORT = 25565;

	// System Properties
	/**
	 * System property key for disabling vanilla TCP connections.
	 */
	public static final String PROP_DISABLE_VANILLA_TCP = "wsmc.disableVanillaTCP";

	/**
	 * System property key for setting the WebSocket endpoint path.
	 */
	public static final String PROP_WSMC_ENDPOINT = "wsmc.wsmcEndpoint";

	/**
	 * System property key for enabling debug logging.
	 */
	public static final String PROP_DEBUG = "wsmc.debug";

	/**
	 * System property key for enabling byte dump logging.
	 */
	public static final String PROP_DUMP_BYTES = "wsmc.dumpBytes";

	/**
	 * System property key for maximum frame payload length.
	 */
	public static final String PROP_MAX_FRAME_PAYLOAD_LENGTH = "wsmc.maxFramePayloadLength";
}
