package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.Workstations;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.NumberUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Commandrecipe extends EssentialsCommand {
    public Commandrecipe() {
        super("recipe");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final ItemStack itemType;
        if (args[0].equalsIgnoreCase("hand")) {
            if (!sender.isPlayer()) {
                throw new TranslatableException("consoleCannotUseCommand");
            }
            itemType = Inventories.getItemInHand(sender.getPlayer());
        } else {
            itemType = ess.getItemDb().get(args[0]);
        }
        int recipeNo = 0;
        if (args.length > 1) {
            if (NumberUtil.isInt(args[1])) {
                recipeNo = Integer.parseInt(args[1]) - 1;
            } else {
                throw new TranslatableException("invalidNumber");
            }
        }
        final List<Recipe<?>> recipes = new ArrayList<>();
        for (final RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            final Recipe<?> recipe = holder.value();
            if (!(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe || recipe instanceof AbstractCookingRecipe)) {
                continue;
            }
            final ItemStack result = recipe.getResultItem(server.registryAccess());
            if (result != null && !result.isEmpty() && result.getItem() == itemType.getItem()) {
                recipes.add(recipe);
            }
        }
        if (recipes.isEmpty()) {
            throw new TranslatableException("recipeNone", getMaterialName(sender, itemType));
        }
        if (recipeNo < 0 || recipeNo >= recipes.size()) {
            throw new TranslatableException("recipeBadIndex");
        }
        final Recipe<?> selectedRecipe = recipes.get(recipeNo);
        sender.sendTl("recipe", getMaterialName(sender, itemType), recipeNo + 1, recipes.size());
        if (selectedRecipe instanceof AbstractCookingRecipe) {
            furnaceRecipe(sender, (AbstractCookingRecipe) selectedRecipe);
        } else if (selectedRecipe instanceof ShapedRecipe) {
            shapedRecipe(sender, (ShapedRecipe) selectedRecipe, sender.isPlayer());
        } else if (selectedRecipe instanceof ShapelessRecipe) {
            shapelessRecipe(sender, (ShapelessRecipe) selectedRecipe, sender.isPlayer());
        }
        if (recipes.size() > 1 && args.length == 1) {
            sender.sendTl("recipeMore", commandLabel, args[0], getMaterialName(sender, itemType));
        }
    }

    private static ItemStack first(final Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }
        final ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? null : items[0];
    }

    public void furnaceRecipe(final CommandSource sender, final AbstractCookingRecipe recipe) {
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        sender.sendTl("recipeFurnace", getMaterialName(sender, ingredients.isEmpty() ? null : first(ingredients.get(0))));
    }

    /** Opens a crafting table for the user and returns its menu, or null when it could not be opened. */
    private CraftingMenu openWorkbench(final User user) {
        user.getBase().closeContainer();
        user.setRecipeSee(true);
        Workstations.openWorkbench(user.getBase());
        if (user.getBase().containerMenu instanceof CraftingMenu menu) {
            return menu;
        }
        user.setRecipeSee(false);
        return null;
    }

    public void shapedRecipe(final CommandSource sender, final ShapedRecipe recipe, final boolean showWindow) {
        final int width = recipe.getWidth();
        final int height = recipe.getHeight();
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        final ItemStack[][] grid = new ItemStack[3][3];
        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                grid[j][k] = first(ingredients.get(j * width + k));
            }
        }
        if (showWindow) {
            final User user = ess.getUser(sender.getPlayer());
            final CraftingMenu menu = openWorkbench(user);
            if (menu == null) {
                return;
            }
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    final ItemStack item = grid[j][k];
                    if (item == null) {
                        continue;
                    }
                    menu.getSlot(j * 3 + k + 1).set(item.copy());
                }
            }
            menu.broadcastChanges();
        } else {
            final Map<Item, String> colorMap = new LinkedHashMap<>();
            int i = 1;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    final ItemStack item = grid[j][k];
                    final Item key = item == null ? null : item.getItem();
                    if (!colorMap.containsKey(key)) {
                        colorMap.put(key, String.valueOf(i++));
                    }
                }
            }
            final Item[][] materials = new Item[3][3];
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    materials[j][k] = grid[j][k] == null ? null : grid[j][k].getItem();
                }
            }
            sender.sendTl("recipeGrid", colorTag(colorMap, materials, 0, 0), colorTag(colorMap, materials, 0, 1), colorTag(colorMap, materials, 0, 2));
            sender.sendTl("recipeGrid", colorTag(colorMap, materials, 1, 0), colorTag(colorMap, materials, 1, 1), colorTag(colorMap, materials, 1, 2));
            sender.sendTl("recipeGrid", colorTag(colorMap, materials, 2, 0), colorTag(colorMap, materials, 2, 1), colorTag(colorMap, materials, 2, 2));
            final StringBuilder s = new StringBuilder();
            for (final Item items : colorMap.keySet()) {
                s.append(sender.tl("recipeGridItem", colorMap.get(items), getMaterialName(sender, items))).append(" ");
            }
            sender.sendTl("recipeWhere", Text.parsed(s.toString()));
        }
    }

    private Text.ParsedPlaceholder colorTag(final Map<Item, String> colorMap, final Item[][] materials, final int x, final int y) {
        final char colorChar = colorMap.get(materials[x][y]).charAt(0);
        final NamedTextColor namedTextColor = Text.fromChar(colorChar);
        if (namedTextColor == null) {
            throw new IllegalStateException("Illegal amount of materials in recipe");
        }
        return Text.parsed("<" + namedTextColor + ">" + colorChar);
    }

    public void shapelessRecipe(final CommandSource sender, final ShapelessRecipe recipe, final boolean showWindow) {
        final List<ItemStack> ingredients = new ArrayList<>();
        for (final Ingredient ingredient : recipe.getIngredients()) {
            final ItemStack stack = first(ingredient);
            if (stack != null) {
                ingredients.add(stack);
            }
        }
        if (showWindow) {
            final User user = ess.getUser(sender.getPlayer());
            final CraftingMenu menu = openWorkbench(user);
            if (menu == null) {
                return;
            }
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                menu.getSlot(i + 1).set(ingredients.get(i).copy());
            }
            menu.broadcastChanges();
        } else {
            final StringBuilder s = new StringBuilder();
            for (int i = 0; i < ingredients.size(); i++) {
                s.append(getMaterialName(sender, ingredients.get(i)));
                if (i != ingredients.size() - 1) {
                    s.append(",");
                }
                s.append(" ");
            }
            sender.sendTl("recipeShapeless", s.toString());
        }
    }

    public String getMaterialName(final CommandSource sender, final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return sender.tl("recipeNothing");
        }
        return getMaterialName(sender, stack.getItem());
    }

    public String getMaterialName(final CommandSource sender, final Item type) {
        if (type == null) {
            return sender.tl("recipeNothing");
        }
        return UserData.itemKey(type).replace("_", " ").toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getItems();
        } else {
            return Collections.emptyList();
        }
    }
}
