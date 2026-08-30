package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.backup.Backup;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.text.TranslatableException;
import net.minecraft.server.MinecraftServer;

public class Commandbackup extends EssentialsCommand {
    public Commandbackup() {
        super("backup");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final Backup backup = ess.getBackup();
        if (backup == null) {
            throw new TranslatableException("backupDisabled");
        }
        final String command = ess.getSettings().getBackupCommand();
        if (command == null || "".equals(command) || "save-all".equalsIgnoreCase(command)) {
            throw new TranslatableException("backupDisabled");
        }
        backup.run();
        sender.sendTl("backupStarted");
    }
}
