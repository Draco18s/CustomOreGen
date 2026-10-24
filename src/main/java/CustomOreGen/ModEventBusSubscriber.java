package CustomOreGen;

import CustomOreGen.Server.LevelGenFeature;
import CustomOreGen.Server.VanillaOreRemovalModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Mod-bus events (NeoForge routes them by event type, so no bus is named here). */
@EventBusSubscriber(modid = CustomOreGenBase.MODID)
public class ModEventBusSubscriber {
    @SubscribeEvent
	public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
    	CustomPacketPayloadHandler.register(event);
    }

    /**
     * customoregen:cog_feature is the feature type behind data/customoregen/worldgen/feature/cog_feature.json,
     * and customoregen:remove_vanilla_ores is the biome modifier type that implements vanillaOreGen.
     */
    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
    	event.register(Registries.FEATURE_TYPE, helper ->
    			helper.register(Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "cog_feature"), LevelGenFeature.CODEC));
    	event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, helper ->
    			helper.register(Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "remove_vanilla_ores"), VanillaOreRemovalModifier.CODEC));
    }

    @SubscribeEvent
	public static void onFMLPostInit(FMLLoadCompleteEvent event) {
    	CustomOreGenBase.onModPostLoad();
    }
}
