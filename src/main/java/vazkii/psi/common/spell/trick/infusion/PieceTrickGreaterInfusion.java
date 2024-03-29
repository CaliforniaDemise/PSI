/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: http://psi.vazkii.us/license.php
 *
 * File Created @ [06/02/2016, 19:33:34 (GMT)]
 */
package vazkii.psi.common.spell.trick.infusion;

import net.minecraft.item.ItemStack;
import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.recipe.TrickRecipe;
import vazkii.psi.api.spell.*;
import vazkii.psi.common.item.ItemCAD;
import vazkii.psi.common.item.base.ModItems;
import vazkii.psi.common.lib.LibPieceNames;

import java.util.stream.Collectors;

public class PieceTrickGreaterInfusion extends PieceTrickInfusion {
	public PieceTrickGreaterInfusion(Spell spell) {
		super(spell);
	}

	@Override
	protected void addPotencyAndCost(SpellMetadata meta) {
		meta.addStat(EnumSpellStat.POTENCY, 250);
		meta.addStat(EnumSpellStat.COST, 2600);
	}

	@Override
	public Object execute(SpellContext context) {
		super.execute(context);
		for (TrickRecipe recipe :
				PsiAPI.trickRecipes.stream()
						.filter(recipe -> LibPieceNames.TRICK_GREATER_INFUSION.equals(recipe.getPiece()))
						.collect(Collectors.toList())
		) {

			ItemCAD.craft(context.caster, recipe.getInput(), recipe.getOutput());
		}
		return null;
	}

}
