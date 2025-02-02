package svenhjol.charm.mixin.accessor;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import svenhjol.charm.annotation.CharmMixin;

@Mixin(ItemEntity.class)
@CharmMixin(required = true)
public interface ItemEntityAccessor {
    @Accessor
    int getAge();
}