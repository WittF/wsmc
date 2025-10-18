package wsmc.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import wsmc.WSMC;

/**
 * Fabric entry point for WSMC mod.
 * <p>
 * Registers commands when Fabric API is present, otherwise logs a warning and
 * allows the mod to continue running with command features disabled.
 */
public class FabricEntry implements ModInitializer {
	@Override
	public void onInitialize() {
		if (hasCommandApi()) {
			FabricCommandRegistrar.registerCommands();
		} else {
			WSMC.warn("Fabric Command API v2 未检测到，跳过 /wsmc 指令注册；安装 fabric-api 可启用该指令。");
		}
	}

	private boolean hasCommandApi() {
		// fabric-command-api-v2 由 fabric-api 提供；兼容独立或裁剪安装的场景
		return FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")
			|| FabricLoader.getInstance().isModLoaded("fabric-api");
	}
}
