package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commandcondense extends EssentialsCommand {
    private final Map<String, SimpleRecipe> condenseList = new HashMap<>();

    public Commandcondense() {
        super("condense");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        List<ItemStack> is = new ArrayList<>();
        boolean validateReverse = false;
        if (args.length > 0) {
            is = ess.getItemDb().getMatching(user, args);
        } else {
            for (final ItemStack stack : Inventories.getInventory(user.getBase(), false)) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                is.add(stack.copy());
            }
            validateReverse = true;
        }
        boolean didConvert = false;
        for (final ItemStack itemStack : is) {
            if (condenseStack(user, itemStack, validateReverse)) {
                didConvert = true;
            }
        }
        Inventories.update(user.getBase());
        if (didConvert) {
            user.sendTl("itemsConverted");
        } else {
            user.sendTl("itemsNotConverted");
            throw new NoChargeException();
        }
    }

    private boolean condenseStack(final User user, final ItemStack stack, final boolean validateReverse) throws ChargeException, MaxMoneyException {
        final SimpleRecipe condenseType = getCondenseType(stack);
        if (condenseType != null) {
            final ItemStack input = condenseType.getInput();
            final ItemStack result = condenseType.getResult();
            if (validateReverse) {
                boolean pass = false;
                for (final RecipeHolder<?> revRecipe : ess.getServer().getRecipeManager().getRecipes()) {
                    if (!(revRecipe.value() instanceof CraftingRecipe crafting)) {
                        continue;
                    }
                    final ItemStack revResult = crafting.getResultItem(ess.getServer().registryAccess());
                    if (ItemStack.isSameItemSameComponents(revResult, input) && getStackOnRecipeMatch(crafting, result) != null) {
                        pass = true;
                        break;
                    }
                }
                if (!pass) {
                    return false;
                }
            }
            int amount = 0;
            for (final ItemStack contents : Inventories.getInventory(user.getBase(), false)) {
                if (contents != null && !contents.isEmpty() && ItemStack.isSameItemSameComponents(contents, stack)) {
                    amount += contents.getCount();
                }
            }
            final int output = (amount / input.getCount()) * result.getCount();
            amount -= amount % input.getCount();
            if (amount > 0) {
                input.setCount(amount);
                result.setCount(output);
                final Trade remove = new Trade(input, ess);
                final Trade add = new Trade(result, ess);
                remove.charge(user);
                try {
                    add.pay(user, Trade.OverflowType.DROP);
                } catch (final Exception e) {
                    throw new ChargeException(e, "errorWithMessage", e.getMessage());
                }
                return true;
            }
        }
        return false;
    }

    private SimpleRecipe getCondenseType(final ItemStack stack) {
        final String key = net.essentialsx.fabric.user.UserData.itemKey(stack) + stack.getComponentsPatch().hashCode();
        if (condenseList.containsKey(key)) {
            return condenseList.get(key);
        }
        final List<SimpleRecipe> bestRecipes = new ArrayList<>();
        for (final RecipeHolder<?> holder : ess.getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }
            final Collection<ItemStack> recipeItems = getStackOnRecipeMatch(recipe, stack);
            final ItemStack recipeResult = recipe.getResultItem(ess.getServer().registryAccess());
            if (recipeItems != null && (recipeItems.size() == 4 || recipeItems.size() == 9) && (recipeItems.size() > recipeResult.getCount())) {
                final ItemStack input = stack.copy();
                input.setCount(recipeItems.size());
                bestRecipes.add(new SimpleRecipe(recipeResult.copy(), input));
            }
        }
        if (!bestRecipes.isEmpty()) {
            if (bestRecipes.size() > 1) {
                bestRecipes.sort(Comparator.comparingInt((SimpleRecipe r) -> r.getInput().getCount()).reversed());
            }
            final SimpleRecipe recipe = bestRecipes.get(0);
            condenseList.put(key, recipe);
            return recipe;
        }
        condenseList.put(key, null);
        return null;
    }

    private Collection<ItemStack> getStackOnRecipeMatch(final CraftingRecipe recipe, final ItemStack stack) {
        final List<Ingredient> ingredients;
        if (recipe instanceof ShapedRecipe shaped) {
            if (shaped.getWidth() != shaped.getHeight()) {
                return null;
            }
            ingredients = shaped.getIngredients();
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            ingredients = shapeless.getIngredients();
        } else {
            return null;
        }
        final List<ItemStack> inputList = new ArrayList<>();
        for (final Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            if (!ingredient.test(stack)) {
                return null;
            }
            inputList.add(stack.copyWithCount(1));
        }
        return inputList;
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getMatchingItems(args[0]);
        } else {
            return Collections.emptyList();
        }
    }

    private static final class SimpleRecipe {
        private final ItemStack result;
        private final ItemStack input;

        private SimpleRecipe(final ItemStack result, final ItemStack input) {
            this.result = result;
            this.input = input;
        }

        public ItemStack getResult() {
            return result.copy();
        }

        public ItemStack getInput() {
            return input.copy();
        }
    }
}
