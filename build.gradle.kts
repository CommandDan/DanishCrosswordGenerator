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