package dev.drimoz.immaterialstorage.chemical;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.client.ChemicalClient;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.gametest.ChemicalGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Chemical Drawers: everything this mod registers only when Mekanism is installed, in 1x1, 1x2
 * and 2x2. <b>Mekanism only: reached from {@code ImmaterialStorage} through {@code Mods.mekanism()}.</b>
 */
public final class ChemicalContent {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ImmaterialStorage.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ImmaterialStorage.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmaterialStorage.MOD_ID);

    public static final DeferredBlock<ChemicalDrawerBlock> CHEMICAL_DRAWER = drawer(Compartments.ONE);
    public static final DeferredBlock<ChemicalDrawerBlock> CHEMICAL_DRAWER_2 = drawer(Compartments.TWO);
    public static final DeferredBlock<ChemicalDrawerBlock> CHEMICAL_DRAWER_4 = drawer(Compartments.FOUR);
    public static final List<DeferredBlock<ChemicalDrawerBlock>> DRAWERS = List.of(CHEMICAL_DRAWER, CHEMICAL_DRAWER_2, CHEMICAL_DRAWER_4);
    public static final List<DeferredItem<BlockItem>> DRAWER_ITEMS = DRAWERS.stream().map(ITEMS::registerSimpleBlockItem).toList();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalDrawerBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("chemical_drawer", () -> BlockEntityType.Builder.of(ChemicalDrawerBlockEntity::new,
                    CHEMICAL_DRAWER.get(), CHEMICAL_DRAWER_2.get(), CHEMICAL_DRAWER_4.get()).build(null));

    private static DeferredBlock<ChemicalDrawerBlock> drawer(Compartments compartments) {
        return BLOCKS.register("chemical_drawer" + compartments.suffix(),
                () -> new ChemicalDrawerBlock(BlockBehaviour.Properties.of().strength(3.0f), compartments));
    }

    private ChemicalContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ChemicalContent::registerCapabilities);
        modBus.addListener(ChemicalContent::addToCreativeTab);
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(ChemicalGameTests.class));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ChemicalClient.register(modBus);
        }
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(StorageDrawersContract.CREATIVE_TAB)) {
            DRAWER_ITEMS.forEach(event::accept);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(StorageDrawersContract.DRAWER_GROUP, BLOCK_ENTITY.get(), (be, context) -> be.node());
        event.registerBlockEntity(ChemicalCapabilities.BLOCK, BLOCK_ENTITY.get(), (be, side) -> be.chemicals());
        for (var name : List.of(StorageDrawersContract.CONTROLLER_BLOCK_ENTITY, StorageDrawersContract.CONTROLLER_IO_BLOCK_ENTITY)) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(name);
            if (type != null) {
                event.registerBlockEntity(ChemicalCapabilities.BLOCK, (BlockEntityType<BlockEntity>) type,
                        (be, side) -> be instanceof IControlGroup group ? ControllerChemicalHandler.of(group) : null);
            }
        }
    }
}
