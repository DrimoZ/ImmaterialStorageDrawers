package dev.drimoz.immaterialstorage.source;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.client.SourceClient;
import dev.drimoz.immaterialstorage.gametest.SourceGameTests;
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
 * The Source Drawer: everything this mod registers only when Ars Nouveau is installed. <b>Ars Nouveau
 * only: reached from {@code ImmaterialStorage} through {@code Mods.arsNouveau()}.</b>
 */
public final class SourceContent {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ImmaterialStorage.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ImmaterialStorage.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmaterialStorage.MOD_ID);

    public static final DeferredBlock<SourceDrawerBlock> SOURCE_DRAWER = BLOCKS.register("source_drawer",
            () -> new SourceDrawerBlock(BlockBehaviour.Properties.of().strength(3.0f)));
    public static final DeferredItem<BlockItem> SOURCE_DRAWER_ITEM = ITEMS.registerSimpleBlockItem(SOURCE_DRAWER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SourceDrawerBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("source_drawer",
                    () -> BlockEntityType.Builder.of(SourceDrawerBlockEntity::new, SOURCE_DRAWER.get()).build(null));

    private SourceContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(SourceContent::registerCapabilities);
        modBus.addListener(SourceContent::addToCreativeTab);
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(SourceGameTests.class));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SourceClient.register(modBus);
        }
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(StorageDrawersContract.CREATIVE_TAB)) {
            event.accept(SOURCE_DRAWER_ITEM);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(StorageDrawersContract.DRAWER_GROUP, BLOCK_ENTITY.get(), (be, context) -> be.node());
        event.registerBlockEntity(SourceCapabilities.BLOCK, BLOCK_ENTITY.get(), (be, side) -> be.source());
        for (var name : List.of(StorageDrawersContract.CONTROLLER_BLOCK_ENTITY, StorageDrawersContract.CONTROLLER_IO_BLOCK_ENTITY)) {
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(name);
            if (type != null) {
                event.registerBlockEntity(SourceCapabilities.BLOCK, (BlockEntityType<BlockEntity>) type,
                        (be, side) -> be instanceof IControlGroup group ? ControllerSourceStorage.of(group) : null);
            }
        }
    }
}
