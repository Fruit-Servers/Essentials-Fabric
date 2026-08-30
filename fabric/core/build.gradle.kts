import java.net.URI
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Dev-server dependencies that are not on the Gradle classpath (Impactor + its Cloud command framework).
// They are fetched at configuration time into devmods/ and fed through Loom as remapped local runtime mods, so
// `runServer` boots with the hard economy dependency present (Section 10). Never bundled into the built jar.
//
// Fabric Loader does not load nested (jar-in-jar) mods from classpath mods in a dev environment, so the nested jars are
// flattened into devmods/flat/ (deduplicated by mod id, Fabric API modules dropped because the dev classpath provides them).
val devMods = mapOf(
    "Impactor-Fabric-5.3.5+1.21.1.jar" to "https://cdn.modrinth.com/data/LdBYVaPS/versions/KwNU9SQW/Impactor-Fabric-5.3.5%2B1.21.1.jar",
    "cloud-fabric-2.0.0-beta.10.jar" to "https://cdn.modrinth.com/data/dGpAFG2X/versions/WGE6VNhn/cloud-fabric-2.0.0-beta.10.jar"
)
val devModsDir = file("devmods").also { it.mkdirs() }
val devModsFlatDir = devModsDir.resolve("flat")

fun readModJson(bytes: ByteArray): com.google.gson.JsonObject? {
    ZipInputStream(bytes.inputStream()).use { zin ->
        var entry = zin.nextEntry
        while (entry != null) {
            if (entry.name == "fabric.mod.json") {
                return com.google.gson.JsonParser.parseString(zin.readBytes().toString(Charsets.UTF_8)).asJsonObject
            }
            entry = zin.nextEntry
        }
    }
    return null
}

/** Recursively collects nested mods and rewrites the parent without its "jars" list. */
fun flattenMod(name: String, bytes: ByteArray, out: MutableMap<String, Pair<String, ByteArray>>) {
    val json = readModJson(bytes) ?: return
    val id = json["id"].asString
    val version = json["version"].asString
    if (id.startsWith("fabric-") || id == "fabric-api") return
    val nested = json.getAsJsonArray("jars")?.map { it.asJsonObject["file"].asString }?.toSet() ?: emptySet()
    val entries = linkedMapOf<String, ByteArray>()
    ZipInputStream(bytes.inputStream()).use { zin ->
        var entry = zin.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) entries[entry.name] = zin.readBytes()
            entry = zin.nextEntry
        }
    }
    nested.forEach { path -> entries[path]?.let { flattenMod(path.substringAfterLast('/'), it, out) } }
    json.remove("jars")
    val rewritten = ByteArrayOutputStream()
    ZipOutputStream(rewritten).use { zout ->
        entries.forEach { (path, data) ->
            if (path in nested) return@forEach
            zout.putNextEntry(ZipEntry(path))
            zout.write(if (path == "fabric.mod.json") json.toString().toByteArray(Charsets.UTF_8) else data)
            zout.closeEntry()
        }
    }
    val existing = out[id]
    // length-then-lexical so that beta.10 sorts above beta.9
    if (existing == null || compareValuesBy(version, existing.first, { it.length }, { it }) > 0) {
        out[id] = version to rewritten.toByteArray()
    }
}

val wantsDevServer = gradle.startParameter.taskNames.any { it.contains("runServer") || it.contains("downloadDevMods") }
if (wantsDevServer && (!devModsFlatDir.isDirectory || devModsFlatDir.listFiles().isNullOrEmpty())) {
    val flat = linkedMapOf<String, Pair<String, ByteArray>>()
    devMods.forEach { (name, url) ->
        val target = devModsDir.resolve(name)
        if (!target.exists()) {
            logger.lifecycle("Downloading dev mod $name")
            URI(url).toURL().openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        flattenMod(name, target.readBytes(), flat)
    }
    devModsFlatDir.mkdirs()
    flat.forEach { (id, pair) -> devModsFlatDir.resolve("$id-${pair.first}.jar".replace('/', '_').replace('+', '_')).writeBytes(pair.second) }
    logger.lifecycle("Flattened ${flat.size} dev mods into ${devModsFlatDir}")
}

base {
    archivesName.set("EssentialsFabric")
}

dependencies {
    implementation(project(path = ":api", configuration = "namedElements"))

    // Permissions (soft dependency; operator fallback without it) - Section 7.1
    "modImplementation"("me.lucko:fabric-permissions-api:${property("fabric_permissions_api_version")}")
    "include"("me.lucko:fabric-permissions-api:${property("fabric_permissions_api_version")}")

    // Optional group/meta adapter - Section 7.2
    compileOnly("net.luckperms:api:${property("luckperms_api_version")}")

    // Impactor 5.3.5 economy API (hard runtime dependency, never shaded) - Section 10
    compileOnly("net.impactdev.impactor.api:economy:${property("impactor_api_version")}") {
        exclude(group = "com.h2database")
        exclude(group = "mysql")
        exclude(group = "org.mariadb.jdbc")
        exclude(group = "org.mongodb")
        exclude(group = "com.zaxxer")
        exclude(group = "com.google.inject")
        exclude(group = "org.apache.maven")
        exclude(group = "net.kyori", module = "event-api")
        exclude(group = "org.spongepowered")
    }

    // Adventure text (MiniMessage/legacy) - provided at runtime by Impactor's bundled Adventure jars (Section 12.2)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")
    testImplementation("net.kyori:adventure-api:4.17.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.17.0")
    testImplementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    testImplementation("net.kyori:adventure-text-serializer-gson:4.17.0")

    // YAML for administrator-facing files and imported userdata - Section 6.1
    implementation("org.yaml:snakeyaml:${property("snakeyaml_version")}")
    "include"("org.yaml:snakeyaml:${property("snakeyaml_version")}")

    testImplementation("org.yaml:snakeyaml:${property("snakeyaml_version")}")

    // Dev server only: Impactor + Cloud (remapped by Loom for the named dev runtime)
    "modLocalRuntime"(fileTree(devModsFlatDir) { include("*.jar") })
}

tasks.named<Jar>("jar") {
    from(rootProject.file("../LICENSE")) { rename { "LICENSE_essentials-fabric" } }
}

