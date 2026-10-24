package CustomOreGen.Client;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.GeometryData;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client half of CustomPacketPayloadHandler; kept separate so dedicated servers never load client classes. */
@EventBusSubscriber(modid = CustomOreGenBase.MODID, value = Dist.CLIENT)
public class ClientPacketHandler
{
	@SubscribeEvent
	public static void onRegisterPayloadHandlers(RegisterClientPayloadHandlersEvent event)
	{
		event.register(CustomPacketPayload.TYPE, ClientPacketHandler::handle);
	}

	/** Sends a packet to the server. */
	public static void sendToServer(CustomPacketPayload msg)
	{
		ClientPacketDistributor.sendToServer(msg);
	}

	private static void handle(CustomPacketPayload msg, IPayloadContext context)
	{
		Minecraft mc = Minecraft.getInstance();

		if (mc.level != null && ClientState.hasLevelChanged(mc.level))
		{
			ClientState.onLevelChanged(mc.level);
		}

		if (msg == null)
		{
			return;
		}

		switch (msg.type)
		{
			case DebuggingGeometryData:
				ClientState.addDebuggingGeometry((GeometryData)msg.data);
				break;

			case DebuggingGeometryRenderMode:
				setRenderMode((String)msg.data);
				break;

			case DebuggingGeometryReset:
				ClientState.clearDebuggingGeometry();
				break;

			case CommandResponse:
				if (mc.player != null && msg.data != null)
				{
					mc.player.sendSystemMessage(Component.literal((String)msg.data));
				}
				break;

			default:
				throw new RuntimeException("Unhandled client packet type " + msg.type);
		}
	}

	/** Sets the named mode, or cycles to the next one when no name is given. */
	private static void setRenderMode(String name)
	{
		Minecraft mc = Minecraft.getInstance();

		if (name == null || name.isEmpty())
		{
			int mode = ClientState.dgRenderingMode == null ? 0 : ClientState.dgRenderingMode.ordinal();
			ClientState.dgRenderingMode = WireframeRenderMode.values()[(mode + 1) % WireframeRenderMode.values().length];
		}
		else
		{
			WireframeRenderMode match = null;
			for (WireframeRenderMode mode : WireframeRenderMode.values())
			{
				if (mode.name().equalsIgnoreCase(name))
				{
					match = mode;
					break;
				}
			}

			if (match == null)
			{
				if (mc.player != null)
				{
					mc.player.sendSystemMessage(Component.literal("§cError: Invalid wireframe mode '" + name + "'"));
				}
				return;
			}
			ClientState.dgRenderingMode = match;
		}

		if (mc.player != null)
		{
			mc.player.sendSystemMessage(Component.literal("COG Client wireframe mode: " + ClientState.dgRenderingMode.name()));
		}
	}
}
