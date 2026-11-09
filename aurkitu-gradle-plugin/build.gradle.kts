// The Gradle plugin is its own Gradle build, next to the Maven reactor. It uses aurkitu-core of the same version:
// build the reactor first (mvn install) so the core is in the local Maven repository.
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.2.1"
}

// One version for every artifact: the Maven reactor's.
val pom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file("../pom.xml"))
val reactorVersion: String = pom.documentElement.childNodes.let { nodes ->
    (0 until nodes.length).map(nodes::item).first { it.nodeName == "version" }.textContent.trim()
}

group = "io.github.mhradek"
version = reactorVersion

repositories {
    mavenLocal {
        content { includeGroup("io.github.mhradek") }
    }
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
    options.encoding = "UTF-8"
}

val functionalTest: SourceSet by sourceSets.creating

dependencies {
    implementation("io.github.mhradek:aurkitu-core:$reactorVersion")

    "functionalTestImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
    "functionalTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/mhradek/aurkitu"
    vcsUrl = "https://github.com/mhradek/aurkitu"
    testSourceSets(functionalTest)
    plugins {
        create("aurkitu") {
            id = "io.github.mhradek.aurkitu"
            implementationClass = "io.github.mhradek.aurkitu.gradle.AurkituPlugin"
            displayName = "Aurkitu"
            description = "Generates FlatBuffers schemas (.fbs) from annotated Java models."
            tags = listOf("flatbuffers", "schema", "fbs", "codegen", "serialization")
            // Shown on the portal. AurkituPluginFunctionalTest.worksWithTheConfigurationCache checks it.
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the plugin in real builds (Gradle TestKit)."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    // The test builds use the annotations of this version, from the local Maven repository.
    systemProperty("aurkitu.version", reactorVersion)
    systemProperty("maven.repo.local", System.getProperty("maven.repo.local")
        ?: "${System.getProperty("user.home")}/.m2/repository")
    // The Maven plugin's end-to-end test projects: the Gradle plugin must write the same schemas.
    systemProperty("aurkitu.itDir", file("../aurkitu-maven-plugin/src/it").absolutePath)
    inputs.dir("../aurkitu-maven-plugin/src/it")
    // Optional: a mirror of Maven Central for the test builds, where Central itself is rate-limited.
    System.getenv("CENTRAL_MIRROR")?.let { systemProperty("aurkitu.centralMirror", it) }
    // Optional: a real flatc, to check the generated schemas compile.
    System.getenv("FLATC_PATH")?.let { environment("FLATC_PATH", it) }
    testLogging { events("failed", "skipped") }
}

tasks.check { dependsOn(functionalTestTask) }

// As in the Maven build: the getters' Javadoc is in the extension, not repeated on every method.
tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
