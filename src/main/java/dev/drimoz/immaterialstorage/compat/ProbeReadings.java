package dev.drimoz.immaterialstorage.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * What the probe mods (Jade, The One Probe) show of the contents they cannot read on their own: a
 * line per tank, with its amount and the colour of what is in it.
 *
 * <p>The optional modules (chemicals, Source) register their readers here, so the probe plugins never
 * name a Mekanism or Ars Nouveau class and load whatever is installed. Energy and fluids are not
 * here: the probes read those themselves (energy through {@link EnergyReading}).
 *
 * <p>A reader says whether Jade shows it too; every reader does today (on our blocks Jade's own
 * chemical bars, drawn by Mekanism, are replaced by ours: see the Jade provider).
 */
public final class ProbeReadings {

    /** One tank: its name, how much it holds of how much, in what unit, and a bar colour (ARGB). */
    public record Line(Component label, long stored, long capacity, String unit, int color) {
    }

    private record Reader(Function<BlockEntity, List<Line>> read, boolean forJade) {
    }

    private static final List<Reader> READERS = new CopyOnWriteArrayList<>();

    private ProbeReadings() {
    }

    /** Called once per optional module at mod construction. */
    public static void register(Function<BlockEntity, List<Line>> reader, boolean forJade) {
        READERS.add(new Reader(reader, forJade));
    }

    /** Every line the installed modules have for this block entity, for Jade or for The One Probe. */
    public static List<Line> of(BlockEntity blockEntity, boolean forJade) {
        List<Line> lines = new ArrayList<>();
        if (blockEntity == null) {
            return lines;
        }
        for (Reader reader : READERS) {
            if (!forJade || reader.forJade()) {
                lines.addAll(reader.read().apply(blockEntity));
            }
        }
        return lines;
    }
}
