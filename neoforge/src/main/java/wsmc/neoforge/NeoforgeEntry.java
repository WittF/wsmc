package wsmc.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import wsmc.WSMC;
import wsmc.commands.WsmcCommand;

@Mod(WSMC.MODID)
public class NeoforgeEntry {
	public NeoforgeEntry() {
	}

	@EventBusSubscriber(modid = WSMC.MODID)
	public static class NeoforgeEventHandler {
		@SubscribeEvent
		public static void onRegisterCommands(RegisterCommandsEvent event) {
			WsmcCommand.register(event.getDispatcher());
		}
	}
}
