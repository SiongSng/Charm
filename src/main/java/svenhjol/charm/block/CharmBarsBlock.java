package svenhjol.charm.block;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import svenhjol.charm.module.CharmModule;

public class CharmBarsBlock extends IronBarsBlock implements ICharmBlock {
    private final CharmModule module;

    public CharmBarsBlock(CharmModule module, String name, Properties settings) {
        super(settings);

        this.module = module;
        this.register(module, name);
    }

    public CharmBarsBlock(CharmModule module, String name) {
        this(module, name, Properties.copy(Blocks.IRON_BARS));
    }

    @Override
    public CreativeModeTab getItemGroup() {
        return CreativeModeTab.TAB_DECORATIONS;
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        if (enabled())
            super.fillItemCategory(group, items);
    }

    @Override
    public boolean enabled() {
        return module.enabled;
    }
}
