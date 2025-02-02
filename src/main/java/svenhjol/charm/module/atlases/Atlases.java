package svenhjol.charm.module.atlases;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import svenhjol.charm.Charm;
import svenhjol.charm.module.CharmModule;
import svenhjol.charm.handler.ModuleHandler;
import svenhjol.charm.helper.RegistryHelper;
import svenhjol.charm.helper.ItemNBTHelper;
import svenhjol.charm.helper.PlayerHelper;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.annotation.Module;
import svenhjol.charm.event.PlayerTickCallback;
import svenhjol.charm.init.CharmAdvancements;
import svenhjol.charm.mixin.accessor.MapItemSavedDataAccessor;
import svenhjol.charm.mixin.accessor.SlotAccessor;

import java.util.*;

@Module(mod = Charm.MOD_ID, client = AtlasesClient.class, description = "Storage for maps that automatically updates the displayed map as you explore.",
    requiresMixins = {"PlayerTickCallback", "RenderHeldItemCallback", "ItemTooltipCallback"})
public class Atlases extends CharmModule {
    public static final ResourceLocation ID = new ResourceLocation(Charm.MOD_ID, "atlas");
    public static final ResourceLocation MSG_SERVER_ATLAS_TRANSFER = new ResourceLocation(Charm.MOD_ID, "server_atlas_transfer");
    public static final ResourceLocation MSG_CLIENT_UPDATE_ATLAS_INVENTORY = new ResourceLocation(Charm.MOD_ID, "client_update_atlas_inventory");
    public static final ResourceLocation TRIGGER_MADE_ATLAS_MAPS = new ResourceLocation(Charm.MOD_ID, "made_atlas_maps");

    public static final int NUMBER_OF_MAPS_FOR_ACHIEVEMENT = 10;

    // add items to this list to whitelist them in atlases
    public static final List<Item> VALID_ATLAS_ITEMS = new ArrayList<>();
    private static final Map<UUID, AtlasInventory> serverCache = new HashMap<>();
    private static final Map<UUID, AtlasInventory> clientCache = new HashMap<>();

    @Config(name = "Open in off hand", description = "Allow opening the atlas while it is in the off hand.")
    public static boolean offHandOpen = false;

    @Config(name = "Map scale", description = "Map scale used in atlases by default.")
    public static int defaultScale = 0;

    public static AtlasItem ATLAS_ITEM;
    public static MenuType<AtlasContainer> CONTAINER;

    @Override
    public void register() {
        ATLAS_ITEM = new AtlasItem(this);

        VALID_ATLAS_ITEMS.add(Items.MAP);
        VALID_ATLAS_ITEMS.add(Items.FILLED_MAP);

        CONTAINER = RegistryHelper.screenHandler(ID, (syncId, playerInventory) -> new AtlasContainer(syncId, playerInventory, findAtlas(playerInventory)));
    }

    @Override
    public void init() {
        PlayerTickCallback.EVENT.register(this::handlePlayerTick);

        // listen for network requests to run the server callback
        ServerPlayNetworking.registerGlobalReceiver(MSG_SERVER_ATLAS_TRANSFER, this::handleServerAtlasTransfer);
    }

    public static boolean inventoryContainsMap(Inventory inventory, ItemStack itemStack) {
        if (inventory.contains(itemStack)) {
            return true;
        } else if (ModuleHandler.enabled(Atlases.class)) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack atlasStack = inventory.player.getItemInHand(hand);
                if (atlasStack.getItem() == ATLAS_ITEM) {
                    AtlasInventory inv = getInventory(inventory.player.level, atlasStack);
                    if (inv.hasItemStack(itemStack)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static AtlasInventory getInventory(Level world, ItemStack stack) {
        UUID id = ItemNBTHelper.getUuid(stack, AtlasInventory.ID);
        if (id == null) {
            id = UUID.randomUUID();
            ItemNBTHelper.setUuid(stack, AtlasInventory.ID, id);
        }
        Map<UUID, AtlasInventory> cache = world.isClientSide ? clientCache : serverCache;
        AtlasInventory inventory = cache.get(id);
        if (inventory == null) {
            inventory = new AtlasInventory(stack);
            cache.put(id, inventory);
        }
        if(inventory.getAtlasItem() != stack) {
            inventory.reload(stack);
        }
        return inventory;
    }

    public static void sendMapToClient(ServerPlayer player, ItemStack map, boolean markDirty) {
        if (map.getItem().isComplex()) {
            if(markDirty) {
                Integer mapId = MapItem.getMapId(map);
                MapItemSavedData mapState = MapItem.getSavedData(mapId, player.level);

                if (mapState == null) {
                    return;
                }

                ((MapItemSavedDataAccessor)mapState).invokeSetColorsDirty(0, 0);
            }
            map.getItem().inventoryTick(map, player.level, player, -1, true);
            Packet<?> packet = ((ComplexItem) map.getItem()).getUpdatePacket(map, player.level, player);
            if (packet != null) {
                player.connection.send(packet);
            }
        }
    }

    public static void updateClient(ServerPlayer player, int atlasSlot) {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeInt(atlasSlot);
        ServerPlayNetworking.send(player, MSG_CLIENT_UPDATE_ATLAS_INVENTORY, data);
    }

    private static AtlasInventory findAtlas(Inventory inventory) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = inventory.player.getItemInHand(hand);
            if (stack.getItem() == ATLAS_ITEM) {
                return getInventory(inventory.player.level, stack);
            }
        }
        throw new IllegalStateException("No atlas in any hand, can't open!");
    }

    public static void setupAtlasUpscale(Inventory playerInventory, CartographyTableMenu container) {
        if (ModuleHandler.enabled(Atlases.class)) {
            Slot oldSlot = container.slots.get(0);
            container.slots.set(0, new Slot(oldSlot.container, ((SlotAccessor)oldSlot).accessGetIndex(), oldSlot.x, oldSlot.y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return oldSlot.mayPlace(stack) || stack.getItem() == ATLAS_ITEM && getInventory(playerInventory.player.level, stack).getMapInfos().isEmpty();
                }
            });
        }
    }

