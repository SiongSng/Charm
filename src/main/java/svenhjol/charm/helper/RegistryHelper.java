package svenhjol.charm.helper;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import svenhjol.charm.mixin.accessor.BlockEntityTypeAccessor;
import svenhjol.charm.mixin.accessor.PotionBrewingAccessor;
import svenhjol.charm.mixin.accessor.SimpleParticleTypeAccessor;
import svenhjol.charm.mixin.accessor.WoodTypeAccessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnusedReturnValue")
public class RegistryHelper {
    public static List<String> SUPPRESS_DATA_FIXER_ERROR = new ArrayList<>();

    public static Block block(ResourceLocation id, Block block) {
        return Registry.register(Registry.BLOCK, id, block);
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(ResourceLocation id, BlockEntityType.BlockEntitySupplier<T> builder, Block... blocks) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.of(builder, blocks).build(null));
    }

    public static void brewingRecipe(Potion input, Item reagant, Potion output) {
        PotionBrewingAccessor.invokeAddMix(input, reagant, output);
    }

    public static ConfiguredFeature<?, ?> configuredFeature(ResourceLocation id, ConfiguredFeature<?, ?> configuredFeature) {
        ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registry.CONFIGURED_FEATURE_REGISTRY, id);
        BuiltinRegistries.register(BuiltinRegistries.CONFIGURED_FEATURE, key.location(), configuredFeature);
        return configuredFeature;
    }

    public static ConfiguredStructureFeature<?, ?> configuredStructureFeature(ResourceLocation id, ConfiguredStructureFeature<?, ?> configuredFeature) {
        ResourceKey<ConfiguredStructureFeature<?, ?>> key = ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, id);
        BuiltinRegistries.register(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE, key.location(), configuredFeature);
        return configuredFeature;
    }

    public static SimpleParticleType defaultParticleType(ResourceLocation id) {
        SimpleParticleType type = SimpleParticleTypeAccessor.invokeConstructor(false);
        return Registry.register(Registry.PARTICLE_TYPE, id.toString(), type);
    }

    public static Enchantment enchantment(ResourceLocation id, Enchantment enchantment) {
        return Registry.register(Registry.ENCHANTMENT, id, enchantment);
    }

    public static <T extends Entity> EntityType<T> entity(ResourceLocation id, FabricEntityTypeBuilder<T> build) {
        SUPPRESS_DATA_FIXER_ERROR.add(id.toString());
        EntityType<T> entityType = build.build();
        return Registry.register(Registry.ENTITY_TYPE, id, entityType);
    }

    public static Item item(ResourceLocation id, Item item) {
        return Registry.register(Registry.ITEM, id, item);
    }

    public static LootItemFunctionType lootFunctionType(ResourceLocation id, LootItemFunctionType lootFunctionType) {
        return Registry.register(Registry.LOOT_FUNCTION_TYPE, id, lootFunctionType);
    }

    public static WoodType signType(ResourceLocation id) {
        // crashes when using fully qualified namespace, so convert colon to underscore
        return WoodTypeAccessor.invokeRegister(WoodTypeAccessor.invokeInit(id.toString().replace(":", "_")));
    }

    public static PoiType pointOfInterestType(ResourceLocation id, PoiType poit) {
        return Registry.register(Registry.POINT_OF_INTEREST_TYPE, id, poit);
    }

    public static Potion potion(ResourceLocation id, Potion potion) {
        return Registry.register(Registry.POTION, id, potion);
    }

    public static <T extends Recipe<?>> RecipeType<T> recipeType(String recipeId) {
        return RecipeType.register(recipeId);
    }

    public static <S extends RecipeSerializer<T>, T extends Recipe<?>> S recipeSerializer(String recipeId, S serializer) {
        return RecipeSerializer.register(recipeId, serializer);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> screenHandler(ResourceLocation id, MenuType.MenuSupplier<T> factory) {
        return Registry.register(Registry.MENU, id, new MenuType<>(factory));
    }

    public static SoundEvent sound(ResourceLocation id, SoundEvent sound) {
        return Registry.register(Registry.SOUND_EVENT, id, sound);
    }

    public static MobEffect statusEffect(ResourceLocation id, MobEffect statusEffect) {
        return Registry.register(Registry.MOB_EFFECT, id, statusEffect);
    }

    public static StructurePieceType structurePiece(ResourceLocation id, StructurePieceType structurePieceType) {
        return Registry.register(Registry.STRUCTURE_PIECE, id, structurePieceType);
    }

    public static VillagerProfession villagerProfession(String id, VillagerProfession profession) {
        return Registry.register(Registry.VILLAGER_PROFESSION, id, profession);
    }

    public static void addBlocksToBlockEntity(BlockEntityType<?> type, Block... blocks) {
        Set<Block> typeBlocks = ((BlockEntityTypeAccessor) type).getValidBlocks();
        List<Block> mutable = new ArrayList<>(typeBlocks);

        for (Block block : blocks) {
            if (!mutable.contains(block))
                mutable.add(block);
        }

        ((BlockEntityTypeAccessor)type).setValidBlocks(new HashSet<>(mutable));
    }

}
