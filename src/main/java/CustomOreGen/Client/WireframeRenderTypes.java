package CustomOreGen.Client;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * How debugging wireframes are drawn. WIREFRAME and POLYGON use vanilla's translucent lines (and, for
 * POLYGON's filled faces, debug quads), which are hidden behind blocks; WIREFRAMEOVERLAY uses a copy of
 * the line pipeline with the depth test turned off, so the wireframes show through the terrain.
 */
@EventBusSubscriber(modid = CustomOreGenBase.MODID, value = Dist.CLIENT)
public final class WireframeRenderTypes
{
	private WireframeRenderTypes() {}

	private static final RenderPipeline LINES_OVERLAY_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "pipeline/wireframe_lines_overlay"))
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.build();

	private static final RenderType LINES_OVERLAY = RenderType.create(CustomOreGenBase.MODID + "_wireframe_lines_overlay",
			RenderSetup.builder(LINES_OVERLAY_PIPELINE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());

	@SubscribeEvent
	public static void onRegisterPipelines(RegisterRenderPipelinesEvent event)
	{
		event.registerPipeline(LINES_OVERLAY_PIPELINE);
	}

	/** Line segments: format POSITION_COLOR_NORMAL_LINE_WIDTH. */
	static RenderType lines(WireframeRenderMode mode)
	{
		return mode == WireframeRenderMode.WIREFRAMEOVERLAY ? LINES_OVERLAY : RenderTypes.linesTranslucent();
	}

	/** Filled polygons (POLYGON mode), as quads: format POSITION_COLOR. */
	static RenderType polygons()
	{
		return RenderTypes.debugQuads();
	}
}
