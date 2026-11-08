package dev.drimoz.immaterialstorage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityControllerIO;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import com.jaquadro.minecraft.storagedrawers.item.EnumUpgradeCreative;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgrade;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeCreative;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStorage;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeVoid;
import com.jaquadro.minecraft.storagedrawers.util.CountFormatter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.List;

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

    /**
     * Registry name of the controller's block entity type ({@code ModBlockEntities.CONTROLLER}).
     * One type for every controller block, framed ones included. Its block entities implement
     * {@code IControlGroup}.
     */
    public static final ResourceLocation CONTROLLER_BLOCK_ENTITY =
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "controller");

    /**
     * Registry name of the Controller Slave's block entity type, framed ones included. Its block
     * entities are {@code IControlGroup}s whose {@code getBoundControlGroup()} is their controller.
     */
    public static final ResourceLocation CONTROLLER_IO_BLOCK_ENTITY =
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "controller_io");

    /**
     * Storage Drawers' single-drawer screen texture, which our upgrade screen draws on so it is the
     * screen players know. Its slot positions are copied in DrawerMenu and DrawerScreen.
     */
    public static final ResourceLocation DRAWER_SCREEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "textures/gui/drawers_1.png");

    /**
     * The controller and Controller Slave block entity classes, for probe mods that register by
     * class. Internal Storage Drawers classes, hence here.
     */
    public static final List<Class<? extends BlockEntity>> CONTROLLER_CLASSES =
            List.of(BlockEntityController.class, BlockEntityControllerIO.class);

    /** Storage Drawers' creative tab, where our blocks are listed after theirs. */
    public static final ResourceKey<CreativeModeTab> CREATIVE_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "storagedrawers"));

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

    /**
     * Storage Drawers' own rule for whether an upgrade fits next to the ones installed: kinds that
     * allow several (storage tiers) always fit, any other kind fits once per group. Creative storage
     * and creative vending share a group, so a drawer holds one or the other, as in Storage Drawers.
     */
    public static boolean fitsAlongside(ItemStack candidate, Iterable<ItemStack> installed) {
        if (!(candidate.getItem() instanceof ItemUpgrade upgrade) || !upgrade.isEnabled()) {
            return false;
        }
        if (upgrade.getAllowMultiple()) {
            return true;
        }
        for (ItemStack stack : installed) {
            if (stack.getItem() instanceof ItemUpgrade other && other.getUpgradeGroup() == upgrade.getUpgradeGroup()) {
                return false;
            }
        }
        return true;
    }

    // --- Text ---

    /**
     * An amount written the way Storage Drawers writes counts on its own drawers (1234, 12.3K, 345M,
     * 1.2G), with their formatter, so a wall of drawers reads alike. Theirs is int; past it, energy
     * carries on in the same notation (G, T, P). Client side only: their class refers to the client
     * font, which they do not use for this.
     */
    public static String formatCount(long amount) {
        if (amount <= Integer.MAX_VALUE) {
            return CountFormatter.formatApprox(null, (int) amount);
        }
        if (amount >= 1_000_000_000_000_000L) {
            return String.format("%.1fP", amount / 1e15);
        }
        if (amount >= 1_000_000_000_000L) {
            return String.format("%.1fT", amount / 1e12);
        }
        return String.format("%.1fG", amount / 1e9);
    }

    private StorageDrawersContract() {
    }
}
