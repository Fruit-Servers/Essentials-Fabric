package net.essentialsx.fabric.items;

import net.essentialsx.fabric.Essentials;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;

/**
 * Complete item stack (de)serialisation using the registry-aware 1.21.1 codec (Section 11.1).
 * Used for {@code @base64} kit entries created with NBT serialisation enabled.
 */
public class ItemSerializer {
    private final Essentials ess;

    public ItemSerializer(final Essentials ess) {
        this.ess = ess;
    }

    public String serialize(final ItemStack stack) {
        try {
            final Tag tag = stack.save(ess.getServer().registryAccess());
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                NbtIo.writeAnyTag(tag, out);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (final Exception e) {
            ess.getLogger().warn("Failed to serialize item stack", e);
            return null;
        }
    }

    public ItemStack deserialize(final String base64) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(base64.replace("\n", "").replace("\r", ""));
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                final Tag tag = NbtIo.readAnyTag(in, NbtAccounter.create(2 * 1024 * 1024));
                if (tag instanceof CompoundTag compound) {
                    return ItemStack.parse(ess.getServer().registryAccess(), compound).orElse(null);
                }
            }
        } catch (final Exception e) {
            ess.getLogger().warn("Failed to deserialize item stack", e);
        }
        return null;
    }
}
