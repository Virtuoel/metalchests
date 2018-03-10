package T145.metalchests.blocks;

import javax.annotation.Nullable;

import T145.metalchests.lib.MetalChestType;
import T145.metalchests.tiles.TileMetalChest;
import T145.metalchests.tiles.base.TileInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockMetalChest extends BlockContainer {

	public static final PropertyEnum<MetalChestType> VARIANT = PropertyEnum.<MetalChestType>create("variant", MetalChestType.class);
	public static final AxisAlignedBB CHEST_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);

	public BlockMetalChest() {
		super(Material.IRON);
		setDefaultState(blockState.getBaseState().withProperty(VARIANT, MetalChestType.IRON));
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileMetalChest(MetalChestType.values()[meta]);
	}

	@Override
	public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
		return state.getValue(VARIANT).getMapColor();
	}

	@Override
	protected Block setSoundType(SoundType sound) {
		blockSoundType = getDefaultState().getValue(VARIANT).getSoundType();
		return this;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasCustomBreakingProgress(IBlockState state) {
		return true;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return CHEST_AABB;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof TileInventory) {
			dropInventoryItems(world, pos, (TileInventory) te);
			world.updateComparatorOutputLevel(pos, this);
		}

		super.breakBlock(world, pos, state);
	}

	private void dropInventoryItems(World world, BlockPos pos, TileInventory inventory) {
		for (int slot = 0; slot < inventory.getInventorySize(); ++slot) {
			ItemStack stack = inventory.getInventory().getStackInSlot(slot);

			if (!stack.isEmpty()) {
				InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof TileMetalChest) {
			EnumFacing front = EnumFacing.getHorizontal(MathHelper.floor((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3).getOpposite();
			((TileMetalChest) te).setFront(front);
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		// show custom gui

		return true;
	}

	private boolean isBlocked(World world, BlockPos pos) {
		return this.isBelowSolidBlock(world, pos) || this.isOcelotSittingOnChest(world, pos);
	}

	private boolean isBelowSolidBlock(World world, BlockPos pos) {
		return world.getBlockState(pos.up()).doesSideBlockChestOpening(world, pos.up(), EnumFacing.DOWN);
	}

	private boolean isOcelotSittingOnChest(World world, BlockPos pos) {
		for (Entity entity : world.getEntitiesWithinAABB(EntityOcelot.class, new AxisAlignedBB(pos.getX(), (pos.getY() + 1), pos.getZ(), (pos.getX() + 1), (pos.getY() + 2), (pos.getZ() + 1)))) {
			EntityOcelot ocelot = (EntityOcelot) entity;

			if (ocelot.isSitting()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
		return calcRedstoneFromInventory((TileInventory) world.getTileEntity(pos));
	}

	public static int calcRedstoneFromInventory(@Nullable TileInventory inv) {
		if (inv == null) {
			return 0;
		} else {
			int i = 0;
			float f = 0.0F;

			for (int j = 0; j < inv.getInventorySize(); ++j) {
				ItemStack itemstack = inv.getInventory().getStackInSlot(j);

				if (!itemstack.isEmpty()) {
					f += itemstack.getCount() / Math.min(inv.getInventory().getSlotLimit(j), itemstack.getMaxStackSize());
					++i;
				}
			}

			f = f / inv.getInventorySize();
			return MathHelper.floor(f * 14.0F) + (i > 0 ? 1 : 0);
		}
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public int damageDropped(IBlockState state) {
		return state.getValue(VARIANT).ordinal();
	}

	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
		for (MetalChestType type : MetalChestType.values()) {
			if (type.isRegistered()) {
				items.add(new ItemStack(this, 1, type.ordinal()));
			}
		}
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(VARIANT, MetalChestType.values()[meta]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(VARIANT).ordinal();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { VARIANT });
	}
}