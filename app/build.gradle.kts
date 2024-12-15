plugins {
    // Applies the common conventions plugin for the URL shortener project.
    id("urlshortener-common-conventions")
    // Applies the Kotlin Spring plugin using an alias from the version catalog.
    alias(libs.plugins.kotlin.spring)
    // Applies the Spring Boot plugin using an alias from the version catalog.
    alias(libs.plugins.spring.boot)
    // Applies the Spring Dependency Management plugin using an alias from the version catalog.
    alias(libs.plugins.spring.dependency.management)
    id("org.sonarqube") version "4.4.1.3373" // sonarLint
}
subprojects {
    apply(plugin = "org.sonarqube") // Apply the plugin to all the folders
}

dependencies {
    // Core modules
    implementation(project(":core"))        // Core logic
    implementation(project(":delivery"))    // Delivery layer
    implementation(project(":repositories")) // Repositories layer
    implementation(project(":gateway"))     // Gateway layer

    // Spring Boot starters
    implementation(libs.spring.boot.starter)         // Spring Boot base starter
    implementation(libs.spring.boot.starter.webflux) // WebFlux for reactive controllers

    // R2DBC for reactive database access
    implementation(libs.spring.boot.starter.data.r2dbc) // R2DBC starter
    implementation(libs.r2dbc.h2)                       // R2DBC H2 driver

    // Cache
    implementation(libs.caffeine) // Caffeine

    // Third-party libraries
    implementation(libs.bootstrap)         // Bootstrap for front-end
    implementation(libs.jquery)            // jQuery for front-end
    implementation(libs.uap.java)          // UA-Parser for user-agent parsing
    implementation(libs.zxing.core)        // ZXing for QR code generation
    implementation(libs.dotenv.kotlin)     // dotenv for environment variables

    // Kotlin-specific libraries
    runtimeOnly(libs.kotlin.reflect)       // Kotlin reflection

    // Test dependencies
    testImplementation(libs.kotlin.test)                 // Kotlin test utilities
    testImplementation(libs.mockito.kotlin)              // Mockito Kotlin
    testImplementation(libs.spring.boot.starter.test)    // Spring Boot testing
    testImplementation(libs.spring.boot.starter.webflux) // WebFlux testing
    testImplementation(libs.httpclient5)                 // Apache HttpClient 5
    testImplementation(libs.junit.jupiter)               // JUnit Jupiter for tests
    testRuntimeOnly(libs.junit.platform.launcher)         // JUnit platform launcher
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
