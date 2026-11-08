package dev.drimoz.immaterialstorage;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's settings: only what Storage Drawers' own config does not already cover. The upgrade
 * multipliers stay in Storage Drawers' config, so one pack setting sizes every drawer alike.
 *
 * <p>Common, like Storage Drawers' multipliers. A new value applies to a drawer the next time it loads
 * or changes upgrades; a drawer holding more than its new size keeps it all (see the drawers'
 * {@code applyUpgrades}).
 */
public final class ISConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.LongValue ENERGY_BASE = BUILDER
            .comment("FE an Energy Drawer holds with no storage upgrade.",
                    "Storage upgrades multiply it by Storage Drawers' own multipliers.")
            .defineInRange("energyDrawerBaseCapacity", 500_000L, 1L, Long.MAX_VALUE);

    private static final ModConfigSpec.IntValue FLUID_BASE = BUILDER
            .comment("mB a Fluid Drawer holds with no storage upgrade.",
                    "Storage upgrades multiply it by Storage Drawers' own multipliers; the fluid API stops at 2,147,483,647 mB.")
            .defineInRange("fluidDrawerBaseCapacity", 32_000, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.LongValue CHEMICAL_BASE = BUILDER
            .comment("mB a Chemical Drawer holds with no storage upgrade (Mekanism installed).",
                    "Storage upgrades multiply it by Storage Drawers' own multipliers; compartments share it.")
            .defineInRange("chemicalDrawerBaseCapacity", 32_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ISConfig() {
    }

    public static long energyBaseCapacity() {
        return ENERGY_BASE.getAsLong();
    }

    public static int fluidBaseCapacity() {
        return FLUID_BASE.getAsInt();
    }

    public static long chemicalBaseCapacity() {
        return CHEMICAL_BASE.getAsLong();
    }
}
