package dev.drimoz.immaterialstorage.network;

import com.jaquadro.minecraft.storagedrawers.api.storage.Drawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Learns which Storage Drawers controller counts this block, without asking the controller.
 *
 * <p>We expose {@link #node()} as Storage Drawers' {@code DRAWER_GROUP}. The controller, finding a
 * group that is also {@link INetworked}, calls {@link Node#softBindControlGroup} on it. That call
 * is our only way in: the controller's list of positions is private.
 *
 * <p>Storage Drawers never unbinds a node when the controller is broken (the call is commented out
 * in {@code BlockEntityController.setRemoved}). So every read re-checks the binding with
 * {@code isSoftBindingValid}, which Storage Drawers' own drawers do too.
 *
 * <p>Not this class's job: storing anything, or aggregating anything on the controller.
 */
public class NetworkNodeBlockEntity extends BlockEntity {

    private final Node node = new Node();

    public NetworkNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ImmaterialStorage.NETWORK_NODE_BLOCK_ENTITY.get(), pos, state);
    }

    /** What we answer to Storage Drawers' {@code DRAWER_GROUP}. Same instance every time. */
    public Node node() {
        return node;
    }

    /** The controller that currently counts this block, or {@code null}. */
    @Nullable
    public IControlGroup controller() {
        Set<IControlGroup> bound = node.getSoftBoundControlGroups();
        return bound.isEmpty() ? null : bound.iterator().next();
    }

    /**
     * A drawer group with no drawers. Non-null on purpose: the controller only keeps a soft binding
     * valid while its record holds this exact group ({@code record.storage == node}).
     */
    public final class Node implements IDrawerGroup, INetworked {

        // A plain set, one entry in practice (two controllers only share a node when their
        // ranges overlap); pruned on every read.
        private final Set<IControlGroup> softBound = new HashSet<>();

        @Override
        public int getDrawerCount() {
            return 0;
        }

        @Override
        public @NotNull IDrawer getDrawer(int slot) {
            return Drawers.DISABLED;
        }

        @Override
        public int[] getAccessibleDrawerSlots() {
            return new int[0];
        }

        @Override
        public boolean isGroupValid() {
            return !isRemoved();
        }

        @Override
        public void softBindControlGroup(IControlGroup group) {
            softBound.add(group);
        }

        @Override
        public Set<IControlGroup> getSoftBoundControlGroups() {
            softBound.removeIf(group -> !group.isSoftBindingValid(getBlockPos(), this));
            return softBound;
        }
    }
}
