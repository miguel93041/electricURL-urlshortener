plugins {
    // Apply the common conventions plugin for the project
    id("urlshortener-common-conventions")

    // Apply the Kotlin Spring plugin
    alias(libs.plugins.kotlin.spring)

    // Apply the Spring Boot plugin but do not apply it immediately
    alias(libs.plugins.spring.boot) apply false

    // Apply the Spring Dependency Management plugin
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Include the core project as an implementation dependency
    implementation(project(":core"))

    // Include WebFlux for reactive controllers
    implementation(libs.spring.boot.starter.webflux)

    // Include Apache Commons Validator for input validation
    implementation(libs.commons.validator)

    // Include Google Guava for utility functions
    implementation(libs.guava)

    // Include kotlin-result for functional programming support
    implementation(libs.kotlin.result)

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
