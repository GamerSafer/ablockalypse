plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.gamersafer.minecraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:3.3.39")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.2.0c-beta6")
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