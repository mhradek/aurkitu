// The Gradle plugin is its own Gradle build, next to the Maven reactor. It uses aurkitu-core of the same version:
// build the reactor first (mvn install) so the core is in the local Maven repository.
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    jacoco
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

// The functional tests' name: their source set, their test task and the repository they publish the plugin to.
val functionalTestName = "functionalTest"
val functionalTest: SourceSet = sourceSets.create(functionalTestName)

dependencies {
    implementation("io.github.mhradek:aurkitu-core:$reactorVersion")

    // The unit tests use the Gradle API (ProjectBuilder) at run time. Since Gradle 9, java-gradle-plugin no longer
    // puts it on the test runtime classpath; Gradle 8 did, so the tests passed there and failed to load on 9.
    testImplementation(gradleApi())
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    "functionalTestImplementation"("org.junit.jupiter:junit-jupiter:6.1.3")
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

// The plugin as published (with its marker and POM), for the test that resolves it by id and version.
val functionalTestRepo = layout.buildDirectory.dir("functional-test-repo")
publishing {
    repositories {
        maven {
            name = functionalTestName
            url = uri(functionalTestRepo)
        }
    }
}

val functionalTestTask = tasks.register<Test>(functionalTestName) {
    description = "Runs the plugin in real builds (Gradle TestKit)."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    // The test builds use the annotations of this version, from the local Maven repository.
    systemProperty("aurkitu.version", reactorVersion)
    systemProperty("maven.repo.local", System.getProperty("maven.repo.local")
        ?: "${System.getProperty("user.home")}/.m2/repository")
    // The Maven plugin's descriptor, whose parameters the Gradle options must match.
    val mavenRepo = System.getProperty("maven.repo.local") ?: "${System.getProperty("user.home")}/.m2/repository"
    systemProperty("aurkitu.mavenPluginJar",
        "$mavenRepo/io/github/mhradek/aurkitu-maven-plugin/$reactorVersion/aurkitu-maven-plugin-$reactorVersion.jar")
    dependsOn("publishAllPublicationsToFunctionalTestRepository")
    systemProperty("aurkitu.pluginRepo", functionalTestRepo.get().asFile.absolutePath)
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

// Unit tests (ProjectBuilder: the plugin's wiring and the task's action, in this JVM) and their coverage gate:
// at least 80% of lines and branches in every class, as for the Maven modules. The functional tests run in
// separate Gradle processes, which JaCoCo here does not see.
tasks.test {
    useJUnitPlatform()
}
jacoco {
    toolVersion = "0.8.16"
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
}
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
