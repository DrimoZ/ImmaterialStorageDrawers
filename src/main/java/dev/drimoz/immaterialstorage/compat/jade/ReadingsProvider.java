package dev.drimoz.immaterialstorage.compat.jade;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.compat.ProbeReadings;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/**
 * Jade's bars for the contents it does not read on its own ({@link ProbeReadings}): read on the
 * server, sent with Jade's own block data, drawn on the client as Jade draws its energy bar.
 *
 * <p>On our blocks these replace Mekanism's chemical bars, which it draws for any block with its
 * chemical capability: in Jade 15.10 their text is lost on pale chemicals (white on a white bar) and
 * their first label overlaps the title. This provider runs after Mekanism's: on the server it drops
 * the data Mekanism sends for them, on the client any bar left. The
 * fill colour is darkened from the content's tint, so white text reads on any chemical.
 */
public enum ReadingsProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "contents");
    private static final String LINES = "ImmaterialStorageLines";
    /** Mekanism's Jade component, whose elements carry its uid as their tag. */
    private static final String MEKANISM_DATA = "mek_data";
    private static final ResourceLocation MEKANISM_TOOLTIP = ResourceLocation.fromNamespaceAndPath("mekanism", "tooltip_renderer");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof DrawerBlockEntity || accessor.getBlockEntity() instanceof IControlGroup) {
            // Mekanism's data for its own bars; ours replace them on these blocks, empty or not.
            data.remove(MEKANISM_DATA);
        }
        ListTag lines = new ListTag();
        for (ProbeReadings.Line line : ProbeReadings.of(accessor.getBlockEntity(), true)) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Label", Component.Serializer.toJson(line.label(), accessor.getLevel().registryAccess()));
            tag.putLong("Stored", line.stored());
            tag.putLong("Capacity", line.capacity());
            tag.putString("Unit", line.unit());
            tag.putInt("Color", line.color());
            lines.add(tag);
        }
        if (!lines.isEmpty()) {
            data.put(LINES, lines);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(LINES)) {
            return;
        }
        tooltip.remove(MEKANISM_TOOLTIP);
        IElementHelper helper = IElementHelper.get();
        for (Tag entry : data.getList(LINES, Tag.TAG_COMPOUND)) {
            CompoundTag line = (CompoundTag) entry;
            long stored = line.getLong("Stored");
            long capacity = line.getLong("Capacity");
            Component label = Component.Serializer.fromJson(line.getString("Label"), accessor.getLevel().registryAccess());
            Component text = Component.empty().append(label == null ? Component.empty() : label).append(" ")
                    .append(StorageDrawersContract.formatCount(stored) + " / " + StorageDrawersContract.formatCount(capacity)
                            + (line.getString("Unit").isEmpty() ? "" : " " + line.getString("Unit")));
            int color = line.getInt("Color");
            float ratio = capacity <= 0 ? 0 : (float) Math.min(1d, stored / (double) capacity);
            tooltip.add(helper.progress(ratio, text,
                    helper.progressStyle().color(scaled(color, 0.5f), scaled(color, 0.3f)).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
    }

    /** The colour at this brightness: dark enough under white text, whatever the tint. */
    private static int scaled(int color, float factor) {
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** After Mekanism's component, so its bars are there to remove. */
    @Override
    public int getDefaultPriority() {
        return 5000;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
