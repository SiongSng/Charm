package svenhjol.charm.base;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import svenhjol.charm.module.StackableBooks;
import svenhjol.charm.module.StackablePotions;
import svenhjol.meson.Meson;

public class CharmAsmHooks {
    /**
     * If true, the potion glint will not be applied to the PotionItem.
     * Disable the RemovePotionGlint to restore vanilla behavior.
     * @return True to disable potion glint.
     */
    public static boolean removePotionGlint() {
        return Meson.enabled("charm:remove_potion_glint");
    }

    /**
     * Overrides the vanilla default minimum XP of 1 (> 0) to zero (> -1).
     * @return -1 if NoAnvilMinimumXp is enabled, vanilla default of 0 if not enabled.
     */
    public static int getMinimumRepairCost() {
        return Meson.enabled("charm:no_anvil_minimum_xp") ? -1 : 0;
    }

    /**
     * Simply checks if the StackablePotions module is enabled and that the input stack can be added.
     * Returning true forces Forge's BrewingRecipeRegistry#isValidInput method to return true.
     * @param stack ItemStack to verify as valid brewing stand input.
     * @return True to force BrewingRecipeRegistry#isValidInput to be valid.
     */
    public static boolean checkBrewingStandStack(ItemStack stack) {
        return Meson.enabled("charm:stackable_potions") && StackablePotions.isValidItemStack(stack);
    }

    public static ItemStack checkAnvilInventory(IInventory inventory) {
        if (Meson.enabled("charm:stackable_books"))
            return StackableBooks.checkItemStack(inventory.getStackInSlot(1));

        return ItemStack.EMPTY;
    }
}
