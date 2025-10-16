**简体中文** | [English](README.en.md)

# WSMC
为 Minecraft Java 版启用 WebSocket 支持。
由于大多数 CDN 服务商（至少是免费套餐）不支持原始 TCP 代理，借助此模组，服务器所有者现在可以将服务器隐藏在 CDN 后面，让玩家通过 WebSocket 连接，从而防止 DDoS 攻击。

支持 Minecraft Forge、Neoforge 和 Fabric：
* **1.20.6** - 1.21.4（Forge、Neoforge、Fabric）
* **1.20.2** - 1.20.4（所有 loader）
* **1.20.1**（所有 loader）
* **1.18.2** - 1.20（旧版本分支）

**注意**：Minecraft 1.20.5 和 1.21.2 没有 Forge 支持，仅支持 Neoforge 和 Fabric。

当前分支适用于 1.20.5 及以上版本。

此模组独立运行，没有任何依赖。

## 在服务端安装此模组后：
* 服务器将允许玩家通过 WebSocket 连接。
* 玩家仍然可以使用原版 TCP 连接。
* 服务器在同一监听端口上同时接受和处理 TCP 和 WebSocket 连接。
* 即使不在客户端安装此模组，玩家仍然可以使用原版 TCP 连接到已安装此模组的服务器。
* 服务器可以从 WebSocket 握手中获取客户端统计信息（例如真实 IP）。

## 在客户端安装此模组后：
* 客户端可以使用类似 `ws://hostname.com:port/path_to_minecraft_endpoint` 的 URI 加入启用了 WebSocket 的服务器。
* 客户端仍然可以使用旧语法（例如 `hostname_or_ip:port`）通过原版 TCP 连接任何服务器。

## 注意事项
* 此模组不影响任何游戏玩法。
* 此模组不修改任何 GUI。
* 即使你安装了此模组，原版客户端仍然可以加入你的服务器（注意你安装的其他模组可能会阻止原版客户端加入）。
* 在客户端安装此模组不会阻止你加入其他原版或模组服务器。
* 服务器仍然可以获取通过 CDN 代理的 WebSocket 连接的玩家的真实 IP。
* 此模组与其他 TCP-WebSocket 代理兼容，例如 websocat。

## 客户端选项
有时 DNS 会为 HTTP 主机名（ws）或 SNI（wss）返回较慢的 IP。客户端可能希望控制如何解析 IP 地址。

客户端可以选择性地控制 WebSocket 握手期间使用的 HTTP 主机名和 SNI：
```
指定 http 主机名的非安全 WebSocket 连接：
ws://host.com@ip.ip.ip.ip

将 sni 和 http 主机名指定为相同值（sni-host.com），从 ip.ip.ip.ip 解析服务器 IP：
wss://sni-host.com@ip.ip.ip.ip

sni 和 http 主机名设置为不同值，从 host.com 解析服务器 IP：
wss://sni.com:@host.com[:port]

sni 和 http 主机名设置为不同值，从 sni.com 解析服务器 IP：
wss://:host.com@sni.com[:port]

分别设置 sni、http 主机名和服务器地址：
wss://sni.com:host.com@ip.ip.ip.ip
```

端口和路径规范可以同时附加。

## 配置
此模组的配置通过"系统属性"传递。你可以在 JVM 命令行中使用 `-D` 来传递这些选项。

| 属性键                       | 类型      | 用途                                                                                                            | 作用端         | 默认值    | 示例      |
|------------------------------|----------|----------------------------------------------------------------------------------------------------------------|---------------|---------|----------|
| wsmc.disableVanillaTCP       | boolean  | 禁用原版 TCP 登录和服务器状态查询。                                                                               | Server        | false   | true     |
| wsmc.wsmcEndpoint            | string   | 设置用于 Minecraft 登录和服务器状态的 WebSocket 端点。如果此属性不存在，客户端可以通过任何 WebSocket 端点加入游戏。必须以 / 开头，区分大小写。 | Server        | 未设置   | /mc      |
| wsmc.debug                   | boolean  | 显示调试日志。                                                                                                   | Server Client | false   | true     |
| wsmc.dumpBytes               | boolean  | 转储原始 WebSocket 二进制帧。仅在 `wsmc.debug` 设置为 `true` 时有效。                                              | Server Client | false   | true     |
| wsmc.maxFramePayloadLength   | integer  | 最大允许的帧负载长度。将此值设置为你的模组包的要求，否则 Netty 将抛出错误"已超过最大帧长度 x"。                           | Server Client | 65536   | 65536    |

## 依赖项
### Forge 版本
* `netty-codec-http` 用于处理 HTTP 和 WebSocket

### Fabric 版本
你需要安装 Fabric Loader，然后安装此模组。Fabric API 是可选的，但强烈推荐。

## 开发者指南
要修改和调试代码，首先在 Eclipse IDE 中将"forge"或"fabric"文件夹导入为 Gradle 项目，然后运行 gradle 任务 `genEclipseRuns`。

Windows 用户需要将 `./` 和 `../` 分别替换为 `.\` 和 `..\`。

代码库使用 Minecraft 官方映射表。

在服务端，如果客户端通过 WebSocket 加入，可以访问其 WebSocket 握手请求以获取真实 IP 等信息：

```java
import wsmc.api.IWebSocketServerConnection;
import net.minecraft.network.Connection;
import io.netty.handler.codec.http.HttpRequest;

Connection conn = // ... 从服务器获取连接
HttpRequest handshake = IWebSocketServerConnection.of(conn).getWsHandshakeRequest();
if (handshake != null) {
    String realIP = handshake.headers().get("X-Forwarded-For");
    String country = handshake.headers().get("CF-IPCountry");
}
```

如果 Minecraft 服务器位于反向代理（例如 CDN）后面，这对于获取客户端的真实 IP 和地理位置非常有用。

### 编译项目
```bash
git clone https://github.com/WittF/wsmc.git
cd wsmc

# 编译 Fabric 版本
cd fabric && ./gradlew build

# 编译 Forge 版本
cd forge && ./gradlew build

# 编译 Neoforge 版本
cd neoforge && ./gradlew build
```

### 指定 JRE 路径（自 1.18.1 起，Minecraft 需要 Java 17）：
```
./gradlew -Dorg.gradle.java.home=/path_to_jdk_directory <commands>
```
* 自 1.18.1 起，Minecraft 需要 Java 17
* 自 1.20.5 起，Minecraft 需要 Java 21
