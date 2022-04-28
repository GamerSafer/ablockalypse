plugins {
    id("java")
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
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

//tasks.getByName<Test>("test") {
//    useJUnitPlatform()
//}