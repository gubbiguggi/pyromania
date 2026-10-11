plugins { java }

// Change this to build against another Paper version, e.g. -PpaperVersion=1.21.11-R0.1-SNAPSHOT
val paperVersion = (findProperty("paperVersion") as String?) ?: "1.21.4-R0.1-SNAPSHOT"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

tasks.jar { archiveFileName.set("CustomWeapons.jar") }
