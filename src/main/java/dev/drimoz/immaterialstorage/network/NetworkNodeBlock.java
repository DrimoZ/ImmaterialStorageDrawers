package dev.drimoz.immaterialstorage.network;

import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block the Storage Drawers controller walks through and links to, storing no items.
 *
 * <p>{@link INetworked} must be on the {@link Block}: that is what the controller's network search
 * tests ({@code BlockEntityController.populateRoot}). The block entity does the linking.
 * What the block stores (energy, fluid) is not this class's business.
 */
public class NetworkNodeBlock extends Block implements EntityBlock, INetworked {

    public NetworkNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkNodeBlockEntity(pos, state);
    }
}
