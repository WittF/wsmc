package wsmc;

/**
 * Helper class for reading and parsing WSMC configuration from system properties.
 */
public final class ConfigHelper {
	private ConfigHelper() {
		// Utility class, no instantiation
	}

	/**
	 * Get boolean property value.
	 * @param key the property key
	 * @param defaultValue the default value if property is not set
	 * @return the property value as boolean
	 */
	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = System.getProperty(key, String.valueOf(defaultValue));
		return value.equalsIgnoreCase("true");
	}

	/**
	 * Get string property value.
	 * @param key the property key
	 * @param defaultValue the default value if property is not set
	 * @return the property value as string, or null if not set and no default
	 */
	public static String getString(String key, String defaultValue) {
		return System.getProperty(key, defaultValue);
	}

	/**
	 * Parse the maxFramePayloadLength from system property.
	 * Falls back to default value if property is not set or invalid.
	 * @return the maximum frame payload length in bytes
	 */
	public static int getMaxFramePayloadLength() {
		String value = System.getProperty(
				WsmcConstants.PROP_MAX_FRAME_PAYLOAD_LENGTH,
				String.valueOf(WsmcConstants.DEFAULT_MAX_FRAME_PAYLOAD_LENGTH));

		try {
			int parsed = Integer.parseInt(value);
			if (parsed <= 0) {
				WSMC.warn("Invalid maxFramePayloadLength (must be > 0): {}, using default: {}",
						value, WsmcConstants.DEFAULT_MAX_FRAME_PAYLOAD_LENGTH);
				return WsmcConstants.DEFAULT_MAX_FRAME_PAYLOAD_LENGTH;
			}
			return parsed;
		} catch (NumberFormatException e) {
			WSMC.warn("Unable to parse maxFramePayloadLength, using default. Invalid value: {}", value);
			return WsmcConstants.DEFAULT_MAX_FRAME_PAYLOAD_LENGTH;
		}
	}

	/**
	 * Check if vanilla TCP connections are disabled.
	 * @return true if vanilla TCP is disabled
	 */
	public static boolean isVanillaTcpDisabled() {
		return getBoolean(WsmcConstants.PROP_DISABLE_VANILLA_TCP, false);
	}

	/**
	 * Get the configured WebSocket endpoint path.
	 * @return the endpoint path, or null if not configured (allowing any endpoint)
	 */
	public static String getWsmcEndpoint() {
		return getString(WsmcConstants.PROP_WSMC_ENDPOINT, null);
	}
}
