package dev.drimoz.immaterialstorage.compat.jade;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.compat.ProbeReadings;
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
 */
public enum ReadingsProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "contents");
    private static final String LINES = "ImmaterialStorageLines";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
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
            tooltip.add(helper.progress(ratio, text, helper.progressStyle().color(color, darker(color)), BoxStyle.getNestedBox(), true));
        }
    }

    private static int darker(int color) {
        return 0xFF000000 | ((color >> 17 & 0x7F) << 16) | ((color >> 9 & 0x7F) << 8) | (color >> 1 & 0x7F);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
