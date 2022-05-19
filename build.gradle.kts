plugins {
    id("java")
    id("fr.il_totore.manadrop") version "0.4.1-SNAPSHOT"
}

group = "com.github.xemiru.mcbomberman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
}

dependencies {
    implementation("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

spigot {
    desc {
        named("bomberman")
        authors("xemiru")
        main("com.github.xemiru.mcbomberman.Main")
        apiVersion("1.18")
    }
}

tasks.getByName("processResources").finalizedBy(tasks.getByName("spigotPlugin"))
