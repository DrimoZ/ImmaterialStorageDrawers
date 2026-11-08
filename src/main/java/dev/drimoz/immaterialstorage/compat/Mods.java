package dev.drimoz.immaterialstorage.compat;

import net.neoforged.fml.ModList;

/**
 * Which optional mods are installed, and the one place allowed to ask.
 *
 * <p>The rule this class exists for: code that touches an optional mod's classes only runs after one
 * of these methods said the mod is there. The JVM resolves a class the first time code using it
 * runs, and a missing class then is a {@code NoClassDefFoundError} that takes the whole game down, in
 * a pack that never asked for the feature. So everything using Mekanism or Ars Nouveau lives in classes only ever
 * reached through {@code if (Mods.mekanism())} or {@code if (Mods.arsNouveau())}, from classes that do not use it themselves; and this
 * class stays free of every optional import.
 */
public final class Mods {

    public static final String MEKANISM = "mekanism";
    public static final String ARS_NOUVEAU = "ars_nouveau";

    private Mods() {
    }

    /** Whether Mekanism is installed: the Chemical Drawers. Safe from the mod constructor onwards. */
    public static boolean mekanism() {
        return ModList.get().isLoaded(MEKANISM);
    }

    /** Whether Ars Nouveau is installed: the Source Drawer. Same contract as {@link #mekanism()}. */
    public static boolean arsNouveau() {
        return ModList.get().isLoaded(ARS_NOUVEAU);
    }
}
