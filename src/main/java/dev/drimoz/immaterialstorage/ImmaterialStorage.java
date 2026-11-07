package dev.drimoz.immaterialstorage;

import dev.drimoz.immaterialstorage.network.NetworkNodeBlock;
import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mod entry point and, for now, the whole registry.
 *
 * <p>Spike state (SPIKE.md): one test block, {@code network_node}, with no item, model
 * or recipe. It exists only for the game tests to prove that the controller links us.
 */
@Mod(ImmaterialStorage.MOD_ID)
public final class ImmaterialStorage {

    public static final String MOD_ID = "immaterialstorage_sd";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<NetworkNodeBlock> NETWORK_NODE = BLOCKS.register("network_node",
            () -> new NetworkNodeBlock(BlockBehaviour.Properties.of().strength(2.0f)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkNodeBlockEntity>>
            NETWORK_NODE_BLOCK_ENTITY = BLOCK_ENTITIES.register("network_node",
            () -> BlockEntityType.Builder.of(NetworkNodeBlockEntity::new, NETWORK_NODE.get()).build(null));

    public ImmaterialStorage(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ImmaterialStorage::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(StorageDrawersContract.DRAWER_GROUP, NETWORK_NODE_BLOCK_ENTITY.get(),
                (be, context) -> be.node());
    }
}
