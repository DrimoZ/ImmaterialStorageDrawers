package dev.drimoz.immaterialstorage.source;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import dev.drimoz.immaterialstorage.compat.Mods;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Ars Nouveau's Source capability, reached through its API only. <b>Ars Nouveau only: see
 * {@code compat.Mods}.</b>
 *
 * <p>Ars keeps the capability object in its setup code, not its API. What is stable is the name,
 * {@code ars_nouveau:source}, typed on the API's {@link ISourceCap}; NeoForge interns capabilities by
 * name, so this is Ars's own object. The game test {@code weSpeakArsOwnCapability} holds that true.
 */
public final class SourceCapabilities {

    public static final BlockCapability<ISourceCap, @Nullable Direction> BLOCK = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(Mods.ARS_NOUVEAU, "source"), ISourceCap.class);

    private SourceCapabilities() {
    }
}
