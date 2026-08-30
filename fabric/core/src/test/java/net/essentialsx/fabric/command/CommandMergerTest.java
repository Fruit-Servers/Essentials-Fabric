package net.essentialsx.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Routing rules for a command name Essentials shares with another mod. The command source is a plain
 * String -- "op" is a sender the foreign mod accepts, "player" one it rejects -- so the tree behaviour
 * can be asserted without a server.
 */
class CommandMergerTest {
    /** A sender the foreign mod admits; it gates only its root literal, as mods commonly do. */
    private static final Predicate<String> FOREIGN_REQUIREMENT = source -> source.equals("op");
    /** Essentials' own permission check, held by both senders here. */
    private static final Predicate<String> OUR_REQUIREMENT = source -> true;

    private final List<String> handled = new ArrayList<>();
    private CommandDispatcher<String> dispatcher;

    private int record(final String who) {
        handled.add(who);
        return 1;
    }

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<String>literal("eco")
            .requires(FOREIGN_REQUIREMENT)
            .executes(ctx -> record("foreign:bare"))
            .then(LiteralArgumentBuilder.<String>literal("shop")
                .executes(ctx -> record("foreign:shop"))
                .then(LiteralArgumentBuilder.<String>literal("list").executes(ctx -> record("foreign:shop list"))))
            .then(LiteralArgumentBuilder.<String>literal("set").executes(ctx -> record("foreign:set")))
            .then(RequiredArgumentBuilder.<String, Integer>argument("page", IntegerArgumentType.integer())
                .executes(ctx -> record("foreign:page"))));

        final CommandNode<String> foreign = dispatcher.getRoot().getChild("eco");
        final String argName = CommandMerger.freeArgumentName(foreign, "args");
        assertEquals("args", argName, "no foreign child called 'args' in this fixture");
        final CommandNode<String> merged = CommandMerger.merge(foreign, "eco",
            OUR_REQUIREMENT,
            ctx -> record("essentials:bare"),
            RequiredArgumentBuilder.<String, String>argument(argName, StringArgumentType.greedyString())
                .requires(OUR_REQUIREMENT)
                .executes(ctx -> record("essentials:" + StringArgumentType.getString(ctx, argName))));
        CommandMerger.removeRootLiteral(dispatcher, "eco");
        dispatcher.getRoot().addChild(merged);
    }

    private String run(final String source, final String input) {
        handled.clear();
        try {
            dispatcher.execute(input, source);
        } catch (final CommandSyntaxException ex) {
            return "<rejected>";
        }
        return handled.isEmpty() ? "(nothing)" : handled.get(0);
    }

    @Test
    void foreignSubcommandsSurviveTheMerge() {
        assertEquals("foreign:shop list", run("op", "eco shop list"));
        assertEquals("foreign:shop", run("op", "eco shop"));
    }

    @Test
    void unclaimedInputFallsThroughToEssentials() {
        assertEquals("essentials:give Notch 100", run("op", "eco give Notch 100"));
        assertEquals("essentials:reset Notch", run("op", "eco reset Notch"));
    }

    @Test
    void bareCommandStaysWithTheIncumbent() {
        assertEquals("foreign:bare", run("op", "eco"));
    }

    @Test
    void overlappingSubcommandGoesToTheIncumbent() {
        // Both sides define 'set'; resolving that in Essentials' favour is what overridden-commands is for.
        assertEquals("foreign:set", run("op", "eco set"));
    }

    @Test
    void foreignArgumentKeepsPriorityOverOurGreedyOne() {
        assertEquals("foreign:page", run("op", "eco 3"));
    }

    @Test
    void relaxingTheSharedParentDoesNotOpenTheForeignSubtree() {
        // The foreign mod gates only its root, so the merge has to carry that gate onto every child it
        // adopts -- otherwise sharing the literal would hand its subcommands to senders it rejected.
        assertEquals("<rejected>", run("player", "eco shop list"));
        assertEquals("<rejected>", run("player", "eco set"));
    }

    @Test
    void senderTheForeignModRejectsStillReachesEssentials() {
        assertEquals("essentials:give Notch 100", run("player", "eco give Notch 100"));
        assertEquals("essentials:bare", run("player", "eco"));
        assertEquals("essentials:3", run("player", "eco 3"));
    }

    @Test
    void essentialsIsHiddenWhenItsOwnPermissionIsMissing() {
        dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<String>literal("eco")
            .requires(FOREIGN_REQUIREMENT)
            .executes(ctx -> record("foreign:bare")));
        final CommandNode<String> foreign = dispatcher.getRoot().getChild("eco");
        final CommandNode<String> merged = CommandMerger.merge(foreign, "eco",
            source -> false,
            ctx -> record("essentials:bare"),
            RequiredArgumentBuilder.<String, String>argument("args", StringArgumentType.greedyString())
                .requires(source -> false)
                .executes(ctx -> record("essentials:args")));
        CommandMerger.removeRootLiteral(dispatcher, "eco");
        dispatcher.getRoot().addChild(merged);

        assertEquals("<rejected>", run("player", "eco"));
        assertEquals("<rejected>", run("player", "eco give Notch 100"));
        assertEquals("foreign:bare", run("op", "eco"));
    }

    @Test
    void freeArgumentNameAvoidsAForeignChildOfTheSameName() {
        final CommandDispatcher<String> other = new CommandDispatcher<>();
        other.register(LiteralArgumentBuilder.<String>literal("eco")
            .then(RequiredArgumentBuilder.<String, String>argument("args", StringArgumentType.word())));
        assertEquals("essentials_args", CommandMerger.freeArgumentName(other.getRoot().getChild("eco"), "args"));
    }

    @Test
    void removeRootLiteralClearsBothLookupMaps() {
        final CommandDispatcher<String> other = new CommandDispatcher<>();
        other.register(LiteralArgumentBuilder.<String>literal("eco").executes(ctx -> 1));
        CommandMerger.removeRootLiteral(other, "eco");
        assertThrows(CommandSyntaxException.class, () -> other.execute("eco", "op"));
        assertEquals(0, other.getRoot().getChildren().size());
    }
}
