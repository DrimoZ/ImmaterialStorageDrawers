package dev.drimoz.immaterialstorage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import com.mojang.logging.LogUtils;
import dev.drimoz.immaterialstorage.compat.top.TopCompat;
import dev.drimoz.immaterialstorage.drawer.DrawerMenu;
import dev.drimoz.immaterialstorage.energy.ControllerEnergyStorage;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlock;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.fluid.ControllerFluidHandler;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlock;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.List;

/**
 * Mod entry point and, for now, the whole registry.
 *
 * <p>Two drawers, Energy and Fluid, with their items, listed in Storage Drawers' tab.
 */
@Mod(ImmaterialStorage.MOD_ID)
public final class ImmaterialStorage {

    public static final String MOD_ID = "immaterialstorage_sd";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<EnergyDrawerBlock> ENERGY_DRAWER = BLOCKS.register("energy_drawer",
            () -> new EnergyDrawerBlock(BlockBehaviour.Properties.of().strength(3.0f)));

    public static final DeferredBlock<FluidDrawerBlock> FLUID_DRAWER = BLOCKS.register("fluid_drawer",
            () -> new FluidDrawerBlock(BlockBehaviour.Properties.of().strength(3.0f)));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<BlockItem> ENERGY_DRAWER_ITEM = ITEMS.registerSimpleBlockItem(ENERGY_DRAWER);
    public static final DeferredItem<BlockItem> FLUID_DRAWER_ITEM = ITEMS.registerSimpleBlockItem(FLUID_DRAWER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyDrawerBlockEntity>>
            ENERGY_DRAWER_BLOCK_ENTITY = BLOCK_ENTITIES.register("energy_drawer",
            () -> BlockEntityType.Builder.of(EnergyDrawerBlockEntity::new, ENERGY_DRAWER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidDrawerBlockEntity>>
            FLUID_DRAWER_BLOCK_ENTITY = BLOCK_ENTITIES.register("fluid_drawer",
            () -> BlockEntityType.Builder.of(FluidDrawerBlockEntity::new, FLUID_DRAWER.get()).build(null));

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    /** One upgrade screen for every drawer; the position of the drawer travels with it. */
    public static final DeferredHolder<MenuType<?>, MenuType<DrawerMenu>> DRAWER_MENU =
            MENUS.register("drawer", () -> IMenuTypeExtension.create(DrawerMenu::fromNetwork));

    public ImmaterialStorage(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ISConfig.SPEC);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(ImmaterialStorage::registerCapabilities);
        modBus.addListener(ImmaterialStorage::addToCreativeTab);
        if (ModList.get().isLoaded("theoneprobe")) {
            // Only reached with TOP installed, so its classes are never loaded without it.
            modBus.addListener(TopCompat::register);
        }
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(StorageDrawersContract.CREATIVE_TAB)) {
            event.accept(ENERGY_DRAWER_ITEM);
            event.accept(FLUID_DRAWER_ITEM);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(StorageDrawersContract.DRAWER_GROUP, ENERGY_DRAWER_BLOCK_ENTITY.get(),
                (be, context) -> be.node());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_DRAWER_BLOCK_ENTITY.get(),
                (be, side) -> be.energy());
        event.registerBlockEntity(StorageDrawersContract.DRAWER_GROUP, FLUID_DRAWER_BLOCK_ENTITY.get(),
                (be, context) -> be.node());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_DRAWER_BLOCK_ENTITY.get(),
                (be, side) -> be.tank());

        for (var name : List.of(StorageDrawersContract.CONTROLLER_BLOCK_ENTITY, StorageDrawersContract.CONTROLLER_IO_BLOCK_ENTITY)) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(name);
            if (type == null) {
                // Logged, not thrown: the drawers still work alone, only the network total is lost.
                LOGGER.error("Storage Drawers has no block entity type {}: it will not expose energy or fluids", name);
                continue;
            }
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, (BlockEntityType<BlockEntity>) type,
                    (be, side) -> be instanceof IControlGroup group ? ControllerEnergyStorage.of(group) : null);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, (BlockEntityType<BlockEntity>) type,
                    (be, side) -> be instanceof IControlGroup group ? ControllerFluidHandler.of(group) : null);
        }
    }
}
