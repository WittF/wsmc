package wsmc.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import wsmc.commands.WsmcCommand;

final class FabricCommandRegistrar {
	private FabricCommandRegistrar() {
	}

	static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			WsmcCommand.register(dispatcher);
		});
	}
}
