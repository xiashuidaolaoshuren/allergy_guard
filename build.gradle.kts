import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}

// Load local.properties and expose properties to all subprojects
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    val localProperties = Properties()
    localPropertiesFile.inputStream().use { localProperties.load(it) }
    localProperties.forEach { key, value ->
        project.extra[key.toString()] = value
    }
}