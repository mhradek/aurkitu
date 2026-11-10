// The sample game models, built with Gradle: gradle build writes build/aurkitu/schemas/game.fbs.
pluginManagement {
    val aurkituVersion = providers.gradleProperty("aurkituVersion").get()
    repositories {
        gradlePluginPortal()
        // Only to build against an unreleased Aurkitu installed locally (samples/verify.sh); not needed otherwise.
        if (aurkituVersion.endsWith("-SNAPSHOT")) mavenLocal()
    }
    plugins {
        id("io.github.mhradek.aurkitu") version aurkituVersion
    }
}

rootProject.name = "game"
