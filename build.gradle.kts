plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.gamersafer.minecraft"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.alessiodp.com/releases/") // parties
    maven("https://nexus.phoenixdvpt.fr/repository/maven-public/") // mmoitems
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:3.3.39")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.2.0c-beta6")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7-SNAPSHOT")
    compileOnly("com.alessiodp.parties:parties-api:3.2.6") // docs: https://alessiodp.com/docs/parties/hookintoparties
    compileOnly("net.Indyuce:MMOItems-API:6.7.5-SNAPSHOT") // docs: https://gitlab.com/phoenix-dvpmt/mmoitems/-/wikis/Main%20API%20Features
    compileOnly("io.lumine:MythicLib-dist:1.3.4-SNAPSHOT")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.papermc:paperlib:1.0.7")
}

tasks {
    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveFileName.set("ablockalypse-all-${version}.jar")
        relocate("com/zaxxer/hikari", "lib/com/zaxxer/hikari")
        relocate("io/papermc/lib", "com/gamersafer/minecraft/ablockalypse/paperlib")
    }
}