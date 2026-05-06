plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "de.boondocksulfur"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/") // Geyser + Floodgate
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.60-stable")
    compileOnly("org.geysermc.geyser:api:2.10.0-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly(fileTree("../CustomJukebox/build/libs") { include("CustomJukebox-*.jar") })
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("CustomJukebox-BedrockExtension-${version}.jar")

        configurations = listOf(project.configurations.runtimeClasspath.get())
        dependencies {
            exclude { it.moduleGroup != "com.google.code.gson" && it.moduleGroup != "org.bstats" }
        }

        relocate("org.bstats", "de.boondocksulfur.customjukeboxbedrock.libs.bstats")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
