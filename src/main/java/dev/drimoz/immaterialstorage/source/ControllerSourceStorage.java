package dev.drimoz.immaterialstorage.source;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import dev.drimoz.immaterialstorage.network.ControllerNodes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Storage Drawers controller's Source: the sum of every Source Drawer on its network, for a relay
 * pointed at the controller. Also what its Controller Slaves expose.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 *
 * <p>The Source counterpart of {@code ControllerEnergyStorage}, and built the same way, for the same
 * reasons - one handler per controller, nothing cached across calls, the network read from
 * {@code ControllerNodes}. Fills and drains in network order, spilling from one drawer to the
 * next.
 *
 * <p>The controller is <em>not</em> registered with Ars's {@code SourceManager}. Each drawer already
 * is, and a consumer near both would count the same Source twice.
 */
public final class ControllerSourceStorage implements ISourceCap {

    private static final Map<IControlGroup, ControllerSourceStorage> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final IControlGroup controller;

    private ControllerSourceStorage(IControlGroup controller) {
        this.controller = controller;
    }

    public static ControllerSourceStorage of(IControlGroup controller) {
        return CACHE.computeIfAbsent(controller, ControllerSourceStorage::new);
    }

    private List<BigSourceStorage> drawers() {
        return ControllerNodes.ofNetwork(controller, SourceDrawerBlockEntity.class).stream()
                .map(SourceDrawerBlockEntity::source).toList();
    }

    @Override
    public int receiveSource(int toReceive, boolean simulate) {
        int accepted = 0;
        // Storage Drawers' Suspend Key holds a drawer back from filling.
        for (BigSourceStorage drawer : ControllerNodes.ofNetwork(controller, SourceDrawerBlockEntity.class).stream()
                .filter(node -> !node.attributes().isSuspended()).map(SourceDrawerBlockEntity::source).toList()) {
            if (accepted >= toReceive) {
                break;
            }
            accepted += drawer.receiveSource(toReceive - accepted, simulate);
        }
        return accepted;
    }

    @Override
    public int extractSource(int toExtract, boolean simulate) {
        int extracted = 0;
        for (BigSourceStorage drawer : drawers()) {
            if (extracted >= toExtract) {
                break;
            }
            extracted += drawer.extractSource(toExtract - extracted, simulate);
        }
        return extracted;
    }

    /** Summed in a long and saturated once at the end, like the energy controller's. */
    @Override
    public int getSource() {
        long total = 0;
        for (BigSourceStorage drawer : drawers()) {
            total += drawer.getSource();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    @Override
    public int getSourceCapacity() {
        long total = 0;
        for (BigSourceStorage drawer : drawers()) {
            total += drawer.getSourceCapacity();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    @Override
    public boolean canAcceptSource(int amount) {
        return receiveSource(amount, true) > 0;
    }

    @Override
    public boolean canProvideSource(int amount) {
        return extractSource(amount, true) > 0;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public int getMaxExtract() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxReceive() {
        return Integer.MAX_VALUE;
    }

    /** A network has no single amount to force; the drawers own their contents. */
    @Override
    public void setSource(int source) {
    }

    @Override
    public void setMaxSource(int max) {
    }
}
