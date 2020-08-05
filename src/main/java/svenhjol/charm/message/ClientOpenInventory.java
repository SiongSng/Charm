package svenhjol.charm.message;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import svenhjol.meson.helper.PlayerHelper;
import svenhjol.meson.message.IMesonMessage;

import java.util.function.Supplier;

public class ClientOpenInventory implements IMesonMessage {
    public static void encode(ClientOpenInventory msg, PacketBuffer buf) {
    }

    public static ClientOpenInventory decode(PacketBuffer buf) {
        return new ClientOpenInventory();
    }

    public static class Handler {
        public static void handle(final ClientOpenInventory msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(PlayerHelper::openInventory);
            ctx.get().setPacketHandled(true);
        }
    }
}
