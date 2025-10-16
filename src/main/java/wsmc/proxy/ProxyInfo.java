package wsmc.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Stores parsed proxy information from various sources:
 * <ul>
 *   <li>HTTP headers (X-Forwarded-For, X-Real-IP, Forwarded, etc.)</li>
 *   <li>PROXY Protocol v1/v2 (HAProxy)</li>
 * </ul>
 *
 * <p>This class provides unified access to client information
 * regardless of the proxy protocol used.</p>
 */
public class ProxyInfo {
	@Nullable
	private final String clientIp;
	private final List<String> proxyChain;
	@Nullable
	private final GeoInfo geoInfo;
	private final ProxySource source;
	@Nullable
	private final ProxyMetadata metadata;

	private ProxyInfo(Builder builder) {
		this.clientIp = builder.clientIp;
		this.proxyChain = Collections.unmodifiableList(new ArrayList<>(builder.proxyChain));
		this.geoInfo = builder.geoInfo;
		this.source = builder.source;
		this.metadata = builder.metadata;
	}

	@Nullable
	public String getClientIp() {
		return clientIp;
	}

	public List<String> getProxyChain() {
		return proxyChain;
	}

	@Nullable
	public String getImmediateProxyIp() {
		return proxyChain.isEmpty() ? null : proxyChain.get(proxyChain.size() - 1);
	}

	@Nullable
	public GeoInfo getGeoInfo() {
		return geoInfo;
	}

	public ProxySource getSource() {
		return source;
	}

	@Nullable
	public ProxyMetadata getMetadata() {
		return metadata;
	}

	public boolean isBehindProxy() {
		return clientIp != null && !proxyChain.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ProxyInfo{");
		sb.append("clientIp='").append(clientIp).append('\'');
		sb.append(", proxyChain=").append(proxyChain);
		sb.append(", source=").append(source);
		if (geoInfo != null) {
			sb.append(", geoInfo=").append(geoInfo);
		}
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Geographic information about the client
	 */
	public static class GeoInfo {
		@Nullable
		private final String countryCode;
		@Nullable
		private final String regionCode;
		@Nullable
		private final String cityName;

		public GeoInfo(@Nullable String countryCode, @Nullable String regionCode, @Nullable String cityName) {
			this.countryCode = countryCode;
			this.regionCode = regionCode;
			this.cityName = cityName;
		}

		@Nullable
		public String getCountryCode() {
			return countryCode;
		}

		@Nullable
		public String getRegionCode() {
			return regionCode;
		}

		@Nullable
		public String getCityName() {
			return cityName;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("GeoInfo{");
			if (countryCode != null) sb.append("country='").append(countryCode).append('\'');
			if (regionCode != null) {
				if (countryCode != null) sb.append(", ");
				sb.append("region='").append(regionCode).append('\'');
			}
			if (cityName != null) {
				if (countryCode != null || regionCode != null) sb.append(", ");
				sb.append("city='").append(cityName).append('\'');
			}
			sb.append('}');
			return sb.toString();
		}
	}

	/**
	 * Additional metadata from proxy headers
	 */
	public static class ProxyMetadata {
		@Nullable
		private final String protocol;
		@Nullable
		private final String host;
		@Nullable
		private final Integer port;

		public ProxyMetadata(@Nullable String protocol, @Nullable String host, @Nullable Integer port) {
			this.protocol = protocol;
			this.host = host;
			this.port = port;
		}

		@Nullable
		public String getProtocol() {
			return protocol;
		}

		@Nullable
		public String getHost() {
			return host;
		}

		@Nullable
		public Integer getPort() {
			return port;
		}

		@Override
		public String toString() {
			return "ProxyMetadata{protocol='" + protocol + "', host='" + host + "', port=" + port + '}';
		}
	}

	/**
	 * Source of proxy information
	 */
	public enum ProxySource {
		NONE,
		HTTP_HEADERS,
		PROXY_PROTOCOL_V1,
		PROXY_PROTOCOL_V2
	}

	public static class Builder {
		private String clientIp;
		private List<String> proxyChain = new ArrayList<>();
		private GeoInfo geoInfo;
		private ProxySource source = ProxySource.NONE;
		private ProxyMetadata metadata;

		public Builder clientIp(String clientIp) {
			this.clientIp = clientIp;
			return this;
		}

		public Builder addProxyIp(String ip) {
			this.proxyChain.add(ip);
			return this;
		}

		public Builder proxyChain(List<String> chain) {
			this.proxyChain = new ArrayList<>(chain);
			return this;
		}

		public Builder geoInfo(GeoInfo geoInfo) {
			this.geoInfo = geoInfo;
			return this;
		}

		public Builder geoInfo(String countryCode, String regionCode, String cityName) {
			this.geoInfo = new GeoInfo(countryCode, regionCode, cityName);
			return this;
		}

		public Builder source(ProxySource source) {
			this.source = source;
			return this;
		}

		public Builder metadata(ProxyMetadata metadata) {
			this.metadata = metadata;
			return this;
		}

		public Builder metadata(String protocol, String host, Integer port) {
			this.metadata = new ProxyMetadata(protocol, host, port);
			return this;
		}

		/**
		 * Check if the proxy chain is empty
		 * @return true if no proxy IPs have been added
		 */
		public boolean isProxyChainEmpty() {
			return this.proxyChain.isEmpty();
		}

		public ProxyInfo build() {
			return new ProxyInfo(this);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ProxyInfo none() {
		return new Builder().source(ProxySource.NONE).build();
	}
}
