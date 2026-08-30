package net.essentialsx.fabric.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Brigadier tree surgery for command names Essentials shares with another mod. Kept free of any
 * Minecraft or Essentials type so the routing rules can be exercised directly in tests.
 */
public final class CommandMerger {
    private CommandMerger() {
    }

    /**
     * Builds a single literal that serves both the mod that already owned {@code literal} and Essentials,
     * instead of one deleting the other. The foreign node's children are carried over and Essentials'
     * branch is added beside them; Brigadier's {@link CommandNode#getRelevantNodes} returns a matching
     * literal child on its own and only falls back to argument children when none matches, so
     * {@code /eco shop list} reaches the other mod while {@code /eco give Notch 100} reaches Essentials.
     *
     * <p>The shared parent must admit anyone either side would have admitted, or the other mod's op level
     * would hide Essentials' branch (and vice versa). Relaxing the parent must not relax the other mod's
     * command though: mods commonly gate only the root literal and leave their subcommands open, so the
     * foreign requirement is re-imposed on every subtree adopted here and on its bare executor.
     *
     * <p>Two consequences worth knowing: a foreign subcommand the sender may not run is rejected rather
     * than falling through to {@code ourBranch}, since Brigadier commits to a matching literal — the same
     * thing the sender would see without Essentials installed; and where the other mod has an argument
     * child of its own, that argument is tried before ours, so it keeps priority for inputs both parse.
     * A genuine overlap, both sides defining the same subcommand, needs {@code overridden-commands}.
     */
    public static <S> LiteralCommandNode<S> merge(final CommandNode<S> foreign, final String literal, final Predicate<S> ourRequirement, final Command<S> ourExecutor, final ArgumentBuilder<S, ?> ourBranch) {
        final Predicate<S> foreignRequirement = foreign.getRequirement();
        final LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.<S>literal(literal)
            .requires(source -> foreignRequirement.test(source) || ourRequirement.test(source));
        final Command<S> incumbent = foreign.getCommand();
        if (incumbent == null) {
            builder.executes(ourExecutor);
        } else {
            // The incumbent keeps the bare "/<literal>" form, but only for senders it would have accepted.
            builder.executes(ctx -> foreignRequirement.test(ctx.getSource()) ? incumbent.run(ctx) : ourExecutor.run(ctx));
        }
        for (final CommandNode<S> child : foreign.getChildren()) {
            builder.then(gated(child, foreignRequirement));
        }
        builder.then(ourBranch);
        return builder.build();
    }

    /**
     * Copy of {@code node} that additionally requires {@code gate}, used to carry down the requirement its
     * old parent enforced for it. Its own children are reused untouched, since this copy re-imposes the
     * gate for the whole subtree below it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> CommandNode<S> gated(final CommandNode<S> node, final Predicate<S> gate) {
        final Predicate<S> own = node.getRequirement();
        final ArgumentBuilder<S, ?> builder;
        if (node instanceof ArgumentCommandNode) {
            final ArgumentCommandNode<S, ?> argument = (ArgumentCommandNode<S, ?>) node;
            final RequiredArgumentBuilder<S, ?> required = RequiredArgumentBuilder.argument(argument.getName(), (ArgumentType) argument.getType());
            if (argument.getCustomSuggestions() != null) {
                ((RequiredArgumentBuilder) required).suggests(argument.getCustomSuggestions());
            }
            builder = required;
        } else {
            builder = LiteralArgumentBuilder.literal(node.getName());
        }
        builder.requires(source -> gate.test(source) && own.test(source));
        if (node.getCommand() != null) {
            builder.executes(node.getCommand());
        }
        if (node.getRedirect() != null) {
            builder.forward(node.getRedirect(), node.getRedirectModifier(), node.isFork());
        }
        for (final CommandNode<S> child : node.getChildren()) {
            builder.then(child);
        }
        return builder.build();
    }

    /**
     * An argument name {@code node} does not already use. Brigadier merges children by name alone, so
     * reusing a foreign argument's name would silently drop one of the two.
     */
    public static <S> String freeArgumentName(final CommandNode<S> node, final String preferred) {
        String name = preferred;
        while (node.getChild(name) != null) {
            name = "essentials_" + name;
        }
        return name;
    }

    /** Brigadier has no public removal API; drop the literal from the root's lookup maps. */
    public static <S> void removeRootLiteral(final CommandDispatcher<S> dispatcher, final String lower) {
        dispatcher.getRoot().getChildren().removeIf(n -> n.getName().equals(lower));
        try {
            final java.lang.reflect.Field field = CommandNode.class.getDeclaredField("children");
            field.setAccessible(true);
            ((Map<?, ?>) field.get(dispatcher.getRoot())).remove(lower);
            final java.lang.reflect.Field lit = CommandNode.class.getDeclaredField("literals");
            lit.setAccessible(true);
            ((Map<?, ?>) lit.get(dispatcher.getRoot())).remove(lower);
        } catch (final ReflectiveOperationException ignored) {
        }
    }
}
