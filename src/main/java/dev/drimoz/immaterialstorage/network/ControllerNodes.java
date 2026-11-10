package dev.drimoz.immaterialstorage.network;

import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Which of our nodes each Storage Drawers controller counts: the controller to nodes direction,
 * which Storage Drawers keeps private.
 *
 * <p>Filled by {@link NetworkNodeBlockEntity} when a controller soft-binds it, emptied when the
 * node is removed or unloaded. Every read re-checks {@code isSoftBindingValid}, because Storage
 * Drawers never tells a node it was dropped: a node moved out of range, or a controller that
 * rebuilt without it, is only noticed here.
 *
 * <p>Weak keys: this map must not keep an unloaded controller in memory. Server thread only, like
 * the controller's own rebuild and the capability lookups that read it.
 */
public final class ControllerNodes {

    private static final Map<IControlGroup, Set<NetworkNodeBlockEntity>> NODES = new WeakHashMap<>();

    private ControllerNodes() {
    }

    static void add(IControlGroup controller, NetworkNodeBlockEntity node) {
        NODES.computeIfAbsent(controller, c -> new LinkedHashSet<>()).add(node);
    }

    static void remove(NetworkNodeBlockEntity node) {
        for (Set<NetworkNodeBlockEntity> nodes : NODES.values()) {
            nodes.remove(node);
        }
    }

    /**
     * The live nodes of the network a controller or Controller Slave answers for. A slave answers
     * for the controller it is bound to; a controller is bound to nothing and answers for itself.
     * Resolved on every call: a slave can be rebound at any time.
     */
    public static <T extends NetworkNodeBlockEntity> List<T> ofNetwork(IControlGroup group, Class<T> kind) {
        IControlGroup bound = group.getBoundControlGroup();
        return of(bound != null ? bound : group, kind);
    }

    /** The controller's live nodes of the given kind, in the order they were linked. */
    public static <T extends NetworkNodeBlockEntity> List<T> of(IControlGroup controller, Class<T> kind) {
        Set<NetworkNodeBlockEntity> nodes = NODES.get(controller);
        if (nodes == null) {
            return List.of();
        }
        List<T> live = new ArrayList<>(nodes.size());
        for (Iterator<NetworkNodeBlockEntity> it = nodes.iterator(); it.hasNext(); ) {
            NetworkNodeBlockEntity node = it.next();
            if (node.isRemoved() || !controller.isSoftBindingValid(node.getBlockPos(), node.node())) {
                it.remove();
            } else if (kind.isInstance(node)) {
                live.add(kind.cast(node));
            }
        }
        // Storage Drawers' Priority Key: higher first; stable, so linking order breaks ties.
        live.sort(java.util.Comparator.comparingInt((T node) -> node instanceof DrawerBlockEntity drawer
                ? -drawer.attributes().getPriority() : 0));
        return live;
    }
}
