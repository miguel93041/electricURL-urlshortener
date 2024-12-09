plugins {
    // Apply the common conventions plugin for the URL shortener project
    id("urlshortener-common-conventions")
    // Applies the Kotlin Spring plugin using an alias from the version catalog.
    alias(libs.plugins.kotlin.spring)
    // Applies the Spring Boot plugin using an alias from the version catalog.
    alias(libs.plugins.spring.boot)
    // Applies the Spring Dependency Management plugin using an alias from the version catalog.
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Spring Boot WebFlux for reactive web capabilities
    implementation(libs.spring.boot.starter.webflux)
    // UA-Parser for user-agent parsing
    implementation(libs.uap.java)
    // ZXing for QR code generation
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)
    // kotlin-result for functional programming support
    implementation(libs.kotlin.result)

    // Test dependencies
    testImplementation(libs.kotlin.test)               // Kotlin test utilities
    testImplementation(libs.mockito.kotlin)            // Mockito Kotlin
    testImplementation(libs.junit.jupiter)             // JUnit Jupiter for tests
    testRuntimeOnly(libs.junit.platform.launcher)       // JUnit platform launcher
}

dependencyManagement {
    imports {
        // Imports the Spring Boot BOM (Bill of Materials) for dependency management.
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        // Ensures that all dependencies from the org.jetbrains.kotlin group use version 1.9.23.
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23")
        }
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
