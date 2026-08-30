package net.essentialsx.fabric.utils;

import net.minecraft.server.level.ServerPlayer;

public final class SetExpFix {
    private SetExpFix() {
    }

    public static void setTotalExperience(final ServerPlayer player, final int exp) {
        if (exp < 0) {
            throw new IllegalArgumentException("Experience is negative!");
        }
        player.experienceProgress = 0;
        player.experienceLevel = 0;
        player.totalExperience = 0;
        int amount = exp;
        while (amount > 0) {
            final int expToLevel = getExpAtLevel(player);
            amount -= expToLevel;
            if (amount >= 0) {
                player.giveExperiencePoints(expToLevel);
            } else {
                amount += expToLevel;
                player.giveExperiencePoints(amount);
                amount = 0;
            }
        }
    }

    private static int getExpAtLevel(final ServerPlayer player) {
        return getExpAtLevel(player.experienceLevel);
    }

    public static int getExpAtLevel(final int level) {
        if (level <= 15) {
            return (2 * level) + 7;
        }
        if ((level >= 16) && (level <= 30)) {
            return (5 * level) - 38;
        }
        return (9 * level) - 158;
    }

    public static int getExpToLevel(final int level) {
        int currentLevel = 0;
        int exp = 0;
        while (currentLevel < level) {
            exp += getExpAtLevel(currentLevel);
            currentLevel++;
        }
        if (exp < 0) {
            exp = Integer.MAX_VALUE;
        }
        return exp;
    }

    public static int getTotalExperience(final ServerPlayer player) {
        int exp = Math.round(getExpAtLevel(player) * player.experienceProgress);
        int currentLevel = player.experienceLevel;
        while (currentLevel > 0) {
            currentLevel--;
            exp += getExpAtLevel(currentLevel);
        }
        if (exp < 0) {
            exp = Integer.MAX_VALUE;
        }
        return exp;
    }

    public static int getExpUntilNextLevel(final ServerPlayer player) {
        final int exp = Math.round(getExpAtLevel(player) * player.experienceProgress);
        final int nextLevel = player.experienceLevel;
        return getExpAtLevel(nextLevel) - exp;
    }
}
