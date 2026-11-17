plugins {
    // Settings plugins are resolved before the version catalog exists, so this version cannot live there.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "scoreline"
