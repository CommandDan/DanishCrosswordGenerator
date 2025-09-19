import java.nio.file.Files

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
    id("com.gradleup.shadow") version "9.1.0"
}

group = "dk.marcusrokatis"
version = "1.2-SNAPSHOT"

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
val wordlistFile = layout.projectDirectory.file("GPT-WordList.yml")
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
    maybeInclude(wordlistFile)
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