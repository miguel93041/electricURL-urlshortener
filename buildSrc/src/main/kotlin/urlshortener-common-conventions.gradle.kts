plugins {
    // Applies the Kotlin JVM plugin to the project.
    kotlin("jvm")
    // Applies the Detekt plugin for static code analysis.
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    // Configures the Kotlin JVM toolchain to use JDK 21.
    jvmToolchain(21)
}

repositories {
    // Adds the Maven Central repository to the list of repositories.
    mavenCentral()
}

tasks {
    test {
        // Configures the test task to use the JUnit Platform.
        useJUnitPlatform()
    }
}
