package net.essentialsx.fabric.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item/block classification helpers replacing Bukkit material sets.
 */
public final class MaterialUtil {
    private MaterialUtil() {
    }

    public static boolean isHelmet(final Item item) {
        return item instanceof ArmorItem armor && armor.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.HEAD;
    }

    public static boolean isChestplate(final Item item) {
        return (item instanceof ArmorItem armor && armor.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.CHEST) || item instanceof ElytraItem;
    }

    public static boolean isLeggings(final Item item) {
        return item instanceof ArmorItem armor && armor.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.LEGS;
    }

    public static boolean isBoots(final Item item) {
        return item instanceof ArmorItem armor && armor.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.FEET;
    }

    public static boolean isArmor(final Item item) {
        return item instanceof ArmorItem;
    }

    public static boolean isBed(final BlockState state) {
        return state.is(BlockTags.BEDS);
    }

    public static boolean isBed(final Item item) {
        return new ItemStack(item).is(ItemTags.BEDS);
    }

    public static boolean isBanner(final Item item) {
        return new ItemStack(item).is(ItemTags.BANNERS) || item == Items.SHIELD;
    }

    public static boolean isFirework(final Item item) {
        return item == Items.FIREWORK_ROCKET;
    }

    public static boolean isFireworkCharge(final Item item) {
        return item == Items.FIREWORK_STAR;
    }

    public static boolean isLeatherArmor(final Item item) {
        return item instanceof ArmorItem armor && armor.getMaterial().is(ArmorMaterials.LEATHER);
    }

    public static boolean isPlayerHead(final ItemStack stack) {
        return stack.is(Items.PLAYER_HEAD);
    }

    public static boolean isPotion(final Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW;
    }

    public static boolean isSignPost(final Block block) {
        return block instanceof StandingSignBlock;
    }

    public static boolean isWallSign(final Block block) {
        return block instanceof WallSignBlock;
    }

    public static boolean isHangingSign(final Block block) {
        return block instanceof CeilingHangingSignBlock;
    }

    public static boolean isWallHangingSign(final Block block) {
        return block instanceof WallHangingSignBlock;
    }

    public static boolean isSign(final Block block) {
        return block instanceof SignBlock;
    }

    public static boolean isSign(final BlockState state) {
        return state.getBlock() instanceof SignBlock;
    }

    public static boolean isEditableBook(final Item item) {
        return item == Items.WRITTEN_BOOK || item == Items.WRITABLE_BOOK;
    }

    public static boolean isSkull(final Item item) {
        return item == Items.PLAYER_HEAD || item == Items.SKELETON_SKULL || item == Items.WITHER_SKELETON_SKULL || item == Items.CREEPER_HEAD || item == Items.ZOMBIE_HEAD || item == Items.DRAGON_HEAD || item == Items.PIGLIN_HEAD;
    }

    public static boolean isAir(final Item item) {
        return item == Items.AIR;
    }

    public static boolean isBlock(final Item item) {
        return item instanceof BlockItem;
    }

    public static int getDamage(final ItemStack stack) {
        return stack.getDamageValue();
    }

    public static boolean isSpawner(final Block block) {
        return block == Blocks.SPAWNER;
    }

    public static boolean hasCustomName(final ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }
}
