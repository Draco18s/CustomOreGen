package CustomOreGen.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.GeometryStream.GeometryStreamException;
import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;
import CustomOreGen.Util.Transform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Client side of the debugging wireframes. Chunk geometry arrives from the server, is built into a
 * WireframeMesh per chunk, and is drawn every frame: the meshes to draw are picked while the level's
 * render state is extracted, then submitted with the rest of the frame's custom geometry.
 */
public class ClientState
{
    public static WireframeRenderMode dgRenderingMode = WireframeRenderMode.WIREFRAMEOVERLAY;
    public static boolean dgEnabled = true;
    private static Level _world = null;
    private static int _dgScanCounter = 0;
    private static int _dgBatchID = 0;
    // Built geometry per chunk. Only touched on the render thread.
    private static Map<Long,WireframeMesh> _dgChunkGeometry = new HashMap<Long, WireframeMesh>();
    private static Set<Long> _chunkDGRequests = new HashSet<Long>();
    // Filled from the network thread's enqueued work, drained on the render thread.
    private static Queue<GeometryData> geometryDataQueue = new ConcurrentLinkedQueue<GeometryData>();
	private static volatile boolean clearDisplayLists;

	// What this frame draws, picked while the render state is extracted.
	private record FrameWireframes(WireframeRenderMode mode, List<WireframeMesh> meshes) {}
	private static final ContextKey<FrameWireframes> FRAME_KEY = new ContextKey<>(Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "wireframes"));

    public enum WireframeRenderMode
    {
        NONE,
        WIREFRAME,
        POLYGON,
        WIREFRAMEOVERLAY;
    }

    public static void onExtractRenderState(ExtractLevelRenderStateEvent event)
    {
    	if (clearDisplayLists) {
    		clearDebuggingDisplayLists();
    		clearDisplayLists = false;
    	}

        if (_world != null && dgEnabled && dgRenderingMode != null && dgRenderingMode != WireframeRenderMode.NONE)
        {
        	buildGeometryDisplayLists();
        	Vec3 camera = event.getCamera().position();

            if (_dgScanCounter == 0)
            {
                _dgScanCounter = 60;
                requestNearbyGeometry(camera);
            }
            else
            {
                --_dgScanCounter;
            }

            if (!_dgChunkGeometry.isEmpty())
            {
            	event.getRenderState().setRenderData(FRAME_KEY, new FrameWireframes(dgRenderingMode, new ArrayList<WireframeMesh>(_dgChunkGeometry.values())));
            }
        }
    }

    public static void onSubmitGeometry(SubmitCustomGeometryEvent event)
    {
    	FrameWireframes frame = event.getLevelRenderState().getRenderData(FRAME_KEY);
    	if (frame == null)
    	{
    		return;
    	}

    	Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
    	PoseStack poseStack = event.getPoseStack();
    	SubmitNodeCollector collector = event.getSubmitNodeCollector();
    	float lineWidth = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
    	// Without glPolygonMode, the outline modes draw each triangle's edges as lines.
    	boolean outlinePolygons = frame.mode() != WireframeRenderMode.POLYGON;

    	poseStack.pushPose();
    	poseStack.translate(-camera.x, -camera.y, -camera.z);

    	for (WireframeMesh mesh : frame.meshes())
    	{
    		if (mesh.lineVertexCount() > 0 || (outlinePolygons && mesh.triangleVertexCount() > 0))
    		{
    			collector.submitCustomGeometry(poseStack, WireframeRenderTypes.lines(frame.mode()), (pose, buffer) -> {
    				float[] linePos = mesh.linePositions.elements();
    				int[] lineColors = mesh.lineColors.elements();
    				for (int i = 0; i + 1 < mesh.lineVertexCount(); i += 2)
    				{
    					addLine(pose, buffer, linePos, lineColors, i, i + 1, lineWidth);
    				}
    				if (outlinePolygons)
    				{
    					float[] pos = mesh.trianglePositions.elements();
    					int[] colors = mesh.triangleColors.elements();
    					for (int i = 0; i + 2 < mesh.triangleVertexCount(); i += 3)
    					{
    						addLine(pose, buffer, pos, colors, i, i + 1, lineWidth);
    						addLine(pose, buffer, pos, colors, i + 1, i + 2, lineWidth);
    						addLine(pose, buffer, pos, colors, i + 2, i, lineWidth);
    					}
    				}
    			});
    		}

    		if (!outlinePolygons && mesh.triangleVertexCount() > 0)
    		{
    			collector.submitCustomGeometry(poseStack, WireframeRenderTypes.polygons(), (pose, buffer) -> {
    				float[] pos = mesh.trianglePositions.elements();
    				int[] colors = mesh.triangleColors.elements();
    				for (int i = 0; i + 2 < mesh.triangleVertexCount(); i += 3)
    				{
    					// The debug pipeline draws quads, so each triangle repeats its last corner.
    					addVertex(pose, buffer, pos, colors, i);
    					addVertex(pose, buffer, pos, colors, i + 1);
    					addVertex(pose, buffer, pos, colors, i + 2);
    					addVertex(pose, buffer, pos, colors, i + 2);
    				}
    			});
    		}
    	}

    	poseStack.popPose();
    }

    // The line shader widens each segment on screen along its normal, which is the segment's direction.
    private static void addLine(PoseStack.Pose pose, VertexConsumer buffer, float[] pos, int[] colors, int from, int to, float width)
    {
    	float dx = pos[to * 3] - pos[from * 3];
    	float dy = pos[to * 3 + 1] - pos[from * 3 + 1];
    	float dz = pos[to * 3 + 2] - pos[from * 3 + 2];
    	float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
    	if (length < 1.0E-6F)
    	{
    		return;
    	}
    	dx /= length; dy /= length; dz /= length;
    	buffer.addVertex(pose, pos[from * 3], pos[from * 3 + 1], pos[from * 3 + 2]).setColor(colors[from]).setNormal(pose, dx, dy, dz).setLineWidth(width);
    	buffer.addVertex(pose, pos[to * 3], pos[to * 3 + 1], pos[to * 3 + 2]).setColor(colors[to]).setNormal(pose, dx, dy, dz).setLineWidth(width);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer, float[] pos, int[] colors, int index)
    {
    	buffer.addVertex(pose, pos[index * 3], pos[index * 3 + 1], pos[index * 3 + 2]).setColor(colors[index]);
    }

    private static void requestNearbyGeometry(Vec3 camera)
    {
    	byte scanRange = 16;
    	int maxRequest = 50 - _chunkDGRequests.size() * 20;
    	int chunkX = Mth.floor(camera.x) >> 4;
    	int chunkZ = Mth.floor(camera.z) >> 4;

    	for (int r = 0; maxRequest > 0 && r <= scanRange; ++r)
    	{
    		int iX = chunkX - r;
    		int iZ = chunkZ - r;

    		for (int i = 0; maxRequest > 0 && i <= r * 8; ++i)
    		{
    			if (i < r * 2)
    			{
    				++iX;
    			}
    			else if (i < r * 4)
    			{
    				++iZ;
    			}
    			else if (i < r * 6)
    			{
    				--iX;
    			}
    			else if (i < r * 8)
    			{
    				--iZ;
    			}
    			else if (r != 0)
    			{
    				continue;
    			}

    			long key = (long)iX << 32 | (long)iZ & 4294967295L;

    			if (!_dgChunkGeometry.containsKey(key) && _chunkDGRequests.add(key))
    			{
    				GeometryRequestData request = new GeometryRequestData(_world, iX, iZ, _dgBatchID);
    				ClientPacketHandler.sendToServer(new CustomPacketPayload(PayloadType.DebuggingGeometryRequest, request));
    				--maxRequest;
    			}
    		}
    	}
    }

    private static void buildGeometryDisplayLists() {
    	while(!geometryDataQueue.isEmpty()) {
    		buildGeometryDisplayList(geometryDataQueue.remove());
    	}
    }

    private static void buildGeometryDisplayList(GeometryData geometryData) {
        GeometryRenderer renderer = new GeometryRenderer();

        try
        {
            for (GeometryStream stream : geometryData.geometry)
            {
                stream.executeStream(renderer);
            }
        }
        catch (GeometryStreamException var8)
        {
            throw new RuntimeException(var8);
        }

        // Faint outline of the chunk itself, so it's visible which chunks have been received.
        renderer.setPositionTransform((new Transform()).translate((float)(geometryData.chunkX * 16 + 8), -1.0F, (float)(geometryData.chunkZ * 16 + 8)).scale(7.5F, 1.0F, 7.5F));
        renderer.setColor(new float[] {0.0F, 1.0F, 0.0F, 0.15F});
        renderer.setVertexMode(PrimitiveType.LINE, new int[] {1});
        renderer.addVertex(new float[] { -1.0F, 0.0F, -1.0F});
        renderer.addVertex(new float[] { -1.0F, 0.0F, 1.0F});
        renderer.addVertex(new float[] {1.0F, 0.0F, 1.0F});
        renderer.addVertex(new float[] {1.0F, 0.0F, -1.0F});
        renderer.addVertexRef(4);

        WireframeMesh geometry = new WireframeMesh();
        renderer.emit(geometry);
        geometry.trim();
        long key = (long)geometryData.chunkX << 32 | (long)geometryData.chunkZ & 4294967295L;
        _dgChunkGeometry.put(key, geometry);

        _chunkDGRequests.remove(key);
	}

    public static boolean hasLevelChanged(Level currentLevel)
    {
        return _world != currentLevel;
    }

    public static void onLevelChanged(Level currentLevel)
    {
        _world = currentLevel;
        CustomOreGenBase.log.debug("Client world changed to " + (_world == null ? null : _world.dimension().identifier()));
        clearDebuggingGeometry();
    }

    public static void addDebuggingGeometry(GeometryData geometryData)
    {
        if (_world != null && geometryData.batchID == _dgBatchID && _world.dimension().identifier().equals(geometryData.dimensionID))
        {
        	geometryDataQueue.add(geometryData);
        }
    }

    public static void clearDebuggingGeometry()
    {
    	clearDisplayLists = true;
    }

    // Runs on the render thread (from onExtractRenderState), which owns the chunk geometry.
    private static void clearDebuggingDisplayLists()
    {
        _dgChunkGeometry.clear();
        _chunkDGRequests.clear();
        geometryDataQueue.clear();
        ++_dgBatchID;
        dgEnabled = true;
        _dgScanCounter = (RandomSource.create()).nextInt(40);
    }
}
