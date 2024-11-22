plugins {
    // Applies the common conventions plugin for the URL shortener project.
    id("urlshortener-common-conventions")
    // Applies the Kotlin Spring plugin using an alias from the version catalog.
    alias(libs.plugins.kotlin.spring)
    // Applies the Spring Boot plugin using an alias from the version catalog.
    alias(libs.plugins.spring.boot)
    // Applies the Spring Dependency Management plugin using an alias from the version catalog.
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Adds the core project as an implementation dependency.
    implementation(project(":core"))
    // Adds the delivery project as an implementation dependency.
    implementation(project(":delivery"))
    // Adds the repositories project as an implementation dependency.
    implementation(project(":repositories"))
    // Adds the gateway project as an implementation dependency.
    implementation(project(":gateway"))

    // Adds the Spring Boot starter WebFlux as an implementation dependency.
    implementation(libs.spring.boot.starter.webflux)
    // Adds UA-Parser as an implementation dependency.
    implementation(libs.uap.java)
    // Adds ZXing Core as an implementation dependency.
    implementation(libs.zxing.core)
    // Adds dotenv Kotlin as an implementation dependency.
    implementation(libs.dotenv.kotlin)

    // Adds HSQLDB as a runtime-only dependency.
    runtimeOnly(libs.hsqldb)
    // Adds Kotlin reflection library as a runtime-only dependency.
    runtimeOnly(libs.kotlin.reflect)

    // Adds Kotlin test library as a test implementation dependency.
    testImplementation(libs.kotlin.test)
    // Adds Mockito Kotlin library as a test implementation dependency.
    testImplementation(libs.mockito.kotlin)
    // Adds Spring Boot starter test library as a test implementation dependency.
    testImplementation(libs.spring.boot.starter.test)
    // Adds Spring Boot starter web library as a test implementation dependency.
    testImplementation(libs.spring.boot.starter.web)
    // Adds Spring Boot starter JDBC library as a test implementation dependency.
    testImplementation(libs.spring.boot.starter.jdbc)
    // Adds Apache HttpClient 5 as a test implementation dependency.
    testImplementation(libs.httpclient5)
    // Adds JUnit Jupiter as a test implementation dependency.
    testImplementation(libs.junit.jupiter)
    // Adds JUnit Platform launcher as a test runtime-only dependency.
    testRuntimeOnly(libs.junit.platform.launcher)
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
