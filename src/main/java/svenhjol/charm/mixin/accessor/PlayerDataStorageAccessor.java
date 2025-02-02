package svenhjol.charm.mixin.accessor;

import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import svenhjol.charm.annotation.CharmMixin;

import java.io.File;

@Mixin(PlayerDataStorage.class)
@CharmMixin(required = true)
public interface PlayerDataStorageAccessor {
    @Accessor
    File getPlayerDir();
}
