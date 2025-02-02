package svenhjol.charm.module.more_portal_frames;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import svenhjol.charm.Charm;
import svenhjol.charm.module.CharmModule;
import svenhjol.charm.init.CharmTags;
import svenhjol.charm.handler.ModuleHandler;
import svenhjol.charm.annotation.Module;

@Module(mod = Charm.MOD_ID, description = "Crying obsidian can be used to construct nether portal frames.",
    requiresMixins = {"more_portal_frames.*"})
public class MorePortalFrames extends CharmModule {
    public static boolean isValidBlock(BlockState blockState) {
        if (!ModuleHandler.enabled(MorePortalFrames.class))
            return blockState.is(Blocks.OBSIDIAN); // vanilla

        return blockState.is(CharmTags.NETHER_PORTAL_FRAMES);
    }
}
