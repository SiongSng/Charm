package svenhjol.charm.module.atlases;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import svenhjol.charm.helper.ItemNBTHelper;
import svenhjol.charm.init.CharmSounds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class AtlasInventory implements MenuProvider, Container {
    public static final String EMPTY_MAPS = "empty_maps";
    public static final String FILLED_MAPS = "filled_maps";
    public static final String ACTIVE_MAP = "active_map";
    public static final String SCALE = "scale";
    public static final String ID = "id";
    private static final int EMPTY_MAP_SLOTS = 3;
    private final Table<ResourceKey<Level>, Index, MapInfo> mapInfos;
    private final NonNullList<ItemStack> emptyMaps;
    private int diameter;
    private ItemStack atlas;
    private int scale;
    private boolean isOpen = false;

    public AtlasInventory(ItemStack atlas) {
        this.atlas = atlas;
        this.scale = Atlases.defaultScale;
        this.diameter = 128;
        this.emptyMaps = NonNullList.withSize(EMPTY_MAP_SLOTS, ItemStack.EMPTY);
        this.mapInfos = HashBasedTable.create();
        load();
    }

    public void reload(ItemStack atlas) {
        this.atlas = atlas;
        emptyMaps.clear();
        mapInfos.clear();
        load();
    }

    private void load() {
        scale = ItemNBTHelper.getInt(atlas, SCALE, Atlases.defaultScale);
        diameter = 128 * (1 << scale);
        ContainerHelper.loadAllItems(ItemNBTHelper.getCompound(atlas, EMPTY_MAPS), emptyMaps);
        ListTag listNBT = ItemNBTHelper.getList(atlas, FILLED_MAPS);
        for (int i = 0; i < listNBT.size(); ++i) {
            putMapInfo(MapInfo.readFrom(listNBT.getCompound(i)));
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    private void putMapInfo(MapInfo mapInfo) {
        mapInfos.put(mapInfo.dimension, convertCoordsToIndex(mapInfo.x, mapInfo.z), mapInfo);
    }

    public Index getIndexOf(Player player) {
        return convertCoordsToIndex((int) player.getX() + 64,(int) player.getZ() + 64);
    }

    public Index convertCoordsToIndex(int x, int y) {
        return Index.of(convertCoordToIndex(x), convertCoordToIndex(y));
    }

    public int convertCoordToIndex(int coordinate) {
        return Math.floorDiv(coordinate, diameter);
    }

    @Nullable
    private static MapInfo createMapInfo(Level world, ItemStack map) {
        Integer mapId = MapItem.getMapId(map);
        if (mapId == null) return null;

        MapItemSavedData mapData = MapItem.getSavedData(mapId, world);
        return mapData != null ? new MapInfo(mapData.x, mapData.z, MapItem.getMapId(map), map, mapData.dimension) : null;
    }

    public boolean updateActiveMap(ServerPlayer player) {
        MapInfo activeMap = mapInfos.get(player.level.dimension(), getIndexOf(player));
        boolean madeNewMap = false;
        if (activeMap == null && !isOpen) {
            activeMap = makeNewMap(player, (int) player.getX(), (int) player.getZ());
            madeNewMap = activeMap != null;
        }
        if (activeMap != null) {
            Atlases.sendMapToClient(player, activeMap.map, false);
            ItemNBTHelper.setInt(atlas, ACTIVE_MAP, activeMap.id);
        } else {
            ItemNBTHelper.setInt(atlas, ACTIVE_MAP, -1);
        }
        return madeNewMap;
    }

    private MapInfo makeNewMap(ServerPlayer player, int x, int z) {
        for (int i = 0; i < EMPTY_MAP_SLOTS; ++i) {
            ItemStack stack = emptyMaps.get(i);
            if (stack.getItem() == Items.MAP) {
                if (!player.isCreative()) {
                    removeItem(i, 1);
                }
                ItemStack map = MapItem.create(player.level, x, z, (byte) scale, true, true);
                MapInfo mapInfo = createMapInfo(player.level, map);
                putMapInfo(mapInfo);
                setChanged();
                player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.5f,
                    player.level.random.nextFloat() * 0.1F + 0.9F);
                return mapInfo;
            }
        }
        return null;
    }

    @Nullable
    public int getActiveMapId(Level world) {
        return ItemNBTHelper.getInt(atlas, ACTIVE_MAP, -1);
    }

    @Nullable
    public MapItemSavedData getActiveMap(Level world) {
        int mapId = getActiveMapId(world);
        if (mapId == -1)
            return null;

        return world.getMapData(MapItem.makeKey(mapId));
    }

    @Nullable
    public ItemStack getLastActiveMapItem() {
        int activeId = ItemNBTHelper.getInt(atlas, ACTIVE_MAP, -1);
        if (activeId == -1) return null;
        return mapInfos.values().stream().filter(it -> it.id == activeId).findAny().map(it -> it.map).orElse(null);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return atlas.getHoverName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
        return new AtlasContainer(syncId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return EMPTY_MAP_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.emptyMaps) {
            if (!stack.isEmpty()) return false;
        }
        return mapInfos.isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack getItem(int index) {
        return emptyMaps.get(index);
    }

    @Nonnull
    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemstack = ContainerHelper.removeItem(emptyMaps, index, count);
        setChanged();
        return itemstack;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack itemStack = ContainerHelper.takeItem(emptyMaps, index);
        setChanged();
        return itemStack;
    }

    public MapInfo removeMapByCoords(Level world, int x, int z) {
        MapInfo info = mapInfos.remove(world.dimension(), convertCoordsToIndex(x, z));
        setChanged();
        return info;
    }

    public void addToInventory(Level world, ItemStack itemStack) {
        if (itemStack.getItem() == Items.FILLED_MAP) {
            putMapInfo(createMapInfo(world, itemStack));
            setChanged();
        }
    }

    @Override
    public void setItem(int index, @Nonnull ItemStack stack) {
        if (!emptyMaps.get(index).equals(stack)) { // WARNING: doesn't match Forge
            emptyMaps.set(index, stack);
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        ItemNBTHelper.setInt(atlas, SCALE, scale);
        CompoundTag emptyMapNBT = new CompoundTag();
        ContainerHelper.saveAllItems(emptyMapNBT, emptyMaps, false);
        ItemNBTHelper.setCompound(atlas, EMPTY_MAPS, emptyMapNBT);
        ListTag listNBT = new ListTag();
        for (MapInfo mapInfo : mapInfos.values()) {
            CompoundTag nbt = new CompoundTag();
            mapInfo.writeTo(nbt);
            listNBT.add(nbt);
        }
        ItemNBTHelper.setList(atlas, FILLED_MAPS, listNBT);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (heldItem.getItem() == Atlases.ATLAS_ITEM && Objects.equals(ItemNBTHelper.getUuid(atlas, ID), ItemNBTHelper.getUuid(heldItem, ID))) return true;
        }
        return false;
    }

    @Override
    public void clearContent() {
        emptyMaps.clear();
        mapInfos.clear();
    }

    @Override
    public void startOpen(Player player) {
        isOpen = true;
        if(!player.level.isClientSide) {
            player.playNotifySound(CharmSounds.BOOKSHELF_OPEN, SoundSource.BLOCKS, 0.5f, player.level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public void stopOpen(Player player) {
        isOpen = false;
        if(!player.level.isClientSide) {
            player.playNotifySound(CharmSounds.BOOKSHELF_CLOSE, SoundSource.BLOCKS, 0.5f, player.level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    public boolean hasItemStack(ItemStack stack) {
        return Stream.concat(emptyMaps.stream(), mapInfos.values().stream().map(it -> it.map)).anyMatch(it -> !it.isEmpty() && it.sameItemStackIgnoreDurability(stack));
    }

    public ItemStack getAtlasItem() {
        return atlas;
    }

    public Table<ResourceKey<Level>, Index, MapInfo> getMapInfos() {
        return mapInfos;
    }

    public Map<Index, MapInfo> getCurrentDimensionMapInfos(Level world) {
        return mapInfos.row(world.dimension());
    }

    public int getScale() {
        return scale;
    }

    public static class MapInfo {
        private static final String X = "x";
        private static final String Z = "z";
        private static final String ID = "id";
        private static final String MAP = "map";
        private static final String DIMENSION = "dimension";
        public final int x;
        public final int z;
        public final int id;
        public final ItemStack map;
        public final ResourceKey<Level> dimension;

        private MapInfo(int x, int z, int id, ItemStack map, ResourceKey<Level> dimension) {
            this.x = x;
            this.z = z;
            this.id = id;
            this.map = map;
            this.dimension = dimension;
        }

        public static MapInfo readFrom(CompoundTag nbt) {
            return new MapInfo(nbt.getInt(X), nbt.getInt(Z), nbt.getInt(ID), ItemStack.of(nbt.getCompound(MAP)),
                DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, nbt.get(DIMENSION))).result().orElse(Level.OVERWORLD));
        }

        public void writeTo(CompoundTag nbt) {
            nbt.putInt(X, x);
            nbt.putInt(Z, z);
            nbt.putInt(ID, id);
            CompoundTag mapNBT = new CompoundTag();
            map.save(mapNBT);
            nbt.put(MAP, mapNBT);
            ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, dimension.location()).result().ifPresent(it -> nbt.put(DIMENSION, it));
        }
    }

    public static class Index {
        private static final Int2ObjectMap<Int2ObjectMap<Index>> cache = new Int2ObjectOpenHashMap<>();
        public final int x;
        public final int y;

        private Index(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Index plus(Index value) {
            return transform(it -> it.apply(this) + it.apply(value));
        }

        public Index plus(int value) {
            return transform(it -> it.apply(this) + value);
        }

        public Index minus(Index value) {
            return transform(it -> it.apply(this) - it.apply(value));
        }

        public Index minus(int value) {
            return transform(it -> it.apply(this) - value);
        }

        public Index multiply(int value) {
            return transform(it -> it.apply(this) * value);
        }

        public Index divide(int value) {
            return transform(it -> it.apply(this) / value);
        }

        public Index clamp(Index min, Index max) {
            return transform(it -> Mth.clamp(it.apply(this), it.apply(min), it.apply(max)));
        }

        private Index transform(Function<Function<Index, Integer>, Integer> transformer) {
            return Index.of(transformer.apply(it -> it.x), transformer.apply(it -> it.y));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Index index = (Index) o;
            return x == index.x && y == index.y;
        }

        @Override
        public int hashCode() {
            return 7079 * x + y;
        }

        public static Index of(int x, int y) {
            Int2ObjectMap<Index> columnCache = cache.get(x);
            if (columnCache == null) {
                columnCache = new Int2ObjectOpenHashMap<>();
                cache.put(x, columnCache);
            }
            Index index = columnCache.get(y);
            if (index == null) {
                index = new Index(x, y);
                columnCache.put(y, index);
            }
            return index;
        }
    }
}
