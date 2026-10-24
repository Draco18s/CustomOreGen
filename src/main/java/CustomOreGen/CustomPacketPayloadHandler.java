package CustomOreGen;

import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Server.ServerState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Server half of COG's networking. Everything travels as one bidirectional payload type
 * (customoregen:payload); the client's handler is registered by ClientPacketHandler, so dedicated
 * servers never load client classes. NeoForge runs both handlers on the main thread.
 */
public class CustomPacketPayloadHandler {
	// 2: payloads are encoded field by field instead of with Java serialization.
	private static final String PROTOCOL_VERSION = "2";

	public static void register(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
		registrar.playBidirectional(CustomPacketPayload.TYPE, CustomPacketPayload.STREAM_CODEC, CustomPacketPayloadHandler::handleServer);
	}

	private static void handleServer(CustomPacketPayload msg, IPayloadContext context)
    {
		if (!(context.player() instanceof ServerPlayer player) || msg == null)
		{
			return;
		}
		MinecraftServer server = player.level().getServer();
        ServerState.checkIfServerChanged(server, server.getWorldData());

        switch (msg.type)
        {
            case DebuggingGeometryRequest:
            	// Wireframes reveal where ore is, so only operators (or the single-player host) get them.
            	if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !server.isSingleplayerOwner(player.nameAndId()))
            	{
            		return;
            	}
                GeometryRequestData request = (GeometryRequestData)msg.data;
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, request.dimensionID));
                if (level == null)
                {
                	return;
                }
                request.world = level;
                GeometryData geometryData = ServerState.getDebuggingGeometryData(request);

                // Null means debugging mode is off for that dimension; the client is told to start over
                // (DebuggingGeometryReset) when it gets turned on.
                if (geometryData != null)
                {
                    sendTo(new CustomPacketPayload(PayloadType.DebuggingGeometryData, geometryData), player);
                }
                break;

            default:
                CustomOreGenBase.log.warn("Ignoring unexpected packet type " + msg.type + " from " + player.getName().getString());
        }
    }

	/**
	 * Send a packet to a specific player.<br>
	 * Must be called Server side.
	 */
	public static void sendTo(CustomPacketPayload msg, ServerPlayer player)
	{
		if (!(player instanceof FakePlayer))
		{
			PacketDistributor.sendToPlayer(player, msg);
		}
	}

	public static void sendToAllClients(CustomPacketPayload msg) {
		PacketDistributor.sendToAllPlayers(msg);
	}
}
