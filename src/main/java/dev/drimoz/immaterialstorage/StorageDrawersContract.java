package dev.drimoz.immaterialstorage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import net.minecraft.resources.ResourceLocation;
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

    private StorageDrawersContract() {
    }
}
