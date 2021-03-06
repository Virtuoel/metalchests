package T145.metalchests.blocks;

import javax.annotation.Nullable;

import T145.metalchests.MetalChests;
import T145.metalchests.lib.MetalChestType;
import T145.metalchests.tiles.TileMetalChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

public class BlockMetalChest extends BlockContainer {

	public static final PropertyEnum<MetalChestType> VARIANT = PropertyEnum.<MetalChestType>create("variant", MetalChestType.class);

	public BlockMetalChest() {
		super(Material.IRON);
		setRegistryName(new ResourceLocation(MetalChests.MODID, "metal_chest"));
		setDefaultState(blockState.getBaseState().withProperty(VARIANT, MetalChestType.IRON));
		setUnlocalizedName("metalchests:metal_chest");
		setHardness(3F);
		setCreativeTab(MetalChests.TAB);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
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
	public Material getMaterial(IBlockState state) {
		return state.getValue(VARIANT).getMaterial();
	}

	@Override
	public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
		return state.getValue(VARIANT).getSoundType();
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
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return Blocks.CHEST.getBoundingBox(state, source, pos);
	}

	@Override
	public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof TileMetalChest) {
			TileMetalChest chest = (TileMetalChest) te;

			if (chest.getType() == MetalChestType.OBSIDIAN) {
				return 10000F;
			}
		}

		return super.getExplosionResistance(world, pos, exploder, explosion);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof TileMetalChest) {
			TileMetalChest chest = (TileMetalChest) te;

			for (int i = 0; i < chest.getInventory().getSlots(); ++i) {
				ItemStack stack = chest.getInventory().getStackInSlot(i);

				if (!stack.isEmpty()) {
					InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
				}
			}

			world.updateComparatorOutputLevel(pos, this);
		}

		super.breakBlock(world, pos, state);
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
		TileEntity te = world.getTileEntity(pos);

		if (!world.isRemote && te instanceof TileMetalChest) {
			TileMetalChest chest = (TileMetalChest) te;

			if (!player.isSneaking() && !isBlocked(world, pos)) {
				player.openGui(MetalChests.MODID, 0, world, pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return true;
	}

	private boolean isBlocked(World world, BlockPos pos) {
		return isBelowSolidBlock(world, pos) || isOcelotSittingOnChest(world, pos);
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
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof TileMetalChest) {
			TileMetalChest chest = (TileMetalChest) te;
			return ItemHandlerHelper.calcRedstoneFromInventory(chest.getInventory());
		}

		return 0;
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
		return new BlockStateContainer(this, VARIANT);
	}
}