package net.essentialsx.fabric.teleport;

import net.essentialsx.fabric.user.LazyLocation;

public class LocationTarget implements ITarget {
    private final LazyLocation location;

    public LocationTarget(final LazyLocation location) {
        this.location = location;
    }

    @Override
    public LazyLocation getLocation() {
        return location;
    }
}
