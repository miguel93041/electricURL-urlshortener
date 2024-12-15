plugins {
    // Applies the Kotlin DSL plugin to the project.
    `kotlin-dsl`
    id("org.sonarqube") version "4.4.1.3373"
}
subprojects {
    apply(plugin = "org.sonarqube") // Aplica el plugin a los subproyectos
}


repositories {
    // Adds the Maven Central repository to the list of repositories.
    mavenCentral()
}

dependencies {
    // Adds the Kotlin Gradle plugin as a dependency.
    implementation(libs.kotlin.gradle.plugin)
    // Adds the Detekt Gradle plugin as a dependency.
    implementation(libs.detekt.gradle.plugin)
}
