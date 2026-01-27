package wsmc.forge;

import net.minecraftforge.fml.common.Mod;

import wsmc.WSMC;

@Mod(WSMC.MODID)
public class ForgeEntry {
	public static ForgeEntry instance;

	public ForgeEntry() {
		if (instance == null)
			instance = this;
		else
			throw new RuntimeException("Duplicated Class Instantiation: wsmc.forge.ForgeEntry");
	}

	@Mod.EventBusSubscriber(modid = WSMC.MODID, bus = Mod.EventBusSubscriber.Bus.GAME)
	public final static class ForgeEventBusHandler {
		@net.minecraftforge.eventbus.api.listener.SubscribeEvent
		public static void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
			wsmc.commands.WsmcCommand.register(event.getDispatcher());
		}
	}
}
