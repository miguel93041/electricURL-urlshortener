plugins {
    // Apply the common conventions plugin for the URL shortener project
    id("urlshortener-common-conventions")

    // Apply the Spring Boot plugin
    alias(libs.plugins.spring.boot)

    // Apply the Spring Dependency Management plugin
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Add the core project as an implementation dependency
    implementation(project(":core"))

    // Add R2DBC for reactive database access
    implementation(libs.spring.boot.starter.data.r2dbc)

    // Add the R2DBC driver for H2 (in-memory database)
    implementation(libs.r2dbc.h2)

    // Cache
    implementation(libs.caffeine)

    // Add WebFlux for reactive web capabilities
    implementation(libs.spring.boot.starter.webflux)
}

dependencyManagement {
    imports {
        // Import the Spring Boot BOM (Bill of Materials) for dependency management
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        // Ensure that all dependencies from the org.jetbrains.kotlin group use version 1.9.23
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23")
        }
    }
}
