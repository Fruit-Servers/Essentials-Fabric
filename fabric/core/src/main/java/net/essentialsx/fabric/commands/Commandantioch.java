package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.PrimedTnt;

// This command has a in theme message that only shows if you supply a parameter #EasterEgg
public class Commandantioch extends EssentialsCommand {
    public Commandantioch() {
        super("antioch");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0) {
            ess.broadcastMessage("...lobbest thou thy Holy Hand Grenade of Antioch towards thy foe,");
            ess.broadcastMessage("who being naughty in My sight, shall snuff it.");
        }
        final LazyLocation loc = LocationUtil.getTarget(user.getBase());
        if (loc == null) {
            throw new TranslatableException("jumpError");
        }
        final PrimedTnt tnt = new PrimedTnt(user.getWorld(), loc.x(), loc.y(), loc.z(), user.getBase());
        user.getWorld().addFreshEntity(tnt);
    }
}
