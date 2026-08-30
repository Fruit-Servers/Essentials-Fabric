package net.essentialsx.fabric.teleport;

import net.essentialsx.fabric.user.LazyLocation;

import java.util.UUID;

/**
 * Pending teleport request (Section 8.5).
 */
public class TpaRequest {
    private final String name;
    private final UUID requesterUuid;
    private boolean here;
    private long time;
    private LazyLocation location;

    public TpaRequest(final String name, final UUID requesterUuid) {
        this.name = name;
        this.requesterUuid = requesterUuid;
    }

    public String getName() {
        return name;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public boolean isHere() {
        return here;
    }

    public void setHere(final boolean here) {
        this.here = here;
    }

    public long getTime() {
        return time;
    }

    public void setTime(final long time) {
        this.time = time;
    }

    public LazyLocation getLocation() {
        return location;
    }

    public void setLocation(final LazyLocation location) {
        this.location = location;
    }
}
