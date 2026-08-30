package net.essentialsx.fabric.teleport;

import net.essentialsx.fabric.user.LazyLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlayerTarget implements ITarget {
    private final ServerPlayer entity;

    public PlayerTarget(final ServerPlayer entity) {
        this.entity = entity;
    }

    @Override
    public LazyLocation getLocation() {
        return LazyLocation.of(entity);
    }

    public ServerPlayer getPlayer() {
        return entity;
    }
}
