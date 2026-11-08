package dev.drimoz.immaterialstorage.compat.top;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.compat.EnergyReading;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.function.Function;

/**
 * The One Probe's twin of the Jade plugin: TOP's own RF bar, fed the energy in full, for Energy
 * Drawers and the Storage Drawers controllers and slaves that link them.
 *
 * <p>Only ever loaded when TOP is installed (see the mod entry point). TOP asks every provider and
 * keeps all their lines, so its default RF readout, read from the int capability and wrong past
 * 2.1 billion FE, is switched off on these blocks through {@code IProbeConfigProvider}.
 *
 * <p>Taken from Immaterial Drawers' {@code EnergyProbeProvider}.
 */
public final class TopCompat implements IProbeInfoProvider, IProbeConfigProvider {

    /** {@code IProbeConfig}'s "do not show RF at all" mode. */
    private static final int RF_HIDDEN = 0;

    private static final Function<ITheOneProbe, Void> REGISTER = probe -> {
        TopCompat provider = new TopCompat();
        probe.registerProvider(provider);
        probe.registerProbeConfigProvider(provider);
        return null;
    };

    /** Hands the provider to TOP through its published InterModComms entry point. */
    public static void register(InterModEnqueueEvent event) {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", () -> REGISTER);
    }

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "energy");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level,
                             BlockState state, IProbeHitData data) {
        EnergyReading reading = EnergyReading.of(level.getBlockEntity(data.getPos()));
        if (reading == null) {
            return;
        }
        // TOP's own bar and the player's configured colours and number format: only the numbers are ours.
        probeInfo.progress(reading.stored(), reading.capacity(), probeInfo.defaultProgressStyle()
                .suffix("FE")
                .filledColor(Config.rfbarFilledColor)
                .alternateFilledColor(Config.rfbarAlternateFilledColor)
                .borderColor(Config.rfbarBorderColor)
                .numberFormat(Config.rfFormat.get()));
    }

    /**
     * Hides TOP's own RF readout wherever ours applies, and on every controller: a controller always
     * exposes an energy capability (0 / 0 without Energy Drawers), and an empty bar on each one would
     * be noise.
     */
    @Override
    public void getProbeConfig(IProbeConfig config, Player player, Level level, BlockState state, IProbeHitData data) {
        BlockEntity blockEntity = level.getBlockEntity(data.getPos());
        if (blockEntity instanceof EnergyDrawerBlockEntity || blockEntity instanceof IControlGroup) {
            config.setRFMode(RF_HIDDEN);
        }
    }

    @Override
    public void getProbeConfig(IProbeConfig config, Player player, Level level, Entity entity, IProbeHitEntityData data) {
        // Entities hold no drawers.
    }
}
