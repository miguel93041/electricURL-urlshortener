plugins {
    // Apply the common conventions plugin for the project
    id("urlshortener-common-conventions")

    // Apply the Kotlin Spring plugin
    alias(libs.plugins.kotlin.spring)

    // Apply the Spring Boot plugin
    alias(libs.plugins.spring.boot)

    // Apply the Spring Dependency Management plugin
    alias(libs.plugins.spring.dependency.management)

    // Apply jacoco plugin
    id("jacoco")
}

dependencies {
    // Include the core project as an implementation dependency
    implementation(project(":core"))

    // Adds WebFlux for handling reactive HTTP calls
    implementation(libs.spring.boot.starter.webflux)

    // Adds dotenv Kotlin for managing environment variables
    implementation(libs.dotenv.kotlin)

    // Adds caffeine cache
    implementation(libs.caffeine)

    // Test dependencies
    testImplementation(libs.kotlin.test)               // Kotlin test utilities
    testImplementation(libs.mockito.kotlin)            // Mockito Kotlin
    testImplementation(libs.junit.jupiter)             // JUnit Jupiter
    testRuntimeOnly(libs.junit.platform.launcher)       // JUnit platform launcher
    testImplementation(libs.spring.boot.starter.test)  // Spring Boot testing utilities
    testImplementation(libs.spring.boot.starter.webflux) // WebFlux testing
}

dependencyManagement {
    imports {
        // Import the Spring Boot BOM (Bill of Materials) for dependency management
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        // Force the use of Kotlin version 1.9.23 for all dependencies in the detekt configuration
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23")
        }
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}