package CustomOreGen.Client;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * One chunk's debugging wireframe, built once when its geometry arrives and drawn every frame: line
 * segments (two vertices each) and triangles (three each), as world positions with ARGB colors.
 */
public class WireframeMesh
{
	final FloatArrayList linePositions = new FloatArrayList();
	final IntArrayList lineColors = new IntArrayList();
	final FloatArrayList trianglePositions = new FloatArrayList();
	final IntArrayList triangleColors = new IntArrayList();

	public void addLineVertex(float x, float y, float z, int argb)
	{
		this.linePositions.add(x);
		this.linePositions.add(y);
		this.linePositions.add(z);
		this.lineColors.add(argb);
	}

	public void addTriangleVertex(float x, float y, float z, int argb)
	{
		this.trianglePositions.add(x);
		this.trianglePositions.add(y);
		this.trianglePositions.add(z);
		this.triangleColors.add(argb);
	}

	public int lineVertexCount()
	{
		return this.lineColors.size();
	}

	public int triangleVertexCount()
	{
		return this.triangleColors.size();
	}

	/** Drops the spare capacity left from building, since the mesh is kept until the chunk is cleared. */
	void trim()
	{
		this.linePositions.trim();
		this.lineColors.trim();
		this.trianglePositions.trim();
		this.triangleColors.trim();
	}
}
