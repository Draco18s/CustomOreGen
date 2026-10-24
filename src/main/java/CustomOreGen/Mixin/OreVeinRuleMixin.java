package CustomOreGen.Mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import CustomOreGen.Server.SaveLevelConfig;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;

/**
 * Implements the vanillaOreVeins option. Minecraft's large ore veins (copper/granite, iron/tuff) are
 * material rules (data/minecraft/worldgen/material_rule/overworld/*_ore_vein.json) applied while terrain
 * is filled in. When the COG config turns them off, the rule compiles to one that never picks a block,
 * the same thing vanilla does for its own DEBUG_DISABLE_ORE_VEINS switch. Patching the rule, rather than
 * the data, leaves the world's saved settings untouched.
 */
@Mixin(OreVeinRule.class)
public class OreVeinRuleMixin
{
	@Inject(method = "compile", at = @At("HEAD"), cancellable = true)
	private void customoregen$disableVeins(MaterialRuleContext context, CallbackInfoReturnable<RuleEvaluator> cir)
	{
		if (!SaveLevelConfig.vanillaOreVeins())
		{
			cir.setReturnValue((x, y, z) -> null);
		}
	}
}
