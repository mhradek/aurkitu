plugins {
    java
    id("io.github.mhradek.aurkitu") // version: aurkituVersion in gradle.properties (see settings.gradle.kts)
}

val aurkituVersion: String by project

repositories {
    mavenCentral()
    // Only to build against an unreleased Aurkitu installed locally (samples/verify.sh); not needed otherwise.
    if (aurkituVersion.endsWith("-SNAPSHOT")) mavenLocal()
}

// Java 17 classes, as with Maven's maven.compiler.release, from whichever JDK runs Gradle.
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

dependencies {
    implementation("io.github.mhradek:aurkitu-annotations:$aurkituVersion")
}

aurkitu {
    schemaName = "game"
    schemaNamespace = "com.example.game.fb"
    specifiedPackages = listOf("com.example.game")
    fileIdentifier = "GAME"
    failOnValidationIssues = true
}
