package wsmc.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor to access the protected connection field from ServerCommonPacketListenerImpl.
 * <p>
 * This is necessary because although all mod loaders use official Mojang mappings,
 * Fabric's compile-time environment doesn't expose the getConnection() method,
 * requiring us to use Mixin accessors to access the protected field in a
 * platform-independent way.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public interface ServerCommonPacketListenerAccessor {
    /**
     * Accessor for the protected 'connection' field in ServerCommonPacketListenerImpl.
     *
     * @return The network connection this packet listener is attached to
     */
    @Accessor("connection")
    Connection getConnection();
}
