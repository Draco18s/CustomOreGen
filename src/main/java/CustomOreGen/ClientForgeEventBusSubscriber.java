package CustomOreGen;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.CreateWorldScreenHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Game-bus events that only exist on the client. @OnlyIn isn't allowed on subscriber methods, so these
 * live in a class that is only registered on the client.
 */
@EventBusSubscriber(modid = CustomOreGenBase.MODID, value = Dist.CLIENT)
public class ClientForgeEventBusSubscriber
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && ClientState.hasLevelChanged(mc.level))
        {
            ClientState.onLevelChanged(mc.level);
        }
    }

    @SubscribeEvent
    public static void onExtractRenderState(ExtractLevelRenderStateEvent event)
    {
        ClientState.onExtractRenderState(event);
    }

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event)
    {
        ClientState.onSubmitGeometry(event);
    }

    @SubscribeEvent
    public static void onLoadLevel(LevelEvent.Load event)
    {
        if (event.getLevel() instanceof ClientLevel cWorld && ClientState.hasLevelChanged(cWorld))
        {
            ClientState.onLevelChanged(cWorld);
        }
    }

    @SubscribeEvent
    public static void onInitGui(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            CreateWorldScreenHooks.addOptionsButtonToGui((CreateWorldScreen)event.getScreen(), event.getListenersList(), event::addListener);
        }
    }

    @SubscribeEvent
    public static void onActionPerformed(ScreenEvent.MouseButtonReleased.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            CreateWorldScreenHooks.updateOptionsButtonVisibility((CreateWorldScreen)event.getScreen());
        }
    }
}
