package svenhjol.charm.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import svenhjol.charm.annotation.CharmMixin;

@Mixin(HoglinAi.class)
@CharmMixin(required = true)
public interface HoglinAiAccessor {
    @Invoker
    static void invokeSetAvoidTarget(Hoglin hoglin, LivingEntity target) {
        throw new IllegalStateException();
    }
}
