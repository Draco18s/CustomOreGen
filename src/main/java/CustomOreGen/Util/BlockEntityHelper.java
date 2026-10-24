package CustomOreGen.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

@SuppressWarnings("unchecked")
public class BlockEntityHelper {

	public static void readFromPartialNBT(BlockEntity te, CompoundTag source) {
		HolderLookup.Provider registries = te.getLevel().registryAccess();
		CompoundTag dest = te.saveWithFullMetadata(registries);
		dest.merge(source);
		te.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, dest));
	}

	public static void readFromPartialNBT(LevelAccessor world, int x, int y, int z, CompoundTag source) {
		if (source != null) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockEntity te = world.getBlockEntity(pos);
			if (te == null) {
				//te = tryToCreateGTPrefixBlockBlockEntity(world.getBlockState(pos).getBlock());
				//if (te != null) {
				//	world.setBlockEntity(te);
				//}
			}
			if (te != null) {
				BlockEntityHelper.readFromPartialNBT(te, source);
				BlockState state = world.getBlockState(pos);
				world.updateNeighborsAt(pos, state.getBlock());
			}
		}
	}

	static private Class<? extends Block> prefixBlockClass;
	static private Class<? extends BlockEntity> prefixBlockBlockEntityClass;

	static {
		try {
			prefixBlockClass = (Class<? extends Block>) Class.forName("gregapi.block.prefixblock.PrefixBlock");
			prefixBlockBlockEntityClass = (Class<? extends BlockEntity>) Class.forName("gregapi.block.prefixblock.PrefixBlockBlockEntity");
		} catch (ClassNotFoundException e) {
		}
	}

	public static CompoundTag tryToCreateGTPrefixBlockNBT(ItemStack ore) {
		Block block = ((BlockItem)ore.getItem()).getBlock();
		CompoundTag nbt = null;
		if (isGTPrefixBlock(block)) {
			nbt = new CompoundTag();
			nbt.putString("id", "gt.MetaBlockBlockEntity");
		}
		return nbt;
	}

	private static boolean isGTPrefixBlock(Block block) {
		return prefixBlockClass != null && prefixBlockClass.isAssignableFrom(block.getClass());
	}

	@SuppressWarnings({ "deprecation", "unused" })
	private static BlockEntity tryToCreateGTPrefixBlockBlockEntity(Block block) {
		try {
			if (isGTPrefixBlock(block)) {
				return prefixBlockBlockEntityClass.newInstance();
			}
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}

}
