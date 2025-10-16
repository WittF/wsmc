package wsmc.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import io.netty.handler.codec.http.HttpHeaders;
import wsmc.WSMC;

/**
 * Parser for HTTP proxy headers including X-Forwarded-For, X-Real-IP,
 * Forwarded (RFC 7239), and CDN-specific headers.
 *
 * <p>Parsing priority (highest to lowest):
 * <ol>
 *   <li>X-Real-IP</li>
 *   <li>CF-Connecting-IP (Cloudflare)</li>
 *   <li>True-Client-IP (Akamai/Cloudflare Enterprise)</li>
 *   <li>X-Forwarded-For (first IP in chain)</li>
 *   <li>Forwarded (RFC 7239)</li>
 * </ol>
 */
public class ProxyHeaderParser {
	// RFC 7239 Forwarded header pattern
	private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\\\"?([^\\s,;\\\"]+)");
	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=([^\\s,;]+)");
	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\\\"?([^\\s,;\\\"]+)");

	/**
	 * Parse proxy information from HTTP headers
	 *
	 * @param headers HTTP headers from WebSocket handshake
	 * @return ProxyInfo containing parsed proxy data, or ProxyInfo.none() if no proxy headers found
	 */
	public static ProxyInfo parse(HttpHeaders headers) {
		ProxyInfo.Builder builder = ProxyInfo.builder();
		builder.source(ProxyInfo.ProxySource.HTTP_HEADERS);

		// Try X-Real-IP first (highest priority)
		String clientIp = headers.get("X-Real-IP");
		if (clientIp != null && !clientIp.trim().isEmpty()) {
			clientIp = cleanIp(clientIp);
			builder.clientIp(clientIp);
			builder.addProxyIp(clientIp);
			WSMC.debug("Proxy: X-Real-IP = {}", clientIp);
		}

		// Try CF-Connecting-IP (Cloudflare)
		if (clientIp == null) {
			clientIp = headers.get("CF-Connecting-IP");
			if (clientIp != null && !clientIp.trim().isEmpty()) {
				clientIp = cleanIp(clientIp);
				builder.clientIp(clientIp);
				builder.addProxyIp(clientIp);
				WSMC.debug("Proxy: CF-Connecting-IP = {}", clientIp);
			}
		}

		// Try True-Client-IP (Akamai/Cloudflare Enterprise)
		if (clientIp == null) {
			clientIp = headers.get("True-Client-IP");
			if (clientIp != null && !clientIp.trim().isEmpty()) {
				clientIp = cleanIp(clientIp);
				builder.clientIp(clientIp);
				builder.addProxyIp(clientIp);
				WSMC.debug("Proxy: True-Client-IP = {}", clientIp);
			}
		}

		// Parse X-Forwarded-For (comma-separated chain)
		if (clientIp == null) {
			String xForwardedFor = headers.get("X-Forwarded-For");
			if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
				List<String> chain = parseXForwardedFor(xForwardedFor);
				if (!chain.isEmpty()) {
					builder.clientIp(chain.get(0)); // First IP is the client
					builder.proxyChain(chain);
					WSMC.debug("Proxy: X-Forwarded-For chain = {}", chain);
				}
			}
		}

		// Parse RFC 7239 Forwarded header
		if (clientIp == null) {
			String forwarded = headers.get("Forwarded");
			if (forwarded != null && !forwarded.trim().isEmpty()) {
				String forValue = parseForwarded(forwarded, FORWARDED_FOR_PATTERN);
				if (forValue != null) {
					builder.clientIp(forValue);
					builder.addProxyIp(forValue);
					WSMC.debug("Proxy: Forwarded for = {}", forValue);
				}
			}
		}

		// Parse protocol from X-Forwarded-Proto or Forwarded header
		String proto = headers.get("X-Forwarded-Proto");
		if (proto == null) {
			String forwarded = headers.get("Forwarded");
			if (forwarded != null) {
				proto = parseForwarded(forwarded, FORWARDED_PROTO_PATTERN);
			}
		}

		// Parse original host from X-Forwarded-Host or Forwarded header
		String host = headers.get("X-Forwarded-Host");
		if (host == null) {
			String forwarded = headers.get("Forwarded");
			if (forwarded != null) {
				host = parseForwarded(forwarded, FORWARDED_HOST_PATTERN);
			}
		}

		// Parse port from X-Forwarded-Port
		Integer port = null;
		String portStr = headers.get("X-Forwarded-Port");
		if (portStr != null) {
			try {
				port = Integer.parseInt(portStr.trim());
			} catch (NumberFormatException e) {
				WSMC.warn("Invalid X-Forwarded-Port: {}", portStr);
			}
		}

		if (proto != null || host != null || port != null) {
			builder.metadata(proto, host, port);
		}

		// Parse geo information from CDN headers
		String country = headers.get("CF-IPCountry");
		String region = headers.get("CF-Region");
		String city = headers.get("CF-City");
		if (country != null || region != null || city != null) {
			builder.geoInfo(country, region, city);
		}

		ProxyInfo info = builder.build();

		// Return NONE if no proxy information was found
		if (!info.isBehindProxy()) {
			return ProxyInfo.none();
		}

		return info;
	}

	/**
	 * Parse X-Forwarded-For header into IP chain
	 *
	 * @param xForwardedFor X-Forwarded-For header value
	 * @return List of IP addresses from client to last proxy
	 */
	private static List<String> parseXForwardedFor(String xForwardedFor) {
		List<String> chain = new ArrayList<>();
		String[] ips = xForwardedFor.split(",");
		for (String ip : ips) {
			String cleaned = cleanIp(ip);
			if (!cleaned.isEmpty()) {
				chain.add(cleaned);
			}
		}
		return chain;
	}

	/**
	 * Parse RFC 7239 Forwarded header
	 *
	 * @param forwarded Forwarded header value
	 * @param pattern Pattern to extract specific value
	 * @return Extracted value or null
	 */
	@Nullable
	private static String parseForwarded(String forwarded, Pattern pattern) {
		Matcher matcher = pattern.matcher(forwarded);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * Clean and validate IP address string
	 *
	 * @param ip Raw IP address string
	 * @return Cleaned IP address
	 */
	private static String cleanIp(String ip) {
		if (ip == null || ip.isEmpty()) {
			return "";
		}

		// Trim whitespace
		ip = ip.trim();
		if (ip.isEmpty()) {
			return "";
		}

		// Remove square brackets from IPv6 first (e.g., "[::1]" or "[::1]:8080")
		if (ip.startsWith("[")) {
			int closeBracket = ip.indexOf(']');
			if (closeBracket > 0) {
				// Extract IPv6 address from brackets
				String extracted = ip.substring(1, closeBracket);
				// Validate IPv6 format: must contain at least one colon
				if (extracted.contains(":")) {
					// Note: We ignore any port after the bracket (e.g., [::1]:8080)
					return extracted;
				} else {
					// Format error: brackets without valid IPv6 content
					WSMC.warn("Invalid IPv6 format with brackets (missing colons): {}", ip);
					return "";
				}
			} else {
				// Format error: opening bracket without closing bracket
				WSMC.warn("Malformed IPv6 address (unclosed bracket): {}", ip);
				return "";
			}
		}

		// Remove port if present (e.g., "192.168.1.1:8080")
		// Only do this for IPv4 addresses (single colon)
		int colonIndex = ip.lastIndexOf(':');
		if (colonIndex > 0 && colonIndex < ip.length() - 1) {
			// Check if it's IPv4 with port (only one colon)
			if (ip.indexOf(':') == colonIndex) {
				// Single colon - likely IPv4:port
				try {
					Integer.parseInt(ip.substring(colonIndex + 1));
					ip = ip.substring(0, colonIndex);
				} catch (NumberFormatException e) {
					// Not a valid port number, keep as is
				}
			}
			// If multiple colons, assume it's IPv6 and keep as is
		}

		return ip;
	}
}
