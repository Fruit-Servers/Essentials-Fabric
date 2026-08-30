base {
    archivesName.set("EssentialsFabricSpawn")
}

dependencies {
    implementation(project(path = ":api", configuration = "namedElements"))
    implementation(project(path = ":core", configuration = "namedElements"))
    compileOnly("net.luckperms:api:${property("luckperms_api_version")}")
}

// Dev server: reuse the flattened Impactor/Cloud dev mods prepared by :core (see core/build.gradle.kts).
dependencies {
    "modLocalRuntime"(fileTree(rootProject.file("core/devmods/flat")) { include("*.jar") })
    // core's bundled runtime libraries are not carried by namedElements
    runtimeOnly("org.yaml:snakeyaml:${property("snakeyaml_version")}")
    "modLocalRuntime"("me.lucko:fabric-permissions-api:${property("fabric_permissions_api_version")}")
}
