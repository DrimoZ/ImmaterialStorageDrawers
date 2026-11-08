package dev.drimoz.immaterialstorage.compat.jade;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.compat.EnergyReading;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * Puts the real amount of energy into the energy bar Jade already draws, for Energy Drawers and for
 * the Storage Drawers controllers and slaves that link them.
 *
 * <p>Jade reads the energy capability on its own, but that is an int and stops at 2.1 billion FE.
 * Its {@code registerEnergyStorage} hook feeds the same bar from longs; being registered for these
 * classes, it replaces Jade's own reading there rather than adding a second bar. Fluids need nothing:
 * Jade reads the fluid capability, which is enough.
 *
 * <p>Taken from Immaterial Drawers' Jade plugin.
 */
@WailaPlugin
public class ISJadePlugin implements IWailaPlugin {

    /** Jade keys providers by this and lets players switch them off; keep it stable. */
    public static final ResourceLocation ENERGY = ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "energy");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(EnergyExtension.INSTANCE, EnergyDrawerBlockEntity.class);
        StorageDrawersContract.CONTROLLER_CLASSES.forEach(
                type -> registration.registerEnergyStorage(EnergyExtension.INSTANCE, type));
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(EnergyExtension.INSTANCE);
    }

    /** Server and client halves of one provider: Jade matches them by {@link #getUid()}. */
    public enum EnergyExtension
            implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, EnergyView> {
        INSTANCE;

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (!(accessor instanceof BlockAccessor block)) {
                return List.of();
            }
            EnergyReading reading = EnergyReading.of(block.getBlockEntity());
            return reading == null
                    ? List.of()
                    : List.of(new ViewGroup<>(List.of(EnergyView.of(reading.stored(), reading.capacity()))));
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
            return ClientViewGroup.map(groups, tag -> EnergyView.read(tag, "FE"), null);
        }

        @Override
        public ResourceLocation getUid() {
            return ENERGY;
        }
    }
}