    public static boolean makeAtlasUpscaleOutput(ItemStack topStack, ItemStack bottomStack, ItemStack outputStack, Level world,
        ResultContainer craftResultInventory, CartographyTableMenu cartographyContainer) {
        if (ModuleHandler.enabled(Atlases.class) && topStack.getItem() == ATLAS_ITEM) {
            AtlasInventory inventory = getInventory(world, topStack);
            ItemStack output;
            if (inventory.getMapInfos().isEmpty() && bottomStack.getItem() == Items.MAP && inventory.getScale() < 4) {
                output = topStack.copy();
                ItemNBTHelper.setUuid(output, AtlasInventory.ID, UUID.randomUUID());
                ItemNBTHelper.setInt(output, AtlasInventory.SCALE, inventory.getScale() + 1);
            } else {
                output = ItemStack.EMPTY;
            }
            if (!ItemStack.matches(output, outputStack)) {
                craftResultInventory.setItem(2, output);
                cartographyContainer.broadcastChanges();
            }
            return true;
        }
        return false;
    }

    public static void triggerMadeMaps(ServerPlayer player) {
        CharmAdvancements.ACTION_PERFORMED.trigger(player, TRIGGER_MADE_ATLAS_MAPS);
    }

    private void handlePlayerTick(Player player) {
        if (!player.level.isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack atlas = serverPlayer.getItemInHand(hand);
                if (atlas.getItem() == ATLAS_ITEM) {
                    AtlasInventory inventory = getInventory(serverPlayer.level, atlas);
                    if (inventory.updateActiveMap(serverPlayer)) {
                        updateClient(serverPlayer, getSlotFromHand(serverPlayer, hand));
                        if (inventory.getMapInfos().size() >= NUMBER_OF_MAPS_FOR_ACHIEVEMENT) {
                            triggerMadeMaps((ServerPlayer) player);
                        }
                    }
                }
            }
        }
    }

    private void handleServerAtlasTransfer(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf data, PacketSender sender) {
        int atlasSlot = data.readInt();
        int mapX = data.readInt();
        int mapZ = data.readInt();
        MoveMode mode = data.readEnum(MoveMode.class);

        server.execute(() -> {
            if (player == null)
                return;

            AtlasInventory inventory = getInventory(player.level, PlayerHelper.getInventory(player).getItem(atlasSlot));

            switch (mode) {
                case TO_HAND:
                    player.containerMenu.setCarried(inventory.removeMapByCoords(player.level, mapX, mapZ).map);
                    updateClient(player, atlasSlot);
                    break;
                case TO_INVENTORY:
                    player.addItem(inventory.removeMapByCoords(player.level, mapX, mapZ).map);
                    updateClient(player, atlasSlot);
                    break;
                case FROM_HAND:
                    ItemStack heldItem = player.containerMenu.getCarried();
                    if (heldItem.getItem() == Items.FILLED_MAP) {
                        Integer mapId = MapItem.getMapId(heldItem);
                        MapItemSavedData mapState = MapItem.getSavedData(mapId, player.level);
                        if (mapState != null && mapState.scale == inventory.getScale()) {
                            inventory.addToInventory(player.level, heldItem);
                            player.containerMenu.setCarried(ItemStack.EMPTY);
                            updateClient(player, atlasSlot);
                        }
                    }
                    break;
                case FROM_INVENTORY:
                    ItemStack stack = PlayerHelper.getInventory(player).getItem(mapX);
                    if (stack.getItem() == Items.FILLED_MAP) {
                        Integer mapId = MapItem.getMapId(stack);
                        MapItemSavedData mapState = MapItem.getSavedData(mapId, player.level);
                        if (mapState != null && mapState.scale == inventory.getScale()) {
                            inventory.addToInventory(player.level, stack);
                            PlayerHelper.getInventory(player).removeItemNoUpdate(mapX);
                            updateClient(player, atlasSlot);
                        }
                    }
                    break;
            }
        });
    }

    private static int getSlotFromHand(Player player, InteractionHand hand) {
        if(hand == InteractionHand.MAIN_HAND) {
            return PlayerHelper.getInventory(player).selected;
        } else {
            return PlayerHelper.getInventory(player).getContainerSize() - 1;
        }
    }

    public enum MoveMode {
        TO_HAND, TO_INVENTORY, FROM_HAND, FROM_INVENTORY
    }
}
