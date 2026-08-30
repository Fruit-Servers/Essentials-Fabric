package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Arrays;
import java.util.List;

public class VersionCommand extends EssentialsTreeNode {
    /** Mods worth calling out in version output (APIs, permissions, economy). */
    private static final List<String> VERSION_MODS = Arrays.asList(
        "fabric-api",
        "fabric-permissions-api-v0",
        "luckperms",
        "impactor",
        "cloud"
    );

    public VersionCommand() {
        super("version", "ver");
    }

    public static String essentialsVersion() {
        return FabricLoader.getInstance().getModContainer("essentials_fabric").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    private static String version(final String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(null);
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.version")) {
            return;
        }
        final String essVer = essentialsVersion();
        sender.sendTl("versionOutputFine", "Server", ess.getServer().getServerVersion() + " " + ess.getServer().getServerModName());
        sender.sendTl("versionOutputFine", "Brand", "Fabric " + version("fabricloader"));
        sender.sendTl("versionOutputFine", "EssentialsX", essVer);
        boolean isMismatched = false;
        for (final ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            final String id = mod.getMetadata().getId();
            final String version = mod.getMetadata().getVersion().getFriendlyString();
            if (id.startsWith("essentials_fabric") && !id.equals("essentials_fabric")) {
                if (!version.equalsIgnoreCase(essVer)) {
                    isMismatched = true;
                    sender.sendTl("versionOutputWarn", mod.getMetadata().getName(), version);
                } else {
                    sender.sendTl("versionOutputFine", mod.getMetadata().getName(), version);
                }
            } else if (VERSION_MODS.contains(id)) {
                sender.sendTl("versionOutputFine", mod.getMetadata().getName(), version);
            }
        }
        final String layer;
        if (ess.getSettings().isEcoDisabled()) {
            layer = "Disabled";
        } else if (ess.getEconomy() != null) {
            layer = "Impactor (" + ess.getEconomy().currencyName() + ")";
        } else {
            layer = "None";
        }
        sender.sendTl("versionOutputEconLayer", layer);
        if (isMismatched) {
            sender.sendTl("versionMismatchAll");
        }
    }
}
