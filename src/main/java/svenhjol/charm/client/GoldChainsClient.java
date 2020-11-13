package svenhjol.charm.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import svenhjol.charm.base.CharmClientModule;
import svenhjol.charm.module.GoldChains;

public class GoldChainsClient extends CharmClientModule {
    public GoldChainsClient(GoldChains module) {
        super(module);
    }

    @Override
    public void register() {
        RenderTypeLookup.setRenderLayer(GoldChains.GOLD_CHAIN, RenderType.getCutout());
    }
}
