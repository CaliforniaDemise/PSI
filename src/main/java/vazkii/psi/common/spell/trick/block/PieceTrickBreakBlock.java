/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: http://psi.vazkii.us/license.php
 *
 * File Created @ [24/01/2016, 15:35:27 (GMT)]
 */
package vazkii.psi.common.spell.trick.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fluids.IFluidBlock;
import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.param.ParamVector;
import vazkii.psi.api.spell.piece.PieceTrick;

public class PieceTrickBreakBlock extends PieceTrick {

	public static ThreadLocal<Boolean> doingHarvestCheck = ThreadLocal.withInitial(() -> false);

	SpellParam position;

	public PieceTrickBreakBlock(Spell spell) {
		super(spell);
	}

	@Override
	public void initParams() {
		addParam(position = new ParamVector(SpellParam.GENERIC_NAME_POSITION, SpellParam.BLUE, false, false));
	}

	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
		super.addToMetadata(meta);

		meta.addStat(EnumSpellStat.POTENCY, 20);
		meta.addStat(EnumSpellStat.COST, 25);
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		ItemStack tool = context.getHarvestTool();
		Vector3 positionVal = this.getParamValue(context, position);

		if(positionVal == null)
			throw new SpellRuntimeException(SpellRuntimeException.NULL_VECTOR);
		if(!context.isInRadius(positionVal))
			throw new SpellRuntimeException(SpellRuntimeException.OUTSIDE_RADIUS);

		BlockPos pos = positionVal.toBlockPos();
		removeBlockWithDrops(context, context.caster, context.caster.getEntityWorld(), tool, pos, true);

		return null;
	}

	public static void removeBlockWithDrops(SpellContext context, EntityPlayer player, World world, ItemStack tool, BlockPos pos, boolean particles) {
		if(!world.isBlockLoaded(pos) || (context.positionBroken != null && pos.equals(context.positionBroken.getBlockPos())) || !world.isBlockModifiable(player, pos))
			return;

		if (tool.isEmpty())
			tool = PsiAPI.getPlayerCAD(player);

		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if(!block.isAir(state, world, pos) && !(block instanceof BlockLiquid) && !(block instanceof IFluidBlock) && state.getBlockHardness(world, pos) != -1) {
			if(!canHarvestBlock(block, player, world, pos, tool))
				return;

			BreakEvent event = createBreakEvent(state, player, world, pos, tool);
			MinecraftForge.EVENT_BUS.post(event);
			if(!event.isCanceled()) {
				if(!player.capabilities.isCreativeMode) {
					TileEntity tile = world.getTileEntity(pos);

					if(block.removedByPlayer(state, world, pos, player, true)) {
						block.onPlayerDestroy(world, pos, state);
						block.harvestBlock(world, player, pos, state, tile, tool);
						block.dropXpOnBlockBreak(world, pos, event.getExpToDrop());
					}
				} else world.setBlockToAir(pos);
			}

			if(particles)
				world.playEvent(2001, pos, Block.getStateId(state));
		}
	}

	// Based on BreakEvent::new
	public static BreakEvent createBreakEvent(IBlockState state, EntityPlayer player, World world, BlockPos pos, ItemStack tool) {
		BreakEvent event = new BreakEvent(world, pos, state, player);
		if (state == null || !canHarvestBlock(state.getBlock(), player, world, pos, tool) ||
				(state.getBlock().canSilkHarvest(world, pos, world.getBlockState(pos), player) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0))
			event.setExpToDrop(0);
		else
			event.setExpToDrop(state.getBlock().getExpDrop(state, world, pos,
					EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool)));

		return event;
	}

	/**
	 * Item stack aware harvest check
	 * Also sets global state {@link PieceTrickBreakBlock#doingHarvestCheck} to true during the check
	 * @see Block#canHarvestBlock(IBlockAccess, BlockPos, EntityPlayer)
	 */
	public static boolean canHarvestBlock(Block block, EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
		// So the CAD can only be used as a tool when a harvest check is ongoing
		boolean wasChecking = doingHarvestCheck.get();
		doingHarvestCheck.set(true);

		// Swap the main hand with the stack temporarily to do the harvest check
		ItemStack oldHeldStack = player.getHeldItemMainhand();
		//player.setHeldItem(EnumHand.MAIN_HAND, oldHeldStack);
		// Need to do this instead of the above to prevent the re-equip sound
		player.inventory.mainInventory.set(player.inventory.currentItem, stack);

		// Harvest check
		boolean canHarvest = block.canHarvestBlock(world, pos, player);

		// Swap back the main hand
		player.inventory.mainInventory.set(player.inventory.currentItem, oldHeldStack);

		// Reset the harvest check to its previous value
		doingHarvestCheck.set(wasChecking);
		return canHarvest;
	}
}
