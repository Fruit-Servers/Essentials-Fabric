package net.essentialsx.fabric.perm;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Optional LuckPerms adapter (Section 7.2). Only touched when the LuckPerms mod is loaded;
 * every call is guarded so a missing API never breaks the core.
 */
final class LuckPermsBridge {
    private LuckPermsBridge() {
    }

    private static LuckPerms api() {
        try {
            return LuckPermsProvider.get();
        } catch (final Throwable t) {
            return null;
        }
    }

    static String getPrimaryGroup(final UUID uuid) {
        try {
            final LuckPerms lp = api();
            if (lp == null) {
                return null;
            }
            final User user = lp.getUserManager().getUser(uuid);
            return user == null ? null : user.getPrimaryGroup();
        } catch (final Throwable t) {
            return null;
        }
    }

    static List<String> getGroups(final UUID uuid) {
        try {
            final LuckPerms lp = api();
            if (lp == null) {
                return null;
            }
            final User user = lp.getUserManager().getUser(uuid);
            if (user == null) {
                return null;
            }
            final List<String> groups = new ArrayList<>();
            for (final InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
                groups.add(node.getGroupName());
            }
            return groups;
        } catch (final Throwable t) {
            return null;
        }
    }

    static List<String> getAllGroups() {
        try {
            final LuckPerms lp = api();
            if (lp == null) {
                return null;
            }
            final List<String> groups = new ArrayList<>();
            for (final Group group : lp.getGroupManager().getLoadedGroups()) {
                groups.add(group.getName());
            }
            return groups;
        } catch (final Throwable t) {
            return null;
        }
    }
}
