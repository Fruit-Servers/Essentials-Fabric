package net.essentialsx.fabric.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Portable vanilla screen handlers (Section 11.3). Menus use an unrestricted
 * {@link ContainerLevelAccess} so they stay valid without a nearby block.
 */
public final class Workstations {
    private Workstations() {
    }

    private static ContainerLevelAccess access(final ServerPlayer player) {
        return ContainerLevelAccess.create(player.level(), player.blockPosition());
    }

    private static void open(final ServerPlayer player, final Component title, final MenuFactory factory) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player p) {
                return factory.create(id, inventory);
            }
        });
    }

    @FunctionalInterface
    private interface MenuFactory {
        AbstractContainerMenu create(int id, Inventory inventory);
    }

    /** Menu wrapper that never closes because the player is "too far" from a block. */
    private static final class AlwaysValid {
        static ContainerLevelAccess of(final ServerPlayer player) {
            return new ContainerLevelAccess() {
                @Override
                public <T> java.util.Optional<T> evaluate(final java.util.function.BiFunction<net.minecraft.world.level.Level, net.minecraft.core.BlockPos, T> function) {
                    return java.util.Optional.ofNullable(function.apply(player.level(), player.blockPosition()));
                }
            };
        }
    }

    public static void openAnvil(final ServerPlayer player) {
        open(player, Component.translatable("container.repair"), (id, inv) -> new AnvilMenu(id, inv, AlwaysValid.of(player)));
    }

    public static void openCartography(final ServerPlayer player) {
        open(player, Component.translatable("container.cartography_table"), (id, inv) -> new CartographyTableMenu(id, inv, AlwaysValid.of(player)));
    }

    public static void openGrindstone(final ServerPlayer player) {
        open(player, Component.translatable("container.grindstone_title"), (id, inv) -> new GrindstoneMenu(id, inv, AlwaysValid.of(player)));
    }

    public static void openLoom(final ServerPlayer player) {
        open(player, Component.translatable("container.loom"), (id, inv) -> new LoomMenu(id, inv, AlwaysValid.of(player)));
    }

    public static void openSmithing(final ServerPlayer player) {
        open(player, Component.translatable("container.upgrade"), (id, inv) -> new SmithingMenu(id, inv, AlwaysValid.of(player)));
    }

    public static void openStonecutter(final ServerPlayer player) {
        open(player, Component.translatable("container.stonecutter"), (id, inv) -> new StonecutterMenu(id, inv, AlwaysValid.of(player)));
    }

    public static void openWorkbench(final ServerPlayer player) {
        open(player, Component.translatable("container.crafting"), (id, inv) -> new CraftingMenu(id, inv, AlwaysValid.of(player)));
    }

    /**
     * Disposal: a non-persistent 4-row container whose contents are destroyed on close.
     */
    public static void openDisposal(final ServerPlayer player, final Component title) {
        final SimpleContainer container = new SimpleContainer(36);
        open(player, title, (id, inv) -> new TrackedChestMenu(MenuType.GENERIC_9x4, id, inv, container, 4, TrackedChestMenu.Kind.DISPOSAL, null));
    }

    /**
     * Free sign container: filled with copies of the item; taking is allowed, closing discards.
     */
    public static void openFree(final ServerPlayer player, final Component title, final ItemStack item) {
        final SimpleContainer container = new SimpleContainer(36);
        for (int i = 0; i < 36; i++) {
            container.setItem(i, item.copy());
        }
        open(player, title, (id, inv) -> new TrackedChestMenu(MenuType.GENERIC_9x4, id, inv, container, 4, TrackedChestMenu.Kind.FREE, null));
    }

    /**
     * View another player's ender chest.
     */
    public static void openEnderChest(final ServerPlayer viewer, final ServerPlayer target, final boolean readOnly) {
        final Container container = target.getEnderChestInventory();
        open(viewer, Component.translatable("container.enderchest"), (id, inv) -> new TrackedChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3, readOnly ? TrackedChestMenu.Kind.ENDER_READONLY : TrackedChestMenu.Kind.ENDER, target.getUUID()));
    }

    /**
     * View another player's inventory (main 36 slots + armour/offhand in the 5th row).
     */
    public static void openInvsee(final ServerPlayer viewer, final ServerPlayer target, final boolean readOnly, final Component title) {
        final Container container = new PlayerInventoryView(target);
        open(viewer, title, (id, inv) -> new TrackedChestMenu(MenuType.GENERIC_9x5, id, inv, container, 5, readOnly ? TrackedChestMenu.Kind.INVSEE_READONLY : TrackedChestMenu.Kind.INVSEE, target.getUUID()));
    }

    /**
     * Chest menu tagged with its Essentials purpose so listeners can enforce read-only views
     * and clean up on close.
     */
    public static final class TrackedChestMenu extends ChestMenu {
        public enum Kind {
            DISPOSAL, FREE, ENDER, ENDER_READONLY, INVSEE, INVSEE_READONLY
        }

        private final Kind kind;
        private final java.util.UUID target;

        TrackedChestMenu(final MenuType<?> type, final int id, final Inventory inventory, final Container container, final int rows, final Kind kind, final java.util.UUID target) {
            super(type, id, inventory, container, rows);
            this.kind = kind;
            this.target = target;
        }

        public Kind getKind() {
            return kind;
        }

        public java.util.UUID getTarget() {
            return target;
        }

        public boolean isReadOnly() {
            return kind == Kind.ENDER_READONLY || kind == Kind.INVSEE_READONLY;
        }

        @Override
        public void clicked(final int slotId, final int button, final net.minecraft.world.inventory.ClickType clickType, final Player player) {
            if (isReadOnly()) {
                if (slotId >= 0 && slotId < getContainer().getContainerSize()) {
                    return;
                }
                if (clickType == net.minecraft.world.inventory.ClickType.QUICK_MOVE || clickType == net.minecraft.world.inventory.ClickType.SWAP) {
                    return;
                }
            }
            if (kind == Kind.FREE && slotId >= 0 && slotId < getContainer().getContainerSize()) {
                // Taking from a free sign never depletes the container.
                super.clicked(slotId, button, clickType, player);
                if (getContainer().getItem(slotId).isEmpty() || getContainer().getItem(slotId).getCount() < getContainer().getItem(slotId).getMaxStackSize()) {
                    final ItemStack template = firstTemplate();
                    if (template != null) {
                        getContainer().setItem(slotId, template.copy());
                    }
                }
                return;
            }
            super.clicked(slotId, button, clickType, player);
        }

        private ItemStack firstTemplate() {
            for (int i = 0; i < getContainer().getContainerSize(); i++) {
                final ItemStack stack = getContainer().getItem(i);
                if (!stack.isEmpty()) {
                    return stack.copyWithCount(stack.getMaxStackSize());
                }
            }
            return null;
        }

        @Override
        public void removed(final Player player) {
            super.removed(player);
            if (kind == Kind.DISPOSAL || kind == Kind.FREE) {
                getContainer().clearContent();
            }
        }
    }

    /**
     * Live view over a player's inventory laid out as a 5-row chest: rows 1-4 are the
     * hotbar + main inventory, row 5 holds armour and the off-hand slot.
     */
    public static final class PlayerInventoryView implements Container {
        private final ServerPlayer target;

        PlayerInventoryView(final ServerPlayer target) {
            this.target = target;
        }

        private int map(final int slot) {
            if (slot < 36) {
                return slot;
            }
            return switch (slot) {
                case 36 -> 39; // helmet
                case 37 -> 38; // chestplate
                case 38 -> 37; // leggings
                case 39 -> 36; // boots
                case 40 -> 40; // offhand
                default -> -1;
            };
        }

        @Override
        public int getContainerSize() {
            return 45;
        }

        @Override
        public boolean isEmpty() {
            return target.getInventory().isEmpty();
        }

        @Override
        public ItemStack getItem(final int slot) {
            final int mapped = map(slot);
            return mapped == -1 ? ItemStack.EMPTY : target.getInventory().getItem(mapped);
        }

        @Override
        public ItemStack removeItem(final int slot, final int amount) {
            final int mapped = map(slot);
            if (mapped == -1) {
                return ItemStack.EMPTY;
            }
            final ItemStack result = target.getInventory().removeItem(mapped, amount);
            target.inventoryMenu.broadcastChanges();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(final int slot) {
            final int mapped = map(slot);
            if (mapped == -1) {
                return ItemStack.EMPTY;
            }
            return target.getInventory().removeItemNoUpdate(mapped);
        }

        @Override
        public void setItem(final int slot, final ItemStack stack) {
            final int mapped = map(slot);
            if (mapped != -1) {
                target.getInventory().setItem(mapped, stack);
                target.inventoryMenu.broadcastChanges();
            }
        }

        @Override
        public void setChanged() {
            target.getInventory().setChanged();
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            target.getInventory().clearContent();
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return map(slot) != -1;
        }
    }
}
