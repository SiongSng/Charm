package svenhjol.charm.module.variant_chests;

import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import svenhjol.charm.enums.IVariantMaterial;
import svenhjol.charm.event.RenderBlockItemCallback;
import svenhjol.charm.event.StitchTextureCallback;
import svenhjol.charm.module.CharmClientModule;
import svenhjol.charm.module.CharmModule;

import java.util.Set;

public class VariantChestsClient extends CharmClientModule {
    private final VariantChestBlockEntity CACHED_NORMAL_CHEST = new VariantChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
    private final VariantTrappedChestBlockEntity CACHED_TRAPPED_CHEST = new VariantTrappedChestBlockEntity(BlockPos.ZERO, Blocks.TRAPPED_CHEST.defaultBlockState());

    public VariantChestsClient(CharmModule module) {
        super(module);
    }

    @Override
    public void register() {
        BlockEntityRendererRegistry.INSTANCE.register(VariantChests.NORMAL_BLOCK_ENTITY, VariantChestBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(VariantChests.TRAPPED_BLOCK_ENTITY, VariantChestBlockEntityRenderer::new);

        StitchTextureCallback.EVENT.register(this::handleTextureStitch);
        RenderBlockItemCallback.EVENT.register(this::handleBlockItemRender);
    }

    private void handleTextureStitch(TextureAtlas atlas, Set<ResourceLocation> textures) {
        if (atlas.location().toString().equals("minecraft:textures/atlas/chest.png")) {
            VariantChests.NORMAL_CHEST_BLOCKS.keySet().forEach(type -> {
                addChestTexture(textures, type, ChestType.LEFT);
                addChestTexture(textures, type, ChestType.RIGHT);
                addChestTexture(textures, type, ChestType.SINGLE);
            });
        }
    }

    private BlockEntity handleBlockItemRender(Block block) {
        if (block instanceof VariantChestBlock) {
            VariantChestBlock chest = (VariantChestBlock)block;
            CACHED_NORMAL_CHEST.setMaterialType(chest.getMaterialType());
            return CACHED_NORMAL_CHEST;

        } else if (block instanceof VariantTrappedChestBlock) {
            VariantTrappedChestBlock chest = (VariantTrappedChestBlock)block;
            CACHED_TRAPPED_CHEST.setMaterialType(chest.getMaterialType());
            return CACHED_TRAPPED_CHEST;
        }

        return null;
    }

    private void addChestTexture(Set<ResourceLocation> textures, IVariantMaterial variant, ChestType chestType) {
        String chestTypeName = chestType != ChestType.SINGLE ? "_" + chestType.getSerializedName().toLowerCase() : "";
        String[] bases = {"trapped", "normal"};

        for (String base : bases) {
            ResourceLocation id = new ResourceLocation(module.mod, "entity/chest/" + variant.getSerializedName() + "_" + base + chestTypeName);
            VariantChestBlockEntityRenderer.addTexture(variant, chestType, id, base.equals("trapped"));
            textures.add(id);
        }
    }
}
