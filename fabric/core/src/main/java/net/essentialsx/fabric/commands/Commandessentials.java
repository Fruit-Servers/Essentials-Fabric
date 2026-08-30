package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsTreeCommand;
import net.essentialsx.fabric.commands.essentials.CleanupCommand;
import net.essentialsx.fabric.commands.essentials.CommandMapCommand;
import net.essentialsx.fabric.commands.essentials.DebugCommand;
import net.essentialsx.fabric.commands.essentials.DumpCommand;
import net.essentialsx.fabric.commands.essentials.HomesCommand;
import net.essentialsx.fabric.commands.essentials.ItemTestCommand;
import net.essentialsx.fabric.commands.essentials.MooCommand;
import net.essentialsx.fabric.commands.essentials.NyanCommand;
import net.essentialsx.fabric.commands.essentials.ReloadCommand;
import net.essentialsx.fabric.commands.essentials.UsermapCommand;
import net.essentialsx.fabric.commands.essentials.VersionCommand;

// This command has 4 undocumented behaviours #EasterEgg
public class Commandessentials extends EssentialsTreeCommand {
    public Commandessentials() {
        super("essentials");
        registerNode(new VersionCommand());
        registerNode(new DebugCommand());
        registerNode(new CommandMapCommand());
        registerNode(new DumpCommand());
        registerNode(new ReloadCommand());
        registerNode(new CleanupCommand());
        registerNode(new HomesCommand());
        registerNode(new UsermapCommand());
        registerNode(new ItemTestCommand());
        registerNode(new NyanCommand());
        registerNode(new MooCommand());
    }
}
