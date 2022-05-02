plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.gamersafer.minecraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:3.3.39")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
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
    }
}