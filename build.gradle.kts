plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
}

group = "dk.marcusrokatis"
version = "1.0-SNAPSHOT"

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