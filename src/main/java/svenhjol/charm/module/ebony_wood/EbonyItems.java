package svenhjol.charm.module.ebony_wood;

import svenhjol.charm.item.CharmBoatItem;
import svenhjol.charm.item.CharmSignItem;
import svenhjol.charm.module.CharmModule;
import svenhjol.charm.module.extra_boats.CharmBoatEntity;

public class EbonyItems {
    public static class EbonyBoatItem extends CharmBoatItem {
        public EbonyBoatItem(CharmModule module) {
            super(module, "ebony_boat", CharmBoatEntity.BoatType.EBONY);
        }
    }

    public static class EbonySignItem extends CharmSignItem {
        public EbonySignItem(CharmModule module) {
            super(module, "ebony_sign", EbonyWood.SIGN_BLOCK, EbonyWood.WALL_SIGN_BLOCK);
        }
    }
}
