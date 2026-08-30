# Essentials Fabric (1.21.1)

A Fabric-native port of the EssentialsX **core** (`Essentials/`) and **spawn** (`EssentialsSpawn/`) modules for
Minecraft 1.21.1 / Java 21, built from the implementation-ready spec in
`../Essentials_Fabric_1.21.1_Technical_Design.md`.

This directory is a self-contained Gradle build; the Bukkit modules in the parent repository are untouched and remain the
reference implementation.

| Mod id | Artifact | Upstream module |
|---|---|---|
| `essentials_fabric` | `fabric/core/build/libs/EssentialsFabric-<version>.jar` | `Essentials/` (all 153 commands, signs, kits, warps, jails, homes, teleports, AFK, vanish, mail, economy policy, ...) |
| `essentials_fabric_spawn` | `fabric/spawn/build/libs/EssentialsFabricSpawn-<version>.jar` | `EssentialsSpawn/` (`/spawn`, `/setspawn`, respawn/join spawn policy, newbie kit) |

## Provenance / licence

This is a GPL-3.0 derivative port of EssentialsX. Command logic, translations (`messages*.properties`), default
configuration files, `items.json`, `kits.yml`, `worth.yml`, `tpr.yml` and the text files are reused from upstream with the
original attribution kept (`LICENSE_essentials-fabric` inside the jar). It is not an official EssentialsX release.

## Runtime requirements

* Fabric Loader 0.19.3+, Fabric API 0.116.15+1.21.1
* **Impactor 5.3.5+1.21.1** (mod id `impactor`, hard dependency) and its **Cloud** command framework mod (`cloud`).
  Impactor's `EconomyService` is the single authoritative balance store; Essentials only adds policy/commands on top.
  Startup fails with a clear message if Impactor is missing, too old, exposes no economy service, or has no usable
  currency (`economy.impactor-currency` in `config.yml` selects a non-primary currency).
* Optional: `fabric-permissions-api` (bundled), LuckPerms 5.4 (groups, prefixes/suffixes, `/list` groups, kit/home
  limits per group). Without a permissions mod, operators receive every permission and non-operators only the
  `player-commands` list from `config.yml`.

## Building & developing

```
cd fabric
JAVA_HOME="C:/Program Files/Java/jdk-21" ../gradlew.bat build          # EssentialsFabric-*.jar / EssentialsFabricSpawn-*.jar
JAVA_HOME="C:/Program Files/Java/jdk-21" ../gradlew.bat :core:runServer  # dev server in core/run (port 25599)
```

`core/build.gradle.kts` downloads Impactor + Cloud into `core/devmods/` at configuration time and flattens their nested
jars into `core/devmods/flat/` (Fabric Loader does not load jar-in-jar mods from the classpath in a dev environment). These
are `modLocalRuntime` only and are never bundled. Mixins are remapped by Loom at `remapJar` time (no refmap).

## Layout

```
core/src/main/java/net/essentialsx/fabric/
  Essentials.java            container: settings, users, economy, kits, warps, jails, timers, main-thread queue
  EssentialsFabric.java      mod entrypoint; Fabric event wiring; static listener accessors used by mixins
  CommandRegistrar.java      registers all 153 core commands + upstream aliases (see parity/commands.yml)
  command/                   Brigadier bridge (CommandRegistry), EssentialsCommand base classes, exceptions
  commands/                  one class per upstream command (Command<name>), commands/essentials/ = /essentials nodes
  config/                    Settings (upstream config.yml keys), YamlFile, AsyncWriter
  economy/                   ImpactorEconomy binding, Trade (charges), BalanceTop
  items/                     ItemDb, MetaItemStack (data components), Mob/MobData/SpawnMob, Workstations, Inventories
  listener/                  Fabric event handlers + hook methods consumed by mixins (Player/Entity/Block/Jail/Vanish/...)
  mixin/                     vanilla hooks (join/quit/login gates, respawn redirect, keep-inv/xp, display names,
                             chat ignore, sleep, jail teleport/gamemode locks, pickup, targeting, vanish, sign data,
                             unlimited items, left-click-air)
  signs/                     all EssentialsX sign types over SignBlockEntity (+ persistent EssentialsData tag)
  teleport/ user/ warp/ kit/ jail/ mail/ rtp/ backup/ text/ textreader/ utils/
spawn/src/main/java/net/essentialsx/fabric/spawn/   SpawnStorage (spawn.yml), Commandspawn, Commandsetspawn, entrypoint
parity/commands.yml          command/alias parity manifest generated from upstream plugin.yml
```

## Behavioural notes / deliberate differences

* World names: `overworld`, `the_nether`, `the_end`, or `namespace:path` for other dimensions (configurable aliases).
  Permission nodes such as `essentials.worlds.<name>` use these names.
* `/ptime` and `/pweather` are implemented with per-player time/weather packets (`listener/PlayerTimeWeather`), the same
  technique Bukkit uses internally.
* `/essentials dump` writes the support dump to `<data folder>/dumps/<timestamp>/` instead of uploading it.
* `/essentials version` reports Fabric mod versions; `/essentials cmd` lists config-disabled commands.
* `/recipe` shows shaped/shapeless/cooking recipes; item groups are resolved through the vanilla recipe manager.
* Bukkit-only integrations (Vault, PlaceholderAPI, GeoIP, Discord, Protect, XMPP, AntiBuild, Chat) are out of scope;
  chat formatting hooks stay minimal (`chat.radius` is honoured by `/me`).
* Data folder: `config/essentials-fabric/` (config.yml, userdata/, kits.yml, warps/, jail.yml, spawn.yml, ...).

## Releases

`.github/workflows/build-fabric.yml` builds both mods on every push touching `fabric/` and, for a pushed tag matching
`v*` (e.g. `v1.0.0`, `v1.1.0-beta.1`), creates a GitHub Release named after the tag with `EssentialsFabric-*.jar` and
`EssentialsFabricSpawn-*.jar` attached. Tags containing a `-` (beta/rc) are marked as pre-releases; the mod version is
taken from the tag, so `fabric/gradle.properties` only carries the development baseline.
