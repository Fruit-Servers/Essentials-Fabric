package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.signs.EssentialsSign;
import net.essentialsx.fabric.signs.SignListener;
import net.essentialsx.fabric.text.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Persistent Essentials sign data (section 12.x) and the sign-edit hook that runs the sign pipeline
 * (create/validate/colour) before the text is committed.
 */
@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements EssentialsSign.EssentialsSignData {
    @Unique
    private static final String ESSENTIALS_TAG = "EssentialsData";
    @Unique
    private CompoundTag essentials$data;

    protected SignBlockEntityMixin(final net.minecraft.world.level.block.entity.BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public CompoundTag essentials$getData() {
        return essentials$data;
    }

    @Override
    public void essentials$setData(final CompoundTag tag) {
        essentials$data = tag == null || tag.isEmpty() ? null : tag;
        this.setChanged();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void essentials$save(final CompoundTag tag, final HolderLookup.Provider registries, final CallbackInfo ci) {
        if (essentials$data != null && !essentials$data.isEmpty()) {
            tag.put(ESSENTIALS_TAG, essentials$data.copy());
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void essentials$load(final CompoundTag tag, final HolderLookup.Provider registries, final CallbackInfo ci) {
        essentials$data = tag.contains(ESSENTIALS_TAG, CompoundTag.TAG_COMPOUND) ? tag.getCompound(ESSENTIALS_TAG).copy() : null;
    }

    @Inject(method = "updateSignText", at = @At("HEAD"), cancellable = true)
    private void essentials$signChange(final Player player, final boolean front, final List<FilteredText> filtered, final CallbackInfo ci) {
        final SignListener listener = EssentialsFabric.signs();
        final SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (listener == null || !(player instanceof ServerPlayer serverPlayer) || !(self.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (self.isWaxed() || !player.getUUID().equals(self.getPlayerWhoMayEdit())) {
            return; // vanilla rejects these edits itself
        }
        final String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = i < filtered.size() ? filtered.get(i).raw() : "";
        }
        final String[] original = lines.clone();
        final boolean allowed = listener.onSignChange(level, self.getBlockPos(), serverPlayer, lines);
        if (!allowed) {
            ci.cancel();
            self.setAllowedPlayerEditor(null);
            level.sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), 3);
            return;
        }
        boolean changed = false;
        for (int i = 0; i < 4; i++) {
            if (!original[i].equals(lines[i])) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }
        // The pipeline rewrote the lines (e.g. colouring a validated sign): commit them ourselves.
        ci.cancel();
        SignText text = self.getText(front);
        for (int i = 0; i < 4; i++) {
            text = text.setMessage(i, Text.get().legacy(lines[i] == null ? "" : lines[i]));
        }
        self.setText(text, front);
        self.setAllowedPlayerEditor(null);
        self.setChanged();
        level.sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), 3);
    }
}
