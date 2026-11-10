package dev.drimoz.immaterialstorage;

import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlock;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedMaterials;
import com.jaquadro.minecraft.storagedrawers.api.security.ISecurityProvider;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.IProtectable;
import com.jaquadro.minecraft.storagedrawers.core.ModSecurity;
import com.jaquadro.minecraft.storagedrawers.item.ItemKeyring;
import com.jaquadro.minecraft.storagedrawers.item.ItemPersonalKey;
import com.jaquadro.minecraft.storagedrawers.security.SecurityManager;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.MaterialData;
import com.jaquadro.minecraft.storagedrawers.block.tile.util.FrameHelper;
import com.jaquadro.minecraft.storagedrawers.components.item.FrameData;
import com.jaquadro.minecraft.storagedrawers.core.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

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
     * Storage Drawers' drawer screen for 1, 2 or 4 drawers, which our upgrade screen draws on so it is
     * the screen players know. Its slot positions are copied in Compartments and DrawerMenu.
     */
    public static ResourceLocation drawerScreenTexture(int drawers) {
        return ResourceLocation.fromNamespaceAndPath("storagedrawers", "textures/gui/drawers_" + drawers + ".png");
    }

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

    // --- Framing: Storage Drawers' own frame data, so its framing table frames and unframes our
    // drawers exactly like its own. The holder is typed by their public IFramedMaterials; its
    // internal implementation (MaterialData, FrameData, the frame_data component) stays here. ---

    /** An empty frame, in Storage Drawers' own holder. */
    public static IFramedMaterials newFrame() {
        return new MaterialData();
    }

    public static boolean isFramed(IFramedMaterials frame) {
        return !((MaterialData) frame).isEmpty();
    }

    /** The framed drawer the framing table hands out, built by Storage Drawers' own helper. */
    public static ItemStack makeFramedItem(IFramedBlock result, ItemStack source, ItemStack side, ItemStack trim, ItemStack front) {
        return FrameHelper.makeFramedItem(result, source, side, trim, front);
    }

    /** The frame a framing table wrote on a drawer item, replacing the holder's; empty when none. */
    public static void readFrame(IFramedMaterials frame, ItemStack stack) {
        ((MaterialData) frame).read(stack);
    }

    /** Writes the frame on an item as Storage Drawers' framed blocks drop it; nothing when unframed. */
    public static void writeFrame(IFramedMaterials frame, ItemStack stack) {
        if (isFramed(frame)) {
            stack.set(ModDataComponents.FRAME_DATA.get(), new FrameData((MaterialData) frame));
        }
    }

    public static CompoundTag saveFrame(IFramedMaterials frame, HolderLookup.Provider registries) {
        return ((MaterialData) frame).write(registries, new CompoundTag());
    }

    /** Replaces the holder's frame; an empty tag clears it. */
    public static void loadFrame(IFramedMaterials frame, HolderLookup.Provider registries, CompoundTag tag) {
        ((MaterialData) frame).read(registries, tag);
    }

    // --- Keys and security ---

    /**
     * Storage Drawers' {@code DRAWER_ATTRIBUTES} capability, recreated by name like {@link #DRAWER_GROUP}:
     * what its keys (lock, quantify, shroud, priority, suspend) ask a block for. Same context class.
     */
    public static final BlockCapability<IDrawerAttributes, Void> DRAWER_ATTRIBUTES = BlockCapability.create(
            ResourceLocation.fromNamespaceAndPath("storagedrawers", "drawer_attributes"),
            IDrawerAttributes.class, Void.TYPE);

    /** Storage Drawers' own access rule (owner, or the owner's team for the FTB and CoFH keys). */
    public static boolean hasAccess(Player player, IProtectable target) {
        return SecurityManager.hasAccess(player, target);
    }

    public static boolean hasOwnership(Player player, IProtectable target) {
        return SecurityManager.hasOwnership(player.getGameProfile(), target);
    }

    /**
     * The security provider of the Personal Key in hand, directly or on a keyring: "" for the plain key,
     * "unlock" for the admin key; empty when it is no Personal Key, or Personal Keys are disabled.
     */
    public static Optional<String> personalKey(ItemStack stack) {
        ItemStack key = stack.getItem() instanceof ItemKeyring keyring ? keyring.getKey() : stack;
        if (key.getItem() instanceof ItemPersonalKey personal && ModCommonConfig.INSTANCE.TOOLS.personalKey.enable.get()) {
            String provider = personal.getSecurityProviderKey();
            return Optional.of(provider == null ? "" : provider);
        }
        return Optional.empty();
    }

    @Nullable
    public static ISecurityProvider securityProvider(String id) {
        return id == null || id.isEmpty() ? null : ModSecurity.registry.getProvider(id);
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
