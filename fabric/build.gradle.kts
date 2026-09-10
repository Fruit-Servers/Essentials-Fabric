plugins {
    id("fabric-loom") apply false
    id("java-library")
}

allprojects {
    group = property("maven_group") as String
    version = "${property("mod_version")}+mc${property("minecraft_version")}"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "fabric-loom")

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.impactdev.net/repository/development") {
            name = "ImpactDev"
            content { includeGroupByRegex("net[.]impactdev.*") }
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        // Section 10.3: deprecation warnings visible for Impactor 6 migration.
        options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
    }

    tasks.withType<ProcessResources>().configureEach {
        val props = mapOf(
            "version" to project.version.toString(),
            "mod_version" to project.property("mod_version"),
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "minecraft"("com.mojang:minecraft:${property("minecraft_version")}")
        "mappings"(project.extensions.getByName("loom").let { loom ->
            (loom as net.fabricmc.loom.api.LoomGradleExtensionAPI).officialMojangMappings()
        })
        "modImplementation"("net.fabricmc:fabric-loader:${property("loader_version")}")
        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

        "testImplementation"(platform("org.junit:junit-bom:5.11.4"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
