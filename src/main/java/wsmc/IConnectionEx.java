package wsmc;

import io.netty.handler.codec.http.HttpRequest;
import wsmc.api.IWebSocketServerConnection;
import wsmc.proxy.ProxyInfo;

public interface IConnectionEx extends IWebSocketServerConnection {
	public final static ArgHolder<IWebSocketServerAddress> connectToServerArg = ArgHolder.nullable();

	IWebSocketServerAddress getWsInfo();
	void setWsInfo(IWebSocketServerAddress wsInfo);

	void setWsHandshakeRequest(HttpRequest wsHandshakeRequest);

	void setProxyInfo(ProxyInfo proxyInfo);
}
