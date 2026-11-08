package dev.drimoz.immaterialstorage.chemical;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Mekanism's chemical capabilities, reached through its API only. <b>Mekanism only: see
 * {@code compat.Mods}.</b>
 *
 * <p>Mekanism keeps its capability objects in its implementation, not its API. What is stable is the
 * name, {@code mekanism:chemical_handler}, typed on the API's {@link IChemicalHandler}; NeoForge
 * interns capabilities by name, so creating ours with that name returns Mekanism's own object. The
 * game test {@code weSpeakMekanismsOwnCapability} holds that true. Immaterial Drawers does the same.
 */
public final class ChemicalCapabilities {

    private static final ResourceLocation NAME =
            ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "chemical_handler");

    /** What pressurized tubes, machines and Mekanism's own tanks look for on a block. */
    public static final BlockCapability<IChemicalHandler, @Nullable Direction> BLOCK =
            BlockCapability.createSided(NAME, IChemicalHandler.class);

    /** What a chemical tank item exposes: how a player fills or empties a drawer by hand. */
    public static final ItemCapability<IChemicalHandler, @Nullable Void> ITEM =
            ItemCapability.createVoid(NAME, IChemicalHandler.class);

    /** Chemicals a drawer refuses. Empty by default: it is there for pack authors. */
    public static final TagKey<Chemical> DENYLIST = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME,
            ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "chemical_drawer_denylist"));

    private ChemicalCapabilities() {
    }
}
