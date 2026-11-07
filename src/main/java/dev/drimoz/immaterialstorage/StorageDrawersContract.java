package dev.drimoz.immaterialstorage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import com.jaquadro.minecraft.storagedrawers.item.EnumUpgradeCreative;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeCreative;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStorage;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeVoid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Every point where this mod touches Storage Drawers outside its {@code api} package, in one file.
 *
 * <p>When a Storage Drawers release breaks us, this is where to look first. It is not a wrapper
 * around their API: the interfaces of {@code com.jaquadro.minecraft.storagedrawers.api} are used
 * directly everywhere else.
 */
public final class StorageDrawersContract {

    /**
     * Storage Drawers' {@code DRAWER_GROUP} capability, recreated by name instead of read from their
     * internal {@code Capabilities} class. NeoForge interns block capabilities by name, so this is
     * the same instance their controller queries.
     *
     * <p>The context class must be {@code Void.TYPE} (primitive {@code void}), not
     * {@code Void.class}: that is what Storage Drawers passes, and NeoForge refuses to create a
     * capability whose name already exists with a different context class.
     */
    public static final BlockCapability<IDrawerGroup, Void> DRAWER_GROUP = BlockCapability.create(
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "drawer_group"),
            IDrawerGroup.class, Void.TYPE);

    /** Registry name of the controller block ({@code ModBlocks.CONTROLLER}). */
    public static final ResourceLocation CONTROLLER_BLOCK =
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "controller");

    /**
     * Registry name of the controller's block entity type ({@code ModBlockEntities.CONTROLLER}).
     * One type for every controller block, framed ones included. Its block entities implement
     * {@code IControlGroup}.
     */
    public static final ResourceLocation CONTROLLER_BLOCK_ENTITY =
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "controller");

    /*
     * Texture names our models will borrow from Storage Drawers' own assets, so non-item drawers
     * share the controller's non-wood look without copying any file (Storage Drawers is a required
     * dependency, its textures are always there). A rename on their side shows as a missing
     * texture, not a crash. Used from model JSONs, listed here so the coupling has one home:
     * storagedrawers:block/drawers_comp_side, drawers_comp_side_2, drawers_comp_trim, and the
     * parent model storagedrawers:block/drawer_orientable.
     */

    // --- Upgrades: Storage Drawers' own items, read through their internal classes. A deliberate
    // choice over adding items of ours; every such reference is below. ---

    /**
     * What a Storage Drawers storage upgrade adds to a drawer's multiplier, from their config
     * (defaults 2, 4, 8, 16, 64, 256, 2048 for obsidian to netherite); 0 for anything else, or an
     * upgrade disabled in their config.
     */
    public static int storageMultiplier(ItemStack stack) {
        return stack.getItem() instanceof ItemUpgradeStorage upgrade && upgrade.isEnabled()
                ? ModCommonConfig.INSTANCE.UPGRADES.getLevelMult(upgrade.level.getLevel())
                : 0;
    }

    /** The multiplier of a drawer with no storage upgrade (1 by default), as Storage Drawers does. */
    public static int baseMultiplier() {
        return ModCommonConfig.INSTANCE.UPGRADES.getLevelMult(0);
    }

    public static boolean isVoidUpgrade(ItemStack stack) {
        return stack.getItem() instanceof ItemUpgradeVoid upgrade && upgrade.isEnabled();
    }

    /** "Near-infinite storage" in Storage Drawers' words. */
    public static boolean isCreativeStorageUpgrade(ItemStack stack) {
        return stack.getItem() instanceof ItemUpgradeCreative upgrade && upgrade.isEnabled()
                && upgrade.type == EnumUpgradeCreative.STORAGE;
    }

    /** "Unlimited withdrawal" in Storage Drawers' words. */
    public static boolean isCreativeVendingUpgrade(ItemStack stack) {
        return stack.getItem() instanceof ItemUpgradeCreative upgrade && upgrade.isEnabled()
                && upgrade.type == EnumUpgradeCreative.VENDING;
    }

    private StorageDrawersContract() {
    }
}
