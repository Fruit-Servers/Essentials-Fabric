package net.essentialsx.fabric.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class Commandskull extends EssentialsCommand {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern URL_VALUE_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern BASE_64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]{180}$");

    public Commandskull() {
        super("skull");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final String owner;
        final User player;
        if (args.length == 2) {
            player = getPlayer(server, args, 1, false, false);
        } else {
            player = user;
        }
        if (args.length > 0 && player.isAuthorized("essentials.skull.others")) {
            if (BASE_64_PATTERN.matcher(args[0]).matches()) {
                try {
                    final String decoded = new String(Base64.getDecoder().decode(args[0]), StandardCharsets.UTF_8);
                    final JsonObject jsonObject = JsonParser.parseString(decoded).getAsJsonObject();
                    final String url = jsonObject
                        .getAsJsonObject("textures")
                        .getAsJsonObject("SKIN")
                        .get("url")
                        .getAsString();
                    owner = url.substring(url.lastIndexOf("/") + 1);
                } catch (final Exception e) {
                    // Any exception that can realistically happen here is caused by an invalid texture value
                    throw new TranslatableException("skullInvalidBase64");
                }
                if (!URL_VALUE_PATTERN.matcher(owner).matches()) {
                    throw new TranslatableException("skullInvalidBase64");
                }
            } else if (!NAME_PATTERN.matcher(args[0]).matches()) {
                throw new TranslatableException("alphaNames");
            } else {
                owner = args[0];
            }
        } else {
            owner = user.getName();
        }
        ItemStack itemSkull = user.getItemInHand();
        boolean spawn = false;
        if (itemSkull != null && MaterialUtil.isPlayerHead(itemSkull) && user == player) {
            // edit the held head in place
        } else if (user == player ? user.isAuthorized("essentials.skull.spawn") : user.isAuthorized("essentials.skull.spawn.others")) {
            itemSkull = new ItemStack(Items.PLAYER_HEAD);
            spawn = true;
        } else {
            throw new TranslatableException("invalidSkull");
        }
        if (itemSkull.has(DataComponents.PROFILE) && !user.isAuthorized("essentials.skull.modify")) {
            throw new TranslatableException("noPermissionSkull");
        }
        editSkull(user, player, itemSkull, owner, spawn);
    }

    private void editSkull(final User user, final User receive, final ItemStack stack, final String owner, final boolean spawn) {
        ess.runTaskAsynchronously(() -> {
            // Run this stuff async because it causes an HTTP request
            final String shortOwnerName;
            final ResolvableProfile profile;
            if (URL_VALUE_PATTERN.matcher(owner).matches()) {
                final String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + owner + "\"}}}";
                final String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                final PropertyMap properties = new PropertyMap();
                properties.put("textures", new Property("textures", value));
                profile = new ResolvableProfile(Optional.empty(), Optional.of(UUID.randomUUID()), properties);
                shortOwnerName = owner.substring(0, 7);
            } else {
                Optional<GameProfile> fetched = Optional.empty();
                try {
                    fetched = SkullBlockEntity.fetchGameProfile(owner).join();
                } catch (final Exception ignored) {
                }
                profile = fetched.map(ResolvableProfile::new).orElseGet(() -> new ResolvableProfile(Optional.of(owner), Optional.empty(), new PropertyMap()));
                shortOwnerName = owner;
            }
            ess.scheduleSyncDelayedTask(() -> {
                stack.set(DataComponents.PROFILE, profile);
                stack.set(DataComponents.CUSTOM_NAME, Text.get().legacy("§fSkull of " + shortOwnerName));
                if (spawn) {
                    Inventories.addItem(receive.getBase(), stack);
                    Inventories.update(receive.getBase());
                    receive.sendTl("givenSkull", shortOwnerName);
                    if (user != receive) {
                        user.sendTl("givenSkullOther", receive.getDisplayName(), shortOwnerName);
                    }
                    return;
                }
                Inventories.update(user.getBase());
                user.sendTl("skullChanged", shortOwnerName);
            });
        });
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 || args.length == 2) {
            if (user.isAuthorized("essentials.skull.others")) {
                return getPlayers(user);
            } else {
                return new ArrayList<>(List.of(user.getName()));
            }
        } else {
            return Collections.emptyList();
        }
    }
}
