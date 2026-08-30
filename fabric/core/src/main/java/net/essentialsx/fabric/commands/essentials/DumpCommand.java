package net.essentialsx.fabric.commands.essentials;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.essentialsx.fabric.utils.DateUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Writes a support dump. Unlike upstream (which uploads to pastes.dev), the Fabric port writes the
 * files to {@code <dataFolder>/dumps/<timestamp>/} so nothing leaves the server without the admin's say-so.
 */
public class DumpCommand extends EssentialsTreeNode {
    public DumpCommand() {
        super("dump");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sender.sendTl("dumpCreating");
        final JsonObject dump = new JsonObject();
        final JsonObject meta = new JsonObject();
        meta.addProperty("timestamp", Instant.now().toEpochMilli());
        meta.addProperty("sender", sender.isPlayer() ? sender.getName() : null);
        meta.addProperty("senderUuid", sender.isPlayer() ? sender.getPlayer().getUUID().toString() : null);
        dump.add("meta", meta);
        final JsonObject serverData = new JsonObject();
        serverData.addProperty("minecraft-version", ess.getServer().getServerVersion());
        serverData.addProperty("server-brand", ess.getServer().getServerModName());
        serverData.addProperty("fabric-loader", FabricLoader.getInstance().getModContainer("fabricloader").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        serverData.addProperty("online-mode", ess.getServer().usesAuthentication());
        dump.add("server-data", serverData);
        final JsonObject environment = new JsonObject();
        environment.addProperty("java-version", System.getProperty("java.version"));
        environment.addProperty("operating-system", System.getProperty("os.name"));
        environment.addProperty("uptime", DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()));
        environment.addProperty("allocated-memory", (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB");
        dump.add("environment", environment);
        final JsonObject essData = new JsonObject();
        essData.addProperty("version", VersionCommand.essentialsVersion());
        final JsonObject econLayer = new JsonObject();
        econLayer.addProperty("enabled", !ess.getSettings().isEcoDisabled());
        econLayer.addProperty("name", "Impactor");
        econLayer.addProperty("currency", ess.getEconomy() == null ? "null" : ess.getEconomy().currencyName());
        essData.add("economy-layer", econLayer);
        dump.add("ess-data", essData);
        final JsonArray mods = new JsonArray();
        final List<ModContainer> alphabetical = new ArrayList<>(FabricLoader.getInstance().getAllMods());
        alphabetical.sort(Comparator.comparing(o -> o.getMetadata().getId().toUpperCase(Locale.ENGLISH)));
        for (final ModContainer mod : alphabetical) {
            final ModMetadata info = mod.getMetadata();
            final JsonObject modData = new JsonObject();
            modData.addProperty("id", info.getId());
            modData.addProperty("name", info.getName());
            modData.addProperty("version", info.getVersion().getFriendlyString());
            modData.addProperty("description", info.getDescription());
            final JsonArray authors = new JsonArray();
            for (final Person author : info.getAuthors()) {
                authors.add(author.getName());
            }
            modData.add("authors", authors);
            mods.add(modData);
        }
        dump.add("mods", mods);
        boolean config = false;
        boolean kits = false;
        boolean log = false;
        boolean worth = false;
        boolean tpr = false;
        boolean spawns = false;
        for (final String arg : args) {
            if (arg.equals("*") || arg.equalsIgnoreCase("all")) {
                config = kits = log = worth = tpr = spawns = true;
                break;
            } else if (arg.equalsIgnoreCase("config")) {
                config = true;
            } else if (arg.equalsIgnoreCase("kits")) {
                kits = true;
            } else if (arg.equalsIgnoreCase("log")) {
                log = true;
            } else if (arg.equalsIgnoreCase("worth")) {
                worth = true;
            } else if (arg.equalsIgnoreCase("tpr")) {
                tpr = true;
            } else if (arg.equalsIgnoreCase("spawns")) {
                spawns = true;
            }
        }
        final boolean fConfig = config, fKits = kits, fLog = log, fWorth = worth, fTpr = tpr, fSpawns = spawns;
        ess.runTaskAsynchronously(() -> {
            try {
                final Path dir = ess.getDataFolder().resolve("dumps").resolve(String.valueOf(System.currentTimeMillis()));
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("dump.json"), new GsonBuilder().setPrettyPrinting().create().toJson(dump), StandardCharsets.UTF_8);
                if (fConfig) {
                    copy(sender, ess.getSettings().getConfigFile(), dir, "config.yml", null);
                }
                if (fKits) {
                    copy(sender, ess.getKits().getFile(), dir, "kits.yml", null);
                }
                if (fWorth) {
                    copy(sender, ess.getWorth().getFile(), dir, "worth.yml", null);
                }
                if (fTpr) {
                    copy(sender, ess.getRandomTeleport().getFile(), dir, "tpr.yml", null);
                }
                if (fSpawns) {
                    copy(sender, ess.getDataFolder().resolve("spawn.yml"), dir, "spawn.yml", null);
                }
                if (fLog) {
                    copy(sender, Paths.get("logs", "latest.log"), dir, "latest.log", "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
                }
                final String url = dir.toAbsolutePath().toString();
                sender.sendTl("dumpUrl", url);
                if (sender.isPlayer()) {
                    ess.getLogger().info("Support dump written to " + url);
                }
            } catch (final IOException e) {
                sender.sendTl("dumpError", e.getMessage());
            }
        });
    }

    private void copy(final CommandSource sender, final Path source, final Path dir, final String name, final String censorRegex) {
        try {
            if (source == null || !Files.exists(source)) {
                return;
            }
            String content = Files.readString(source, StandardCharsets.UTF_8);
            if (censorRegex != null) {
                content = content.replaceAll(censorRegex, "<censored ip address>");
            }
            Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            sender.sendTl("dumpErrorUpload", name, e.getMessage());
        }
    }

    @Override
    protected List<String> tabComplete(final CommandSource sender, final String commandLabel, final String[] args) {
        final List<String> list = new ArrayList<>(List.of("config", "kits", "log", "worth", "tpr", "spawns", "all"));
        for (final String arg : args) {
            if (arg.equals("*") || arg.equalsIgnoreCase("all")) {
                list.clear();
                return list;
            }
            list.remove(arg.toLowerCase(Locale.ENGLISH));
        }
        return list;
    }
}
