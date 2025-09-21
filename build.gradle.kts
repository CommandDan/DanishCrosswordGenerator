import java.nio.file.Files

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
    id("com.gradleup.shadow") version "9.1.0"
}

repositories {
    mavenCentral()
}

val openPDFVersion = "3.0.0"
val kamlVersion = "0.95.0"
val kotlinxSerializationVersion = "1.9.0"
dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.librepdf:openpdf:$openPDFVersion")                       // PDF
    implementation("com.charleskorn.kaml:kaml:$kamlVersion")                         // YAML
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")   // Serialization for kaml
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}

application {
    mainClass.set("$group.DanishCrosswordGenerator")
}

// Reproducible JAR'er (ingen fil-timestamps, stabil orden)
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// Shadow JAR: én kørbar -all.jar med Main-Class sat
tasks.shadowJar {
    archiveBaseName.set("DanishCrosswordGenerator")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("all")

    manifest.attributes(
        mapOf("Main-Class" to application.mainClass.get())
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // mergeServiceFiles() // hvis du får service/metadata-konflikter
    // undlad minimize() for OpenPDF/TTF/kaml
}


// Eksterne filer
val gptWordlistFile = layout.projectDirectory.file("GPT-WordList.yml")
val rokatisWordlistFile = layout.projectDirectory.file("Rokatis-WordList.yml")
val fontsDir = layout.projectDirectory.dir("src/main/resources/fonts")
val readmeFile = layout.projectDirectory.file("README.md")

// Hjælpere til at kopiere konditionelt
fun CopySpec.maybeInclude(file: RegularFile) {
    if (file.asFile.exists()) from(file)
}
fun CopySpec.maybeIncludeDir(dir: Directory) {
    if (dir.asFile.exists() && dir.asFile.list()?.isNotEmpty() == true) from(dir) { into("fonts") }
}

tasks.register<Zip>("releaseZip") {
    description = "Pack a ZIP archive for release."
    group = "release"
    dependsOn(tasks.shadowJar)

    val baseName = "DanishCrosswordGenerator"
    val versionStr = project.version.toString()
    archiveFileName.set("$baseName-$versionStr.zip")
    destinationDirectory.set(layout.buildDirectory.dir("releases"))

    // ALT ind i roden af zip (ingen dybe mapper)
    from(tasks.shadowJar.get().archiveFile) {
        rename { "${baseName}-${versionStr}-all.jar" }
    }
    // valgfrit indhold
    maybeInclude(gptWordlistFile)
    maybeInclude(rokatisWordlistFile)
    maybeInclude(readmeFile)
    maybeIncludeDir(fontsDir)

    // læg en lille RUN.txt med kommandoeksempler
    val runTxt = layout.buildDirectory.file("tmp/RUN.txt")
    doFirst {
        val text = """
            Kørselseksempler:
              java -jar ${baseName}-${versionStr}-all.jar --wordlist wordlist.yml --sizes 13x13 15x15
              java -jar ${baseName}-${versionStr}-all.jar --seed 1713412345678
        """.trimIndent()
        Files.createDirectories(runTxt.get().asFile.parentFile.toPath())
        runTxt.get().asFile.writeText(text, Charsets.UTF_8)
    }
    from(runTxt) { rename { "RUN.txt" } }

    // reproducible zip
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// --- Release version guard: ensure project.version matches tag "vX.Y.Z" ---
tasks.register("releaseVersion") {
    group = "release"
    description = "Validates that project.version matches the provided release tag (vX.Y.Z)."

    doLast {
        val projectVersion = project.version.toString()

        // Tag kan komme fra:
        //  - CI: GITHUB_REF=refs/tags/v1.2.3
        //  - CLI: -PreleaseTag=v1.2.3
        //  - (fallback) TAG env
        val fromGithubRef = System.getenv("GITHUB_REF") ?: ""
        val tagFromGithub = fromGithubRef.substringAfter("refs/tags/", missingDelimiterValue = "")
        val tagFromProp = (project.findProperty("releaseTag") as String?) ?: ""
        val tagFromEnv = System.getenv("TAG") ?: ""

        val rawTag = listOf(tagFromProp, tagFromGithub, tagFromEnv).firstOrNull { it.isNotBlank() } ?: ""

        if (rawTag.isBlank()) {
            throw GradleException(
                "No release tag provided. Supply -PreleaseTag=vX.Y.Z or set GITHUB_REF/ TAG environment variable."
            )
        }

        // Forventet format: vX.Y.Z (evt. med prærelease/build metadata, justér regex hvis ønsket)
        val tagRegex = Regex("""^v(\d+\.\d+\.\d+)(?:[-+].*)?$""")
        val match = tagRegex.matchEntire(rawTag)
            ?: throw GradleException("Invalid tag format '$rawTag'. Expected 'vX.Y.Z' (e.g., v1.2.3).")

        val tagVersion = match.groupValues[1] // uden 'v'

        if (projectVersion != tagVersion) {
            throw GradleException(
                "Project version ($projectVersion) does not match tag ($rawTag). " +
                        "Update version in gradle.properties to '$tagVersion' or retag."
            )
        }

        println("✔ releaseVersion: tag $rawTag matches project.version=$projectVersion")
    }
}