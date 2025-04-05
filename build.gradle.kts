plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "CopeStudios.CSKoth"
version = "1.0.03"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.21.4-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveBaseName.set("CSKoth")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}