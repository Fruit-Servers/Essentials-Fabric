package net.essentialsx.fabric;

import net.essentialsx.fabric.command.CommandRegistry;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.commands.*;

import java.util.List;

/**
 * Registers every core command with its upstream aliases (generated from the locked
 * EssentialsX {@code plugin.yml}; see parity/commands.yml). 153 commands.
 */
public final class CommandRegistrar {
    private CommandRegistrar() {
    }

    private static void reg(final CommandRegistry registry, final EssentialsCommand command, final String... aliases) {
        registry.register(command, List.of(aliases));
    }

    public static void registerAll(final Essentials ess) {
        final CommandRegistry r = ess.getCommandRegistry();
        reg(r, new Commandafk(), "eafk", "away", "eaway");
        reg(r, new Commandantioch(), "eantioch", "grenade", "egrenade", "tnt", "etnt");
        reg(r, new Commandanvil(), "eanvil");
        reg(r, new Commandback(), "eback", "return", "ereturn");
        reg(r, new Commandbackup(), "ebackup");
        reg(r, new Commandbalance(), "bal", "ebal", "ebalance", "money", "emoney");
        reg(r, new Commandbalancetop(), "ebalancetop", "baltop", "ebaltop");
        reg(r, new Commandban(), "eban");
        reg(r, new Commandbanip(), "ebanip");
        reg(r, new Commandbeezooka(), "ebeezooka", "beecannon", "ebeecannon");
        reg(r, new Commandbigtree(), "ebigtree", "largetree", "elargetree");
        reg(r, new Commandbook(), "ebook");
        reg(r, new Commandbottom(), "ebottom");
        reg(r, new Commandbreak(), "ebreak");
        reg(r, new Commandbroadcast(), "bc", "ebc", "bcast", "ebcast", "ebroadcast", "shout", "eshout");
        reg(r, new Commandbroadcastworld(), "bcw", "ebcw", "bcastw", "ebcastw", "ebroadcastworld", "shoutworld", "eshoutworld");
        reg(r, new Commandburn(), "eburn");
        reg(r, new Commandcartographytable(), "ecartographytable", "carttable", "ecarttable");
        reg(r, new Commandclearinventory(), "ci", "eci", "clean", "eclean", "clear", "eclear", "clearinvent", "eclearinvent", "eclearinventory");
        reg(r, new Commandclearinventoryconfirmtoggle(), "eclearinventoryconfirmtoggle", "clearinventoryconfirmoff", "eclearinventoryconfirmoff", "clearconfirmoff", "eclearconfirmoff", "clearconfirmon", "eclearconfirmon", "clearconfirm", "eclearconfirm");
        reg(r, new Commandcompass(), "ecompass", "direction", "edirection");
        reg(r, new Commandcondense(), "econdense", "compact", "ecompact", "blocks", "eblocks", "toblocks", "etoblocks");
        reg(r, new Commandcreatekit(), "kitcreate", "createk", "kc", "ck");
        reg(r, new Commandcustomtext());
        reg(r, new Commanddelhome(), "edelhome", "remhome", "eremhome", "rmhome", "ermhome");
        reg(r, new Commanddeljail(), "edeljail", "remjail", "eremjail", "rmjail", "ermjail");
        reg(r, new Commanddelkit(), "edelkit", "remkit", "eremkit", "rmkit", "ermkit", "deletekit", "edeletekit");
        reg(r, new Commanddelwarp(), "edelwarp", "remwarp", "eremwarp", "rmwarp", "ermwarp");
        reg(r, new Commanddepth(), "edepth", "height", "eheight");
        reg(r, new Commanddisposal(), "edisposal", "trash", "etrash");
        reg(r, new Commandeco(), "eeco", "economy", "eeconomy");
        reg(r, new Commandeditsign(), "sign", "esign", "eeditsign");
        reg(r, new Commandenchant(), "eenchant", "enchantment", "eenchantment");
        reg(r, new Commandenderchest(), "echest", "eechest", "eenderchest", "endersee", "eendersee", "ec", "eec");
        reg(r, new Commandessentials(), "eessentials", "ess", "eess", "essversion");
        reg(r, new Commandexp(), "eexp", "xp");
        reg(r, new Commandext(), "eext", "extinguish", "eextinguish");
        reg(r, new Commandfeed(), "eat", "eeat", "efeed");
        reg(r, new Commandfireball(), "efireball", "fireentity", "efireentity", "fireskull", "efireskull");
        reg(r, new Commandfirework(), "efirework");
        reg(r, new Commandfly(), "efly");
        reg(r, new Commandgamemode(), "adventure", "eadventure", "adventuremode", "eadventuremode", "creative", "ecreative", "eecreative", "creativemode", "ecreativemode", "egamemode", "gm", "egm", "gma", "egma", "gmc", "egmc", "gms", "egms", "gmt", "egmt", "survival", "esurvival", "survivalmode", "esurvivalmode", "gmsp", "sp", "egmsp", "spec", "spectator");
        reg(r, new Commandgc(), "lag", "elag", "egc", "mem", "emem", "memory", "ememory", "uptime", "euptime", "tps", "etps", "entities", "eentities");
        reg(r, new Commandgetpos(), "coords", "egetpos", "position", "eposition", "whereami", "ewhereami", "getlocation", "egetlocation", "getloc", "egetloc");
        reg(r, new Commandgive(), "egive");
        reg(r, new Commandgod(), "egod", "godmode", "egodmode", "tgm", "etgm");
        reg(r, new Commandgrindstone(), "egrindstone");
        reg(r, new Commandhat(), "ehat", "head", "ehead");
        reg(r, new Commandheal(), "eheal");
        reg(r, new Commandhelp(), "ehelp");
        reg(r, new Commandhelpop(), "ac", "eac", "amsg", "eamsg", "ehelpop");
        reg(r, new Commandhome(), "ehome", "homes", "ehomes");
        reg(r, new Commandice(), "eice", "efreeze");
        reg(r, new Commandignore(), "eignore", "unignore", "eunignore", "delignore", "edelignore", "remignore", "eremignore", "rmignore", "ermignore");
        reg(r, new Commandinfo(), "about", "eabout", "ifo", "eifo", "einfo", "inform", "einform", "news", "enews");
        reg(r, new Commandinvsee(), "einvsee");
        reg(r, new Commanditem(), "i", "eitem", "ei");
        reg(r, new Commanditemdb(), "dura", "edura", "durability", "edurability", "eitemdb", "itemno", "eitemno");
        reg(r, new Commanditemlore(), "lore", "elore", "ilore", "eilore", "eitemlore");
        reg(r, new Commanditemname(), "iname", "einame", "eitemname", "itemrename", "irename", "eitemrename", "eirename");
        reg(r, new Commandjailedplayers(), "ejailedplayers", "ejailed", "ejp");
        reg(r, new Commandjails(), "ejails");
        reg(r, new Commandjump(), "j", "ej", "ejump", "jumpto", "ejumpto");
        reg(r, new Commandkick(), "ekick");
        reg(r, new Commandkickall(), "ekickall");
        reg(r, new Commandkill(), "ekill");
        reg(r, new Commandkit(), "ekit", "kits", "ekits");
        reg(r, new Commandkitreset(), "ekitreset", "kitr", "ekitr", "resetkit", "eresetkit");
        reg(r, new Commandkittycannon(), "ekittycannon");
        reg(r, new Commandlightning(), "elightning", "shock", "eshock", "smite", "esmite", "strike", "estrike", "thor", "ethor");
        reg(r, new Commandlist(), "elist", "online", "eonline", "playerlist", "eplayerlist", "plist", "eplist", "who", "ewho");
        reg(r, new Commandloom(), "eloom");
        reg(r, new Commandmail(), "email", "eemail", "memo", "ememo");
        reg(r, new Commandme(), "action", "eaction", "describe", "edescribe", "eme");
        reg(r, new Commandmore(), "emore");
        reg(r, new Commandmotd(), "emotd");
        reg(r, new Commandmsg(), "w", "m", "t", "pm", "emsg", "epm", "tell", "etell", "whisper", "ewhisper");
        reg(r, new Commandmsgtoggle(), "emsgtoggle");
        reg(r, new Commandmute(), "emute", "silence", "esilence", "unmute", "eunmute");
        reg(r, new Commandnear(), "enear", "nearby", "enearby");
        reg(r, new Commandnick(), "enick", "nickname", "enickname");
        reg(r, new Commandnuke(), "enuke");
        reg(r, new Commandpay(), "epay");
        reg(r, new Commandpayconfirmtoggle(), "epayconfirmtoggle", "payconfirmoff", "epayconfirmoff", "payconfirmon", "epayconfirmon", "payconfirm", "epayconfirm");
        reg(r, new Commandpaytoggle(), "epaytoggle", "payoff", "epayoff", "payon", "epayon");
        reg(r, new Commandping(), "echo", "eecho", "eping", "pong", "epong");
        reg(r, new Commandplaytime(), "eplaytime");
        reg(r, new Commandpotion(), "epotion", "elixer", "eelixer");
        reg(r, new Commandpowertool(), "epowertool", "pt", "ept");
        reg(r, new Commandpowertoollist(), "epowertoollist", "ptlist", "eptlist");
        reg(r, new Commandpowertooltoggle(), "epowertooltoggle", "ptt", "eptt", "pttoggle", "epttoggle");
        reg(r, new Commandptime(), "playertime", "eplayertime", "eptime");
        reg(r, new Commandpweather(), "playerweather", "eplayerweather", "epweather");
        reg(r, new Commandr(), "er", "reply", "ereply");
        reg(r, new Commandrealname(), "erealname");
        reg(r, new Commandrecipe(), "formula", "eformula", "method", "emethod", "erecipe", "recipes", "erecipes");
        reg(r, new Commandremove(), "eremove", "butcher", "ebutcher", "killall", "ekillall", "mobkill", "emobkill");
        reg(r, new Commandrenamehome(), "erenamehome");
        reg(r, new Commandrepair(), "fix", "efix", "erepair");
        reg(r, new Commandrest(), "erest");
        reg(r, new Commandrtoggle(), "ertoggle", "replytoggle", "ereplytoggle");
        reg(r, new Commandrules(), "erules");
        reg(r, new Commandseen(), "eseen", "ealts", "alts");
        reg(r, new Commandsell(), "esell");
        reg(r, new Commandsethome(), "esethome", "createhome", "ecreatehome");
        reg(r, new Commandsetjail(), "esetjail", "createjail", "ecreatejail");
        reg(r, new Commandsettpr(), "esettpr", "settprandom", "esettprandom");
        reg(r, new Commandsetwarp(), "createwarp", "ecreatewarp", "esetwarp");
        reg(r, new Commandsetworth(), "esetworth");
        reg(r, new Commandshowkit(), "kitpreview", "preview", "kitshow");
        reg(r, new Commandskull(), "eskull", "playerskull", "eplayerskull", "head", "ehead");
        reg(r, new Commandsmithingtable(), "esmithingtable", "smithtable", "esmithtable");
        reg(r, new Commandsocialspy(), "esocialspy");
        reg(r, new Commandspawner(), "changems", "echangems", "espawner", "mobspawner", "emobspawner");
        reg(r, new Commandspawnmob(), "mob", "emob", "spawnentity", "espawnentity", "espawnmob");
        reg(r, new Commandspeed(), "flyspeed", "eflyspeed", "fspeed", "efspeed", "espeed", "walkspeed", "ewalkspeed", "wspeed", "ewspeed");
        reg(r, new Commandstonecutter(), "estonecutter");
        reg(r, new Commandsudo(), "esudo");
        reg(r, new Commandsuicide(), "esuicide");
        reg(r, new Commandtempban(), "etempban");
        reg(r, new Commandtempbanip(), "etempbanip");
        reg(r, new Commandthunder(), "ethunder");
        reg(r, new Commandtime(), "day", "eday", "night", "enight", "etime");
        reg(r, new Commandtogglejail(), "jail", "ejail", "tjail", "etjail", "etogglejail", "unjail", "eunjail");
        reg(r, new Commandtop(), "etop");
        reg(r, new Commandtp(), "tele", "etele", "teleport", "eteleport", "etp", "tp2p", "etp2p");
        reg(r, new Commandtpa(), "call", "ecall", "etpa", "tpask", "etpask");
        reg(r, new Commandtpaall(), "etpaall");
        reg(r, new Commandtpacancel(), "etpacancel");
        reg(r, new Commandtpaccept(), "etpaccept", "tpyes", "etpyes");
        reg(r, new Commandtpahere(), "etpahere");
        reg(r, new Commandtpall(), "etpall");
        reg(r, new Commandtpauto(), "etpauto");
        reg(r, new Commandtpdeny(), "etpdeny", "tpno", "etpno");
        reg(r, new Commandtphere(), "s", "etphere");
        reg(r, new Commandtpo(), "etpo");
        reg(r, new Commandtpoffline(), "otp", "offlinetp", "tpoff", "tpoffline", "etpoffline");
        reg(r, new Commandtpohere(), "etpohere");
        reg(r, new Commandtppos(), "etppos");
        reg(r, new Commandtpr(), "etpr", "tprandom", "etprandom");
        reg(r, new Commandtptoggle(), "etptoggle");
        reg(r, new Commandtree(), "etree");
        reg(r, new Commandunban(), "pardon", "eunban", "epardon");
        reg(r, new Commandunbanip(), "eunbanip", "pardonip", "epardonip");
        reg(r, new Commandunlimited(), "eunlimited", "ul", "unl", "eul", "eunl");
        reg(r, new Commandvanish(), "v", "ev", "evanish");
        reg(r, new Commandwarp(), "ewarp", "warps", "ewarps");
        reg(r, new Commandwarpinfo(), "ewarpinfo");
        reg(r, new Commandweather(), "rain", "erain", "sky", "esky", "storm", "estorm", "sun", "esun", "eweather");
        reg(r, new Commandwhois(), "ewhois");
        reg(r, new Commandworkbench(), "craft", "ecraft", "wb", "ewb", "wbench", "ewbench", "eworkbench");
        reg(r, new Commandworld(), "eworld");
        reg(r, new Commandworth(), "eprice", "price", "eworth");
    }
}
