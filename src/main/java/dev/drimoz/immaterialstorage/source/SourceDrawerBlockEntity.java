package dev.drimoz.immaterialstorage.source;

import com.hollingsworth.arsnouveau.api.source.SourceManager;
import dev.drimoz.immaterialstorage.ISConfig;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Source Drawer: Ars Nouveau's Source, sized by Storage Drawers' upgrades. <b>Ars Nouveau only: see
 * {@code compat.Mods}.</b>
 *
 * <p>Ars reaches it two ways: its Source capability ({@link SourceCapabilities}) for relays and
 * turrets, and its {@code SourceManager}, joined on load, for what pulls Source from nearby (the
 * enchanting apparatus, imbuement) and the sourcelinks that fill it.
 *
 * <p>Capacity = the configured base ({@link ISConfig}, 32,000 Source by default) x the storage
 * multiplier, within Ars's int API.
 */
public class SourceDrawerBlockEntity extends DrawerBlockEntity {

    private static final String SOURCE_TAG = "Source";

    private final BigSourceStorage source = new BigSourceStorage(capacityFor(upgrades())) {
        @Override
        public void onChange() {
            contentChanged();
        }

        @Override
        public boolean isDrawerVoid() {
            return upgrades().hasVoid();
        }

        @Override
        public boolean isDrawerCreative() {
            return upgrades().hasCreativeVending();
        }
    };

    public SourceDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(SourceContent.BLOCK_ENTITY.get(), pos, state);
    }

    /** Also what the drawer answers to Ars's Source capability, on every side. */
    public BigSourceStorage source() {
        return source;
    }

    static int capacityFor(DrawerUpgrades upgrades) {
        if (upgrades.hasCreativeStorage()) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) ISConfig.sourceBaseCapacity() * upgrades.storageMultiplier());
    }

    /** Joins Ars's SourceManager, server side only: Ars cleans that registry from the server tick alone. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            SourceManager.INSTANCE.addInterface(level, new SourceDrawerProvider(this));
        }
    }

    @Override
    protected void applyUpgrades() {
        // Never below what it holds: a pack lowering the base size must not delete Source on load.
        source.setCapacity(Math.max(capacityFor(upgrades()), source.getStoredRaw()));
    }

    @Override
    protected boolean contentFits(DrawerUpgrades upgrades) {
        return capacityFor(upgrades) >= source.getStoredRaw();
    }

    @Override
    public boolean isContentEmpty() {
        return source.getStoredRaw() == 0;
    }

    @Override
    protected void saveContent(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(SOURCE_TAG, source.serializeNBT(registries));
    }

    @Override
    protected void loadContent(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(SOURCE_TAG)) {
            source.deserializeNBT(registries, tag.getCompound(SOURCE_TAG));
        }
    }

    /** The tooltip line for a dropped drawer's saved data, or null when it holds no Source. */
    @Nullable
    public static Component describeStored(CompoundTag saved, HolderLookup.Provider registries) {
        if (!saved.contains(SOURCE_TAG)) {
            return null;
        }
        BigSourceStorage stored = new BigSourceStorage(0) {
            @Override
            public void onChange() {
            }

            @Override
            public boolean isDrawerVoid() {
                return false;
            }

            @Override
            public boolean isDrawerCreative() {
                return false;
            }
        };
        stored.deserializeNBT(registries, saved.getCompound(SOURCE_TAG));
        return stored.getStoredRaw() <= 0 ? null : describe(stored.getStoredRaw(), stored.getCapacityRaw());
    }

    /** "stored / capacity Source", exact: the item tooltip and the upgrade screen both say it this way. */
    public static Component describe(int stored, int capacity) {
        return Component.translatable("tooltip.immaterialstorage_sd.source",
                String.format("%,d", stored), String.format("%,d", capacity));
    }
}
